package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.database.DataBase.closeResultSet
import com.fenix.ordenararquivos.database.DataBase.closeStatement
import com.fenix.ordenararquivos.database.DataBase.instancia
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.TrackerResult
import com.fenix.ordenararquivos.model.entities.comicinfo.TrackerMetadata
import com.fenix.ordenararquivos.model.entities.comicinfo.TrackerAuthor
import com.fenix.ordenararquivos.model.enums.TrackerType
import com.fenix.ordenararquivos.api.anilist.AniListTracker
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.util.Utils
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.jfoenix.controls.JFXButton
import dev.katsute.mal4j.MyAnimeList
import javafx.animation.PauseTransition
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.util.Duration
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class ComicInfoServices {

    private val mLOG = LoggerFactory.getLogger(ComicInfoServices::class.java)
    private val mHttpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(4))
        .build()

    private val mUPDATE_COMIC_INFO = "UPDATE ComicInfo SET comic = ?, idMal = ?, series = ?, title = ?, publisher = ?, genre = ?, imprint = ?, seriesGroup = ?, storyArc = ?, maturityRating = ?, alternativeSeries = ?, language = ?,  atualizacao = ? WHERE id = ?"
    private val mINSERT_COMIC_INFO = "INSERT INTO ComicInfo (id, comic, idMal, series, title, publisher, genre, imprint, seriesGroup, storyArc, maturityRating, alternativeSeries, language, criacao, atualizacao) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
    private val mSELECT_COMIC_INFO = "SELECT id, comic, idMal, series, title, publisher, genre, imprint, seriesGroup, storyArc, maturityRating, alternativeSeries, language FROM ComicInfo WHERE language = ? AND (UPPER(comic) LIKE ? or UPPER(series) LIKE ? or UPPER(title) LIKE ?) LIMIT 1"
    private val mSELECT_COMIC_INFO_BY_NAME = "SELECT id, comic, idMal, series, title, publisher, genre, imprint, seriesGroup, storyArc, maturityRating, alternativeSeries, language FROM ComicInfo WHERE UPPER(comic) LIKE ? or UPPER(series) LIKE ? or UPPER(title) LIKE ? LIMIT 1"
    private val mDELETE_COMIC_INFO = "DELETE FROM ComicInfo WHERE id = ?"

    private val mSELECT_ENVIO = "SELECT id, comic, idMal, series, title, publisher, genre, imprint, seriesGroup, storyArc, maturityRating, alternativeSeries, language, atualizacao FROM ComicInfo WHERE atualizacao >= ?"

    private var conn: Connection = instancia

    fun findEnvio(envio: LocalDateTime) : List<ComicInfo> = select(envio)
    fun save(comic: ComicInfo, isSendCloud : Boolean = true, isReceiveCloud: Boolean = false, sincronizacao : LocalDateTime = LocalDateTime.now()) {
        try {
            if (isReceiveCloud) {
                delete(comic.id!!)
                insert(comic, sincronizacao)
            } else if (comic.id == null) {
                comic.id = UUID.randomUUID()
                insert(comic, sincronizacao)
            } else
                update(comic, sincronizacao)

            if (isSendCloud)
                SincronizacaoServices.enviar(comic)
        } catch (e: Exception) {
            mLOG.error("Erro ao salvar o ComicInfo no banco de dados: ${e.message}", e)
            throw e
        }
    }

    fun find(nome: String, linguagem : String) : ComicInfo? = select(nome, linguagem)
    fun find(nome: String) : ComicInfo? = select(nome)

    @Throws(SQLException::class)
    fun select(nome: String, linguagem : String): ComicInfo? {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_COMIC_INFO)
            st.setString(1, linguagem)
            st.setString(2, nome.uppercase())
            st.setString(3, nome.uppercase())
            st.setString(4, nome.uppercase())
            rs = st.executeQuery()
            var comic: ComicInfo? = null
            if (rs.next()) {
                comic = ComicInfo(
                    UUID.fromString(rs.getString("id")), if (rs.getLong("idMal") > 0) rs.getLong("idMal") else null,
                    rs.getString("comic"), rs.getString("title"), rs.getString("series"), rs.getString("publisher"),
                    rs.getString("alternativeSeries"), rs.getString("storyArc"), rs.getString("seriesGroup"),
                    rs.getString("imprint"), rs.getString("genre"), rs.getString("language"),
                    if (rs.getString("maturityRating") != null) AgeRating.valueOf(rs.getString("maturityRating")) else null
                )
            }
            comic
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar o manga.", e)
            throw e
        } finally {
            closeStatement(st)
            closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    fun select(nome: String): ComicInfo? {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_COMIC_INFO_BY_NAME)
            st.setString(1, nome.uppercase())
            st.setString(2, nome.uppercase())
            st.setString(3, nome.uppercase())
            rs = st.executeQuery()
            var comic: ComicInfo? = null
            if (rs.next()) {
                comic = ComicInfo(
                    UUID.fromString(rs.getString("id")), if (rs.getLong("idMal") > 0) rs.getLong("idMal") else null,
                    rs.getString("comic"), rs.getString("title"), rs.getString("series"), rs.getString("publisher"),
                    rs.getString("alternativeSeries"), rs.getString("storyArc"), rs.getString("seriesGroup"),
                    rs.getString("imprint"), rs.getString("genre"), rs.getString("language"),
                    if (rs.getString("maturityRating") != null) AgeRating.valueOf(rs.getString("maturityRating")) else null
                )
            }
            comic
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar o manga.", e)
            throw e
        } finally {
            closeStatement(st)
            closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    private fun update(comic: ComicInfo, sincronizacao : LocalDateTime) {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mUPDATE_COMIC_INFO, Statement.RETURN_GENERATED_KEYS)
            var index = 0
            st.setString(++index, comic.comic)
            st.setLong(++index, comic.idMal ?: 0)
            st.setString(++index, comic.series)
            st.setString(++index, comic.title)
            st.setString(++index, comic.publisher)
            st.setString(++index, comic.genre)
            st.setString(++index, comic.imprint)
            st.setString(++index, comic.seriesGroup)
            st.setString(++index, comic.storyArc)
            st.setString(++index, comic.ageRating?.name)
            st.setString(++index, comic.alternateSeries)
            st.setString(++index, comic.languageISO)
            st.setString(++index, Utils.fromDateTime(sincronizacao))
            st.setString(++index, comic.id.toString())
            val rowsAffected = st.executeUpdate()
            if (rowsAffected < 1) {
                println(st.toString())
                println("Nenhum registro atualizado.")
            }
        } catch (e: SQLException) {
            mLOG.error("Erro ao atualizar o comic info.", e)
            throw e
        } finally {
            closeStatement(st)
        }
    }

    @Throws(Exception::class)
    private fun insert(comic: ComicInfo, sincronizacao : LocalDateTime): UUID? {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mINSERT_COMIC_INFO, Statement.RETURN_GENERATED_KEYS)
            var index = 0
            st.setString(++index, comic.id.toString())
            st.setString(++index, comic.comic)
            st.setLong(++index, comic.idMal ?: 0)
            st.setString(++index, comic.series)
            st.setString(++index, comic.title)
            st.setString(++index, comic.publisher)
            st.setString(++index, comic.genre)
            st.setString(++index, comic.imprint)
            st.setString(++index, comic.seriesGroup)
            st.setString(++index, comic.storyArc)
            st.setString(++index, comic.ageRating?.name)
            st.setString(++index, comic.alternateSeries)
            st.setString(++index, comic.languageISO)
            st.setString(++index, Utils.fromDateTime(sincronizacao))
            st.setString(++index, Utils.fromDateTime(sincronizacao))
            val rowsAffected = st.executeUpdate()
            if (rowsAffected < 1) {
                mLOG.info("Nenhum registro foi inserido.")
                throw Exception("Nenhum registro foi inserido.")
            } else
                return comic.id
        } catch (e: SQLException) {
            println(st.toString()) // Mostrar sql
            mLOG.error("Erro ao inserir o comic info.", e)
            throw e
        } finally {
            closeStatement(st)
        }
        return null
    }

    @Throws(SQLException::class)
    fun delete(id: UUID) {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mDELETE_COMIC_INFO)
            st.setString(1, id.toString())
            st.executeUpdate()
        } catch (e: SQLException) {
            mLOG.error("Erro ao deletar o comic info.", e)
            throw e
        } finally {
            closeStatement(st)
        }
    }

    @Throws(SQLException::class)
    private fun select(envio: LocalDateTime): List<ComicInfo> {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_ENVIO)
            st.setString(1, Utils.fromDateTime(envio))
            rs = st.executeQuery()
            val list = ArrayList<ComicInfo>()
            while (rs.next())
                list.add(
                    ComicInfo(
                    UUID.fromString(rs.getString("id")), if (rs.getLong("idMal") > 0) rs.getLong("idMal") else null,
                    rs.getString("comic"), rs.getString("title"), rs.getString("series"),
                    rs.getString("publisher"),
                    rs.getString("alternativeSeries"), rs.getString("storyArc"), rs.getString("seriesGroup"),
                    rs.getString("imprint"), rs.getString("genre"), rs.getString("language"),
                    if (rs.getString("maturityRating") != null) AgeRating.valueOf(rs.getString("maturityRating")) else null
                )
                )
            list
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar os envios.", e)
            throw e
        } finally {
            closeStatement(st)
            closeResultSet(rs)
        }
    }

    private fun openSiteMal(id: Long) {
        try {
            Desktop.getDesktop().browse(URI("https://myanimelist.net/manga/$id"))
        } catch (e: IOException) {
            mLOG.error(e.message, e)
        } catch (e: URISyntaxException) {
            mLOG.error(e.message, e)
        }
    }

    private fun toTrackerResult(manga: dev.katsute.mal4j.manga.Manga) : TrackerResult {
        val buton = JFXButton("Site")
        buton.styleClass.add("background-White1")
        buton.setOnAction { openSiteMal(manga.id) }

        var imageView : ImageView? = null
        val url = when {
            manga.mainPicture?.largeURL != null -> manga.mainPicture.largeURL
            manga.mainPicture?.mediumURL != null -> manga.mainPicture.mediumURL
            manga.pictures.isNotEmpty() -> {
                when {
                    manga.pictures[0].largeURL != null -> manga.pictures[0].largeURL
                    else -> manga.pictures[0].mediumURL
                }
            }
            else -> null
        }

        if (url != null)
            imageView = ImageView(Image(url, true))

        if (imageView != null) {
            imageView.fitWidth = 170.0
            imageView.fitHeight = 300.0
            imageView.isPreserveRatio = true

            val pause = PauseTransition(Duration.millis(600.0))
            pause.setOnFinished {
                if (url != null) {
                    imageView.image = Image(url, true)
                }
            }
            imageView.setOnMousePressed { e -> if (e.isPrimaryButtonDown) pause.playFromStart() }
            imageView.setOnMouseReleased { pause.stop() }
            imageView.setOnMouseExited { pause.stop() }
            imageView.setOnDragDetected { pause.stop() }
        }

        val altTitles = mutableListOf<String>()
        if (!manga.alternativeTitles.english.isNullOrBlank()) altTitles.add(manga.alternativeTitles.english)
        if (!manga.alternativeTitles.japanese.isNullOrBlank()) altTitles.add(manga.alternativeTitles.japanese)
        if (manga.alternativeTitles.synonyms != null) altTitles.addAll(manga.alternativeTitles.synonyms)

        val genres = manga.genres.map { it.name }
        val authors = manga.authors.map { TrackerAuthor(it.role, it.firstName, it.lastName) }
        val serialization = manga.serialization.map { it.name }

        val metadata = TrackerMetadata(
            id = manga.id,
            title = manga.title,
            alternativeTitles = altTitles,
            genres = genres,
            authors = authors,
            serialization = serialization,
            url = "https://myanimelist.net/manga/${manga.id}",
            type = manga.type?.field() ?: "Manga",
            characters = null // fetched later if needed
        )

        return TrackerResult(
            tracker = TrackerType.MYANIMELIST,
            id = manga.id,
            nome = manga.title,
            descricao = manga.alternativeTitles.japanese + "\n" + manga.alternativeTitles.english,
            site = buton,
            imagem = imageView,
            dados = metadata
        )
    }

    private var MyAnimeLis: MyAnimeList? = null
    
    private fun jaccardSimilarity(s1: String, s2: String): Double {
        val w1 = s1.lowercase().split("\\s+".toRegex()).toSet()
        val w2 = s2.lowercase().split("\\s+".toRegex()).toSet()
        if (w1.isEmpty() && w2.isEmpty()) return 1.0
        val intersection = w1.intersect(w2).size.toDouble()
        val union = w1.union(w2).size.toDouble()
        return intersection / union
    }

    fun getTrackers(idMal: Long?, idAnilist: Long?, nome: String, offset: Int = 0): List<TrackerResult> {
        val executor = Executors.newFixedThreadPool(2)
        val futureMal = CompletableFuture.supplyAsync({
            val lista = mutableListOf<TrackerResult>()
            try {
                if (Configuracao.myAnimeListClient.isBlank()) {
                    mLOG.warn("Não possui o client id do MyAnimeList configurado.")
                    return@supplyAsync lista
                }

                if (MyAnimeLis == null)
                    MyAnimeLis = MyAnimeList.withClientID(Configuracao.myAnimeListClient)

                if (idMal != null) {
                    lista.add(toTrackerResult(MyAnimeLis!!.getManga(idMal)))
                } else if (nome.isNotBlank()) {
                    val query = if (nome.length > 64) nome.substring(0, 64) else nome
                    val limit = Configuracao.registrosConsultaMal
                    val consulta = MyAnimeLis!!.manga.withQuery(query).withLimit(limit).withOffset(offset).search()
                    if (consulta != null && consulta.isNotEmpty()) {
                        for (item in consulta)
                            lista.add(toTrackerResult(item))
                    }
                }
            } catch (e: Exception) {
                mLOG.error("Erro na consulta do MyAnimeList: \${e.message}", e)
            }
            lista
        }, executor)

        val futureAniList = CompletableFuture.supplyAsync({
            if (offset == 0) { // AniList simple search does not paginate in current implementation
                if (idAnilist != null) {
                    AniListTracker.search(idAnilist, "")
                } else if (nome.isNotBlank()) {
                    AniListTracker.search(null, nome)
                } else emptyList()
            } else emptyList()
        }, executor)

        val results = mutableListOf<TrackerResult>()
        try {
            results.addAll(futureMal.get())
            results.addAll(futureAniList.get())
        } catch (e: Exception) {
            mLOG.error("Erro ao aguardar consultas: \${e.message}", e)
        } finally {
            executor.shutdown()
        }

        if (idMal == null && idAnilist == null && nome.isNotBlank()) {
            results.sortByDescending { jaccardSimilarity(nome, it.nome) }
        }

        return results.toList()
    }

    fun getTrackers(id: Long?, nome: String, offset: Int = 0): List<TrackerResult> {
        return getTrackers(id, null, nome, offset)
    }

    private val mDESCRIPTION_MAL = "Tagged with MyAnimeList on "
    private val mDESCRIPTION_ANILIST = "Tagged with AniList on "

    fun updateTracker(comic: ComicInfo, result: TrackerResult, linguagem: Linguagem) {
        val dados = result.dados
        
        if (result.tracker == TrackerType.MYANIMELIST) {
            comic.idMal = dados.id
        } else if (result.tracker == TrackerType.ANILIST) {
            comic.idAnilist = dados.id
        }

        comic.languageISO = linguagem.sigla

        for (author in dados.authors) {
            if (author.role.equals("art", ignoreCase = true)) {
                if (comic.penciller.isNullOrEmpty())
                    comic.penciller = (author.firstName + " " + author.lastName).trim()

                if (comic.inker.isNullOrEmpty())
                    comic.inker = (author.firstName + " " + author.lastName).trim()

                if (comic.coverArtist.isNullOrEmpty())
                    comic.coverArtist = (author.firstName + " " + author.lastName).trim()
            } else if (author.role.equals("story", ignoreCase = true)) {
                if (comic.penciller.isNullOrEmpty())
                    comic.penciller = (author.firstName + " " + author.lastName).trim()
            } else {
                if (author.role.lowercase(Locale.getDefault()).contains("story")) {
                    if (comic.writer.isNullOrEmpty() || comic.penciller.isNullOrEmpty())
                        comic.writer = (author.firstName + " " + author.lastName).trim()
                }
                if (author.role.lowercase(Locale.getDefault()).contains("art")) {
                    if (comic.penciller.isNullOrEmpty())
                        comic.penciller = (author.firstName + " " + author.lastName).trim()

                    if (comic.inker.isNullOrEmpty())
                        comic.inker = (author.firstName + " " + author.lastName).trim()

                    if (comic.coverArtist.isNullOrEmpty())
                        comic.coverArtist = (author.firstName + " " + author.lastName).trim()
                }
            }
        }

        if (comic.genre.isNullOrEmpty() && dados.genres.isNotEmpty()) {
            comic.genre = dados.genres.joinToString("; ")
        }

        comic.series = result.nome
        if (linguagem == Linguagem.PORTUGUESE) {
            val engTitle = dados.alternativeTitles.find { it != result.nome }
            if (!engTitle.isNullOrEmpty()) {
                comic.title = dados.title
                comic.series = engTitle
            }
        } else if (linguagem == Linguagem.JAPANESE) {
            val japTitle = dados.alternativeTitles.find { it.any { c -> c.code in 0x3040..0x30FF || c.code in 0x4E00..0x9FBF } }
            if (!japTitle.isNullOrEmpty())
                comic.title = japTitle
        }


        if (comic.alternateSeries.isNullOrEmpty() && dados.alternativeTitles.isNotEmpty()) {
            val altTitle = dados.alternativeTitles.joinToString("; ")
            if (altTitle.isNotEmpty())
                comic.alternateSeries = altTitle
        }

        if (comic.publisher.isNullOrEmpty() && dados.serialization.isNotEmpty()) {
            comic.publisher = dados.serialization.joinToString("; ")
        }
        
        if (!dados.characters.isNullOrBlank()) {
            comic.characters = dados.characters
        }

        if (comic.web.isNullOrEmpty()) {
            comic.web = dados.url
        } else if (!comic.web!!.contains(dados.url)) {
            comic.web = comic.web + " " + dados.url
        }

        val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val descriptionTag = if (result.tracker == TrackerType.MYANIMELIST) mDESCRIPTION_MAL else mDESCRIPTION_ANILIST
        val trackerIdStr = if (result.tracker == TrackerType.MYANIMELIST) "[Issue ID ${dados.id}]" else "[${result.tracker.label} ID ${dados.id}]"
        
        var notes = ""
        if (comic.notes != null) {
            if (comic.notes!!.contains(";")) {
                for (note in comic.notes!!.split(";"))
                    notes += if (note.lowercase(Locale.getDefault()).contains(descriptionTag.lowercase(Locale.getDefault())))
                        descriptionTag + dateTime.format(LocalDateTime.now()) + ". $trackerIdStr; "
                    else
                        note.trim() + "; "
            } else if (comic.notes!!.lowercase(Locale.getDefault()).contains(descriptionTag.lowercase(Locale.getDefault())))
                notes = descriptionTag + dateTime.format(LocalDateTime.now()) + ". $trackerIdStr; "
            else
                notes += ((comic.notes + "; " + descriptionTag + dateTime.format(LocalDateTime.now())) + ". $trackerIdStr") + "; "
        } else
            notes += descriptionTag + dateTime.format(LocalDateTime.now()) + ". $trackerIdStr; "

        comic.notes = notes.substring(0, notes.lastIndexOf("; "))

        if (result.tracker == TrackerType.MYANIMELIST && comic.characters.isNullOrBlank()) {
            try {
                var responseBody: String? = null
                var attempts = 0
                val maxAttempts = 2

                while (attempts < maxAttempts) {
                    attempts++
                    try {
                        val reqBuilder: HttpRequest.Builder = HttpRequest.newBuilder()
                        val request: HttpRequest = reqBuilder
                            .uri(URI(String.format("https://api.jikan.moe/v4/manga/%s/characters", dados.id)))
                            .timeout(java.time.Duration.ofSeconds(4))
                            .GET()
                            .build()

                        val response: HttpResponse<String> = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString())
                        if (response.statusCode() == 200) {
                            responseBody = response.body()
                            break
                        } else if (response.statusCode() == 429) {
                            mLOG.warn("Limite de requisições do Jikan atingido (429). Tentativa $attempts/$maxAttempts. Aguardando...")
                            Thread.sleep(500L * attempts)
                        } else {
                            mLOG.warn("Falha ao consultar personagens no Jikan. Status: ${response.statusCode()}")
                            break
                        }
                    } catch (e: Exception) {
                        if (attempts >= maxAttempts) {
                            // non-fatal, continue with available data
                        }
                        mLOG.warn("Erro temporário ao consultar personagens no Jikan (tentativa $attempts/$maxAttempts): ${e.message}")
                        Thread.sleep(500L * attempts)
                    }
                }

                if (responseBody != null && responseBody.contains("character")) {
                    val gson = Gson()
                    val element: JsonElement = gson.fromJson(responseBody, JsonElement::class.java)
                    val jsonObject: JsonObject = element.asJsonObject
                    val list: JsonArray? = jsonObject.getAsJsonArray("data")

                    if (list != null) {
                        var characters = ""
                        for (item in list) {
                            val obj: JsonObject? = item?.asJsonObject
                            val characterObj = obj?.getAsJsonObject("character")
                            var character: String? = characterObj?.get("name")?.asString
                            if (character != null) {
                                if (character.contains(", "))
                                    character = character.replace(",", "")
                                else if (character.contains(","))
                                    character = character.replace(",", " ")

                                val role = obj?.get("role")?.asString ?: ""
                                characters += character + if (role.equals("main", true)) " ($role), " else ", "
                            }
                        }
                        if (characters.isNotEmpty())
                            comic.characters = characters.substring(0, characters.lastIndexOf(", ")) + "."
                    }
                }
            } catch (e: Exception) {
                mLOG.error("Erro ao consultar os personagens. " + e.message, e)
            }
        }
    }
}
