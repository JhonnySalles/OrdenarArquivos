package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.controller.TelaInicialController
import com.fenix.ordenararquivos.database.DataBase
import com.fenix.ordenararquivos.database.DataBase.instancia
import com.fenix.ordenararquivos.model.Sincronizacao
import com.fenix.ordenararquivos.model.firebase.Caminhos
import com.fenix.ordenararquivos.model.firebase.Manga
import com.fenix.ordenararquivos.util.Utils
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import io.grpc.LoadBalancerRegistry
import io.grpc.internal.PickFirstLoadBalancerProvider
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStream
import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Collectors


class SincronizacaoServices(private val controller: TelaInicialController) : TimerTask() {

    private val mLOG = LoggerFactory.getLogger(SincronizacaoServices::class.java)

    private val mUPDATE = "UPDATE Sincronizacao SET envio = ?, recebimento = ?"
    private val mSELECT = "SELECT envio, recebimento FROM Sincronizacao LIMIT 1"

    private var conn: Connection = instancia

    private val service = MangaServices()
    private var sincronizacao: Sincronizacao? = null
    private lateinit var DB: Firestore

    private val formaterData = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val formaterDataHora = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private var sincronizando = false

    private val collection = "ORDENAR"

    companion object {
        private val sincronizar: ObservableList<Manga> = FXCollections.observableArrayList()
        fun enviar(manga: com.fenix.ordenararquivos.model.Manga) = sincronizar.add(Manga(manga))
    }

    fun setObserver(listener: ListChangeListener<Manga>) {
        sincronizar.addListener(listener)
    }

    init {
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
    }

    override fun run() {
        if (!sincronizando)
            sincroniza()
    }

    fun consultar() {
        if (sincronizacao == null)
            return

        try {
            val sinc = service.findEnvio(sincronizacao!!.envio).parallelStream().map { m -> Manga(m) }
                .filter { i -> sincronizar.parallelStream().noneMatch { m -> m.equals(i) } }
                .collect(Collectors.toList())
            if (sinc.isNotEmpty())
                sincronizar.addAll(sinc)
        } catch (ex: Exception) {
            mLOG.error(ex.message, ex)
        }
    }


    var registros = 0
    var processados: String = ""

    private fun getIdCloud(manga: Manga) : String = manga.nome + " - " + manga.volume

    private fun envia(): Boolean {
        var processado = false
        registros = 0
        if (!sincronizar.isEmpty()) {
            mLOG.info("Enviando dados a cloud... ")
            val sinc: List<Manga> = sincronizar.parallelStream().sorted { o1: Manga, o2: Manga -> o2.nome.compareTo(o1.nome) }.distinct().toList()
            try {
                sincronizar.clear()
                val envio = LocalDateTime.now().format(formaterDataHora)

                if (sinc.isNotEmpty()) {
                    val docRef = DB.collection(collection).document(formaterData.format(LocalDate.now()))
                    val data: MutableMap<String, Any> = HashMap()
                    for (manga in sinc) {
                        manga.sincronizacao = envio
                        data[getIdCloud(manga)] = manga
                    }
                    val result = docRef.set(data)
                    result.get()
                    registros += sinc.size
                    mLOG.info("Enviado dados a cloud: " + sinc.size + " registros. ")
                }

                if (registros > 0)
                    processados += "Enviado $registros registro(s). "

                mLOG.info("Concluído envio de dados a cloud.")
                processado = true
            } catch (e: Exception) {
                sincronizar.addAll(sinc)
                mLOG.error("Erro ao enviar dados a cloud, adicionado arquivos para novo ciclo. ${e.message}", e)
                throw e
            }
        }
        return processado
    }
    
    private fun receber(): Boolean {
        var processado = false
        try {
            mLOG.info("Recebendo dados a cloud.... ")
            val lista = mutableListOf<com.fenix.ordenararquivos.model.Manga>()
            val atual = LocalDate.now().format(formaterData)

            val query = DB.collection(collection).get()
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
                          manga.addCaminhos(Caminhos.toCominhos(manga, (caminho as HashMap<String, *>) ))
                    }
                }
            }

            mLOG.info("Processando retorno dados a cloud: " + lista.size + " registros.")
            registros = lista.size
            for (sinc in lista) {
                var manga: com.fenix.ordenararquivos.model.Manga? = service.find(sinc)
                if (manga != null)
                    manga.merge(sinc)
                else
                    manga = sinc

                service.save(manga, false)
            }

            if (registros > 0)
                processados += "Recebido $registros registro(s). "

            processado = true
            mLOG.info("Concluído recebimento de dados a cloud.")
        } catch (e: Exception) {
            mLOG.error("Erro ao receber dados a cloud. ${e.message}".trimIndent(), e)
            throw e
        }
        return processado
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

    fun listSize(): Int = sincronizar.size

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

}