package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.configuration.Configuracao
import com.fenix.ordenararquivos.database.DataBase.closeResultSet
import com.fenix.ordenararquivos.database.DataBase.closeStatement
import com.fenix.ordenararquivos.database.DataBase.instancia
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.util.Utils
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.jfoenix.controls.JFXButton
import dev.katsute.mal4j.MyAnimeList
import javafx.scene.image.ImageView
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

    private val mUPDATE_COMIC_INFO = "UPDATE ComicInfo SET comic = ?, idMal = ?, series = ?, title = ?, publisher = ?, genre = ?, imprint = ?, seriesGroup = ?, storyArc = ?, maturityRating = ?, alternativeSeries = ?, language = ?,  atualizacao = ? WHERE id = ?"
    private val mINSERT_COMIC_INFO = "INSERT INTO ComicInfo (id, comic, idMal, series, title, publisher, genre, imprint, seriesGroup, storyArc, maturityRating, alternativeSeries, language, criacao, atualizacao) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
    private val mSELECT_COMIC_INFO = "SELECT id, comic, idMal, series, title, publisher, genre, imprint, seriesGroup, storyArc, maturityRating, alternativeSeries, language FROM ComicInfo WHERE language = ? AND (UPPER(comic) LIKE ? or UPPER(series) LIKE ? or UPPER(title) LIKE ?) LIMIT 1"
    private val mDELETE_COMIC_INFO = "DELETE FROM ComicInfo WHERE id = ?"

    private val mSELECT_ENVIO = "SELECT id, comic, idMal, series, title, publisher, genre, imprint, seriesGroup, storyArc, maturityRating, alternativeSeries, language, atualizacao FROM ComicInfo WHERE atualizacao >= ?"

    private var conn: Connection = instancia

    fun findEnvio(envio: LocalDateTime) : List<ComicInfo> = select(envio)
    fun save(comic: ComicInfo, isSendCloud : Boolean = true, isReceiveCloud: Boolean = false) {
        try {
            if (isReceiveCloud) {
                delete(comic.id!!)
                insert(comic)
            } else if (comic.id == null) {
                comic.id = UUID.randomUUID()
                insert(comic)
            } else
                update(comic)

            if (isSendCloud)
                SincronizacaoServices.enviar(comic)
        } catch (e: Exception) {
            mLOG.warn("Erro ao salvar o manga.")
        }
    }

    fun find(nome: String, linguagem : String) : ComicInfo? = select(nome, linguagem)

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
    private fun update(comic: ComicInfo) {
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
            st.setString(++index, if (comic.ageRating != null) comic.ageRating.toString() else null)
            st.setString(++index, comic.alternateSeries)
            st.setString(++index, comic.languageISO)
            st.setString(++index, Utils.fromDateTime(LocalDateTime.now()))
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
    private fun insert(comic: ComicInfo): UUID? {
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
            st.setString(++index, if (comic.ageRating != null) comic.ageRating.toString() else null)
            st.setString(++index, comic.alternateSeries)
            st.setString(++index, comic.languageISO)
            st.setString(++index, Utils.fromDateTime(LocalDateTime.now()))
            st.setString(++index, Utils.fromDateTime(LocalDateTime.now()))
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
    private fun delete(id: UUID) {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mDELETE_COMIC_INFO)
            st.setString(1, id.toString())
            conn.autoCommit = false
            conn.beginRequest()
            st.executeUpdate()
            conn.commit()
        } catch (e: SQLException) {
            try {
                conn.rollback()
            } catch (e1: SQLException) {
                mLOG.error("Erro ao realizar o rollback.", e)
            }
            mLOG.error("Erro ao deletar o comic info.", e)
            throw e
        } finally {
            try {
                conn.autoCommit = true
            } catch (e: SQLException) {
                mLOG.error("Erro ao atualizar o commit.", e)
            }
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

    private fun toMal(manga: dev.katsute.mal4j.manga.Manga) : Mal {
        val buton = JFXButton("Site")
        buton.styleClass.add("background-White1")
        buton.setOnAction { openSiteMal(manga.id) }

        var imageView : ImageView? = null
        if (manga.mainPicture.mediumURL != null)
            imageView = ImageView(manga.mainPicture.mediumURL)
        else if (manga.pictures.isNotEmpty() && manga.pictures[0].mediumURL != null)
            imageView = ImageView(manga.pictures[0].mediumURL)

        if (imageView != null) {
            imageView.fitWidth = 170.0
            imageView.fitHeight = 300.0
            imageView.isPreserveRatio = true
        }

        return Mal(manga.id, manga.title, manga.alternativeTitles.japanese + "\n" + manga.alternativeTitles.english, buton, imageView, manga)
    }

    private var MyAnimeLis: MyAnimeList? = null
    fun getMal(id: Long?, nome : String) : List<Mal> {
        if (MyAnimeLis == null)
            MyAnimeLis = MyAnimeList.withClientID(Configuracao.myAnimeListClient)

        val lista = mutableListOf<Mal>()
        if (id != null)
            lista.add(toMal(MyAnimeLis!!.getManga(id)))
        else {
            val max = 0
            var page = 0
            do {
                mLOG.info("Realizando a consulta $page")
                val consulta = MyAnimeLis!!.manga.withQuery(nome).withLimit(Configuracao.registrosConsultaMal).withOffset(page).search()
                if (consulta != null && consulta.isNotEmpty()) {
                    for (item in consulta)
                        lista.add(toMal(item))
                }
                page++
                if (page > max)
                    break
            } while (consulta != null && consulta.isNotEmpty())
        }
        return lista.toList()
    }

    private val mDESCRIPTION_MAL = "Tagged with MyAnimeList on "
    fun updateMal(comic: ComicInfo, mal: Mal, linguagem : Linguagem) {
        val mal = mal.mal
        comic.idMal = mal.id
        comic.languageISO = linguagem.sigla

        for (author in mal.authors) {
            if (author.role.equals("art", ignoreCase = true)) {
                if (comic.penciller == null || comic.penciller!!.isEmpty())
                    comic.penciller = (author.firstName + " " + author.lastName).trim()

                if (comic.inker == null || comic.inker!!.isEmpty())
                    comic.inker = (author.firstName + " " + author.lastName).trim()

                if (comic.coverArtist == null || comic.coverArtist!!.isEmpty())
                    comic.coverArtist = (author.firstName + " " + author.lastName).trim()
            } else if (author.role.equals("story", ignoreCase = true)) {
                if (comic.penciller == null || comic.penciller!!.isEmpty())
                    comic.penciller = (author.firstName + " " + author.lastName).trim()
            } else {
                if (author.role.lowercase(Locale.getDefault()).contains("story")) {
                    if (comic.writer == null || comic.penciller!!.isEmpty())
                        comic.writer = (author.firstName + " " + author.lastName).trim()
                }
                if (author.role.lowercase(Locale.getDefault()).contains("art")) {
                    if (comic.penciller == null || comic.penciller!!.isEmpty())
                        comic.penciller = (author.firstName + " " + author.lastName).trim()

                    if (comic.inker == null || comic.inker!!.isEmpty())
                        comic.inker = (author.firstName + " " + author.lastName).trim()

                    if (comic.coverArtist == null || comic.coverArtist!!.isEmpty())
                        comic.coverArtist = (author.firstName + " " + author.lastName).trim()
                }
            }
        }

        if (comic.genre == null || comic.genre!!.isEmpty()) {
            var genero = ""
            for (genre in mal.genres)
                genero += genre.name + "; "
            comic.genre = genero.substring(0, genero.lastIndexOf("; "))
        }

        if (linguagem == Linguagem.PORTUGUESE) {
            if (mal.alternativeTitles.english != null && mal.alternativeTitles.english.isNotEmpty()) {
                comic.title = mal.title
                comic.series = mal.alternativeTitles.english
            }
        } else if (linguagem == Linguagem.JAPANESE) {
            if (mal.alternativeTitles.japanese != null && mal.alternativeTitles.japanese.isNotEmpty())
                comic.title = mal.alternativeTitles.japanese
        }

        var title: String = comic.title
        if (comic.alternateSeries == null || comic.alternateSeries!!.isEmpty()) {
            title = ""
            if (mal.alternativeTitles.japanese != null && mal.alternativeTitles.japanese.isNotEmpty())
                title += mal.alternativeTitles.japanese + "; "

            if (mal.alternativeTitles.english != null && mal.alternativeTitles.english.isNotEmpty())
                title += mal.alternativeTitles.english + "; "


            if (mal.alternativeTitles.synonyms != null)
                for (synonym in mal.alternativeTitles.synonyms)
                    title += "$synonym; "

            if (title.isNotEmpty())
                comic.alternateSeries = title.substring(0, title.lastIndexOf("; "))
        }

        if (comic.publisher == null || comic.publisher!!.isEmpty()) {
            var publisher = ""
            for (pub in mal.serialization)
                publisher += pub.name + "; "

            if (publisher.isNotEmpty())
                comic.publisher = publisher.substring(0, publisher.lastIndexOf("; "))
        }

        val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        var notes = ""
        if (comic.notes != null) {
            if (comic.notes!!.contains(";")) {
                for (note in comic.notes!!.split(";"))
                    notes += if (note.lowercase(Locale.getDefault()).contains(mDESCRIPTION_MAL.lowercase(Locale.getDefault())))
                        mDESCRIPTION_MAL + dateTime.format(LocalDateTime.now()) + ". [Issue ID " + mal.id + "]; "
                    else
                        note.trim() + "; "
            } else if (comic.notes!!.lowercase(Locale.getDefault()).contains(mDESCRIPTION_MAL.lowercase(Locale.getDefault())))
                notes = mDESCRIPTION_MAL + dateTime.format(LocalDateTime.now()) + ". [Issue ID " + mal.id + "]; "
            else
                notes += ((comic.notes + "; " + mDESCRIPTION_MAL + dateTime.format(LocalDateTime.now())) + ". [Issue ID " + mal.id) + "]; "
        } else
            notes += mDESCRIPTION_MAL + dateTime.format(LocalDateTime.now()) + ". [Issue ID " + mal.id + "]; "

        comic.notes = notes.substring(0, notes.lastIndexOf("; "))

        try {
            val reqBuilder: HttpRequest.Builder = HttpRequest.newBuilder()
            val request: HttpRequest = reqBuilder
                .uri(URI(String.format("https://api.jikan.moe/v4/manga/%s/characters", mal.id)))
                .GET()
                .build()
            val response: HttpResponse<String> = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString())

            val responseBody: String = response.body()
            if (responseBody.contains("character")) {
                val gson = Gson()
                val element: JsonElement = gson.fromJson(responseBody, JsonElement::class.java)
                val jsonObject: JsonObject = element.asJsonObject
                val list: JsonArray = jsonObject.getAsJsonArray("data")

                var characters = ""
                for (item in list) {
                    val obj: JsonObject = item.asJsonObject
                    var character: String = obj.getAsJsonObject("character").get("name").asString
                    if (character.contains(", "))
                        character = character.replace(",", "")
                    else if (character.contains(","))
                        character = character.replace(",", " ")

                    characters += character + if (obj.get("role").asString.equals("main", true)) " (" + obj.get("role").asString + "), " else ", "
                }
                if (characters.isNotEmpty())
                    comic.characters = characters.substring(0, characters.lastIndexOf(", ")) + "."
            }
        } catch (e: Exception) {
            mLOG.error("Erro ao consultar os personagens. " + e.message, e)
        }
    }

}
