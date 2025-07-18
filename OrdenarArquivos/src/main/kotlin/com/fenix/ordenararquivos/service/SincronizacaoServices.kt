package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.controller.AbaArquivoController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.database.DataBase.instancia
import com.fenix.ordenararquivos.model.entities.Sincronizacao
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.model.enums.Tipo
import com.fenix.ordenararquivos.model.firebase.ComicInfo
import com.fenix.ordenararquivos.model.firebase.Manga
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.util.Utils
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.grpc.LoadBalancerRegistry
import io.grpc.internal.PickFirstLoadBalancerProvider
import javafx.application.Platform
import javafx.collections.*
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStream
import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Date
import java.util.stream.Collectors


class SincronizacaoServices(private val controller: AbaArquivoController) : TimerTask() {

    private val mLOG = LoggerFactory.getLogger(SincronizacaoServices::class.java)

    private val mUPDATE = "UPDATE Sincronizacao SET envio = ?, recebimento = ?"
    private val mSELECT = "SELECT envio, recebimento FROM Sincronizacao LIMIT 1"

    private var conn: Connection = instancia

    private val serviceManga = MangaServices()
    private val serviceComicInfo = ComicInfoServices()
    private var sincronizacao: Sincronizacao? = null
    private lateinit var DB: Firestore

    private val formaterData = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val formaterDataHora = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private var sincronizando = false

    private val collectOrdenar = "ORDENAR"
    private val collectComicInfo = "COMICINFO"

    companion object {
        private val sincManga: ObservableList<Manga> = FXCollections.observableArrayList()
        private val sincComicInfo: ObservableList<ComicInfo> = FXCollections.observableArrayList()
        private val sincronizar: ObservableList<Pair<Tipo, Int>> = FXCollections.observableArrayList()
        fun enviar(manga: com.fenix.ordenararquivos.model.entities.Manga) = sincManga.add(Manga(manga))
        fun enviar(comic: com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo) = sincComicInfo.add(ComicInfo(comic))
    }

    fun setObserver(listener: ListChangeListener<Pair<Tipo, Int>>) = sincronizar.addListener(listener)

    init {
        if (!DataBase.isTeste) {
            val timer = Timer(true)
            timer.scheduleAtFixedRate(this, 0, (5 * 60 * 1000).toLong())

            try {
                val serviceAccount: InputStream = FileInputStream("secrets-firebase.json")
                val credentials = GoogleCredentials.fromStream(serviceAccount)
                val options = FirebaseOptions.builder().setCredentials(credentials).build()
                FirebaseApp.initializeApp(options)
                DB = FirestoreClient.getFirestore()

                sincronizacao = select()
            } catch (ex: Exception) {
                mLOG.error(ex.message, ex)
            }

            consultar()

            sincManga.addListener {  observable: ListChangeListener.Change<out Manga> ->
                if(observable.list.isEmpty())
                    sincronizar.removeIf { it.first == Tipo.MANGA }
                else if (sincronizar.any { it.first == Tipo.MANGA })
                    sincronizar[sincronizar.indexOfFirst { it.first == Tipo.MANGA }] = Pair(Tipo.MANGA, observable.list.size)
                else
                    sincronizar.add(Pair(Tipo.MANGA, observable.list.size))
            }
            sincComicInfo.addListener {  observable: ListChangeListener.Change<out ComicInfo> ->
                if(observable.list.isEmpty())
                    sincronizar.removeIf { it.first == Tipo.COMICINFO }
                else if (sincronizar.any { it.first == Tipo.COMICINFO })
                    sincronizar[sincronizar.indexOfFirst { it.first == Tipo.COMICINFO }] = Pair(Tipo.COMICINFO, observable.list.size)
                else
                    sincronizar.add(Pair(Tipo.COMICINFO, observable.list.size))
            }
        }
    }

    override fun run() {
        if (!sincronizando)
            sincroniza()
    }

    fun consultar() {
        if (sincronizacao == null)
            return

        try {
            val sinc = serviceManga.findEnvio(sincronizacao!!.envio).parallelStream().map { m -> Manga(m) }
                .filter { i -> sincManga.parallelStream().noneMatch { m -> m.equals(i) } }
                .collect(Collectors.toList())
            if (sinc.isNotEmpty())
                sincManga.addAll(sinc)
        } catch (ex: Exception) {
            mLOG.error("Erro ao consultar registros de Manga para envio.", ex)
        }

        try {
            val sinc = serviceComicInfo.findEnvio(sincronizacao!!.envio).parallelStream().map { c -> ComicInfo(c) }
                .filter { i -> sincComicInfo.parallelStream().noneMatch { c -> c.equals(i) } }
                .collect(Collectors.toList())
            if (sinc.isNotEmpty())
                sincComicInfo.addAll(sinc)
        } catch (ex: Exception) {
            mLOG.error("Erro ao consultar registros de ComicInfo para envio.", ex)
        }

    }


    var registros = 0
    var processados: String = ""

    private fun getIdCloud(manga: Manga) : String = manga.nome + " - " + manga.volume
    private fun getIdCloud(comic: ComicInfo) : String = comic.comic

    private fun envia(): Boolean {
        var notificacao = ""

        var processadoManga = false
        registros = 0
        if (!sincManga.isEmpty()) {
            mLOG.info("Enviando dados de Mangas da cloud... ")
            val sinc: List<Manga> = sincManga.parallelStream().sorted { o1: Manga, o2: Manga -> o2.nome.compareTo(o1.nome) }.distinct().toList()
            try {
                sincManga.clear()
                val envio = LocalDateTime.now().format(formaterDataHora)

                if (sinc.isNotEmpty()) {
                    val docRef = DB.collection(collectOrdenar).document(formaterData.format(LocalDate.now()))
                    val data: MutableMap<String, Any> = HashMap()
                    val document = docRef.get().get()

                    if (document.exists())
                        for (key in document.data!!.keys)
                            data[key] = document.data!![key] as HashMap<String, *>

                    for (manga in sinc) {
                        manga.sincronizacao = envio
                        data[getIdCloud(manga)] = manga
                    }
                    val result = docRef.set(data)
                    result.get()
                    registros += sinc.size
                    mLOG.info("Enviado dados de mangas a cloud: " + sinc.size + " registros. ")
                }

                if (registros > 0) {
                    processados += "Enviado $registros registro(s) de Mangas. "
                    notificacao += "Enviado $registros registro(s) de Mangas.\n"
                }

                mLOG.info("Concluído envio de dados de Mangas da cloud.")
                processadoManga = true
            } catch (e: Exception) {
                sincManga.addAll(sinc)
                mLOG.error("Erro ao enviar dados de Mangas da cloud, adicionado arquivos para novo ciclo. ${e.message}", e)
                throw e
            }
        }

        var processadoComic = false
        registros = 0
        if (!sincComicInfo.isEmpty()) {
            mLOG.info("Enviando dados do ComicInfo a cloud... ")
            val sinc: List<ComicInfo> = sincComicInfo.parallelStream().sorted { o1: ComicInfo, o2: ComicInfo -> o2.comic.compareTo(o1.comic) }.distinct().toList()
            try {
                sincComicInfo.clear()
                val envio = LocalDateTime.now().format(formaterDataHora)

                if (sinc.isNotEmpty()) {
                    val index = mutableMapOf<String, Date>()
                    val docRef = DB.collection(collectComicInfo)
                    val docIndex = docRef.document("_INDEX").get().get()

                    if (docIndex.exists()) {
                        val item = docIndex.data ?: mapOf()
                        index.putAll(item as Map<String, Date>)
                    }

                    for (comic in sinc) {
                        comic.sincronizacao = envio
                        val id = getIdCloud(comic)
                        index[id] = Date()
                        docRef.document(id).set(removeValoresNull(comic)).get()
                    }

                    docRef.document("_INDEX").set(index as Map<String, Any>).get()
                    registros += sinc.size
                    mLOG.info("Enviado dados do ComicInfo a cloud: " + sinc.size + " registros. ")
                }

                if (registros > 0) {
                    processados += "Enviado $registros registro(s) de ComicInfo. "
                    notificacao += "Enviado $registros registro(s) de ComicInfo.\n"
                }

                mLOG.info("Concluído envio de dados do ComicInfo a cloud.")
                processadoComic = true
            } catch (e: Exception) {
                sincComicInfo.addAll(sinc)
                mLOG.error("Erro ao enviar dados do ComicInfo a cloud, adicionado arquivos para novo ciclo. ${e.message}", e)
                throw e
            }
        }

        if (notificacao.isNotEmpty())
            Platform.runLater { Notificacoes.notificacao(Notificacao.SUCESSO, "Recebimento da cloud com sucesso", notificacao.substringBeforeLast("\n")) }

        return processadoManga && processadoComic
    }
    
    private fun receber(): Boolean {
        var notificacao = ""

        val atualizacao = sincronizacao!!.envio.minusSeconds(2)
        var processadoManga = false
        try {
            mLOG.info("Recebendo dados de Manga da cloud.... ")
            val lista = mutableListOf<com.fenix.ordenararquivos.model.entities.Manga>()
            val atual = LocalDate.now().format(formaterData)

            val query = DB.collection(collectOrdenar).get()
            val querySnapshot = query.get()
            val documents = querySnapshot.documents
            for (document in documents) {
                val data = LocalDate.parse(document.id, formaterData)

                if (sincronizacao!!.recebimento.toLocalDate().isAfter(data) && !atual.equals(document.id, ignoreCase = true))
                    continue

                for (key in document.data.keys) {
                    val obj = document.data[key] as HashMap<String, *>
                    val sinc = LocalDateTime.parse(obj["sincronizacao"] as String, formaterDataHora)
                    if (sinc.isAfter(sincronizacao!!.recebimento)) {
                        val manga = Manga.toManga(0, obj)
                        lista.add(manga)
                        val caminhos = (document.data[key] as HashMap<*, *>)["caminhos"] as List<*>
                        for (caminho in caminhos)
                          manga.addCaminhos(com.fenix.ordenararquivos.model.firebase.Caminhos.toCominhos(manga, (caminho as HashMap<String, *>) ))
                    }
                }
            }

            mLOG.info("Processando retorno dados de Manga da cloud: " + lista.size + " registros.")
            registros = lista.size
            for (sinc in lista) {
                var manga: com.fenix.ordenararquivos.model.entities.Manga? = serviceManga.find(sinc)
                if (manga != null)
                    manga.merge(sinc)
                else
                    manga = sinc

                serviceManga.save(manga, false, atualizacao)
            }

            if (registros > 0) {
                processados += "Recebido $registros registro(s) de Manga. "
                notificacao += "Recebido $registros registro(s) de Manga.\n"
            }

            processadoManga = true
            mLOG.info("Concluído recebimento de dados de Manga da cloud.")
        } catch (e: Exception) {
            mLOG.error("Erro ao receber dados de Manga da cloud. ${e.message}".trimIndent(), e)
            throw e
        }

        var processadoComic = false
        try {
            mLOG.info("Recebendo dados do ComicInfo da cloud.... ")
            val lista = mutableListOf<com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo>()

            val docRef = DB.collection(collectComicInfo)
            val docIndex = docRef.document("_INDEX").get().get()
            val documents = mutableListOf<String>()

            val recebimento = Date.from(sincronizacao!!.recebimento.atZone(ZoneId.systemDefault()).toInstant())

            if (docIndex.exists()) {
                docIndex.data?.keys?.forEach {
                    val data = docIndex.data!![it] as com.google.cloud.Timestamp
                    if (recebimento.before(data.toDate()))
                        documents.add(it)
                }
            }

            for (id in documents) {
                val document = docRef.document(id).get().get() ?: continue
                val data = document.data ?: continue
                lista.add(ComicInfo.toComicInfo(data as HashMap<String, *>))
            }

            mLOG.info("Processando retorno dados do ComicInfo da cloud: " + lista.size + " registros.")
            registros = lista.size
            for (sinc in lista) {
                var comic: com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo? = serviceComicInfo.find(sinc.comic, sinc.languageISO)
                if (comic != null)
                    comic.merge(sinc)
                else
                    comic = sinc

                serviceComicInfo.save(comic, isSendCloud = false, isReceiveCloud = true, atualizacao)
            }

            if (registros > 0) {
                processados += "Recebido $registros registro(s) de ComicInfo. "
                notificacao += "Recebido $registros registro(s) de ComicInfo.\n"
            }

            processadoComic = true
            mLOG.info("Concluído recebimento de dados do ComicInfo da cloud.")
        } catch (e: Exception) {
            mLOG.error("Erro ao receber dados do ComicInfo da cloud. ${e.message}".trimIndent(), e)
            throw e
        }

        if (notificacao.isNotEmpty())
            Platform.runLater { Notificacoes.notificacao(Notificacao.SUCESSO, "Recebimento da cloud com sucesso", notificacao.substringBeforeLast("\n")) }

        return processadoManga && processadoComic
    }

    fun sincroniza(): Boolean {
        if (sincronizacao == null)
            return false

        return try {
            LoadBalancerRegistry.getDefaultRegistry().register(PickFirstLoadBalancerProvider())

            sincronizando = true
            controller.animacaoSincronizacao(isProcessando = true, isErro = false)
            processados = ""
            val recebido = receber()
            val enviado = envia()

            if (enviado)
                sincronizacao!!.envio = LocalDateTime.now()

            if (recebido)
                sincronizacao!!.recebimento = LocalDateTime.now()

            if (enviado || recebido) {
                update(sincronizacao!!)
                Platform.runLater { controller.setLog(processados.trim()) }
            } else
                Platform.runLater { controller.setLog("") }

            controller.animacaoSincronizacao(isProcessando = false, isErro = false)
            sincronizando = false
            true
        } catch (e: Exception) {
            controller.animacaoSincronizacao(isProcessando = false, isErro = true)
            false
        }
    }

    fun isConfigurado(): Boolean = sincronizacao != null

    fun isSincronizando(): Boolean = sincronizando

    fun listSize(): Int = sincManga.size + sincComicInfo.size

    fun listPendentes(): String {
        var sinc = ""
        sinc += if (sincManga.isNotEmpty()) "${sincManga.size} (Manga) " else ""
        sinc += if (sincComicInfo.isNotEmpty()) "${sincComicInfo.size} (ComicInfo) " else ""
        return sinc
    }

    @Throws(SQLException::class)
    private fun update(obj: Sincronizacao) {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mUPDATE)
            st.setString(1, Utils.fromDateTime(obj.envio))
            st.setString(2, Utils.fromDateTime(obj.recebimento))

            val rowsAffected = st.executeUpdate()
            if (rowsAffected < 1)
                mLOG.error("Não foi possível gravar o ultimo envio do log.")
        } catch (e: SQLException) {
            mLOG.error("Erro ao atualizar o manga.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
        }
    }

    @Throws(SQLException::class)
    private fun select(): Sincronizacao {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT)
            rs = st.executeQuery()
            return if (rs.next())
                Sincronizacao(Utils.toDateTime(rs.getString("envio")), Utils.toDateTime(rs.getString("recebimento")))
            else
                throw Exception("Registro de ultima sincronização não encontrado")
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar os caminhos.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
        }
    }

    private fun removeValoresNull(userObject: ComicInfo): Map<String, Any> {
        val gson = GsonBuilder().create()
        val map: Map<String, Any> = Gson().fromJson(gson.toJson(userObject), object : TypeToken<HashMap<String?, Any?>?>() {}.type)
        return map
    }

}