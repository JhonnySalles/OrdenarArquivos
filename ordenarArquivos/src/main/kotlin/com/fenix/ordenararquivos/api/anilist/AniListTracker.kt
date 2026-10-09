package com.fenix.ordenararquivos.api.anilist

import com.fenix.ordenararquivos.model.entities.comicinfo.TrackerAuthor
import com.fenix.ordenararquivos.model.entities.comicinfo.TrackerMetadata
import com.fenix.ordenararquivos.model.entities.comicinfo.TrackerResult
import com.fenix.ordenararquivos.model.enums.TrackerType
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jfoenix.controls.JFXButton
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object AniListTracker {
    private val mLOG = LoggerFactory.getLogger(AniListTracker::class.java)
    private val gson = Gson()
    private val mHttpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val mGraphQLSearchQuery = """
        query (${'$'}search: String, ${'$'}id: Int) {
          Page(page: 1, perPage: 50) {
            media(search: ${'$'}search, id: ${'$'}id, type: MANGA) {
              id
              title {
                romaji
                english
                native
              }
              coverImage {
                large
              }
              synonyms
              genres
              staff {
                edges {
                  role
                  node {
                    name {
                      first
                      last
                      full
                    }
                  }
                }
              }
              characters(page: 1, perPage: 15) {
                nodes {
                  name {
                    full
                  }
                }
              }
              format
              description(asHtml: false)
              siteUrl
            }
          }
        }
    """.trimIndent()

    fun search(id: Long?, nome: String): List<TrackerResult> {
        val lista = mutableListOf<TrackerResult>()
        try {
            val variables = JsonObject()
            if (id != null) {
                variables.addProperty("id", id)
            } else {
                variables.addProperty("search", nome)
            }

            val payload = JsonObject()
            payload.addProperty("query", mGraphQLSearchQuery)
            payload.add("variables", variables)

            val request = HttpRequest.newBuilder()
                .uri(URI("https://graphql.anilist.co"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build()

            val response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val responseObj = gson.fromJson(response.body(), JsonObject::class.java)
                val mediaList = responseObj.getAsJsonObject("data")
                    ?.getAsJsonObject("Page")
                    ?.getAsJsonArray("media")

                mediaList?.forEach { item ->
                    val media = item.asJsonObject
                    val mediaId = media.get("id").asLong
                    val formatObj = media.get("format")
                    val format = if (!formatObj.isJsonNull) formatObj.asString else "MANGA"
                    val descObj = media.get("description")
                    val description = if (!descObj.isJsonNull) descObj.asString else ""
                    val url = media.get("siteUrl").asString
                    
                    val titleObj = media.getAsJsonObject("title")
                    val romaji = titleObj?.get("romaji")?.takeIf { !it.isJsonNull }?.asString ?: ""
                    val english = titleObj?.get("english")?.takeIf { !it.isJsonNull }?.asString ?: ""
                    val native = titleObj?.get("native")?.takeIf { !it.isJsonNull }?.asString ?: ""
                    
                    val altTitles = mutableListOf<String>()
                    if (english.isNotBlank()) altTitles.add(english)
                    if (native.isNotBlank()) altTitles.add(native)
                    
                    val synonyms = media.getAsJsonArray("synonyms")
                    synonyms?.forEach { syn -> altTitles.add(syn.asString) }
                    
                    val coverObj = media.getAsJsonObject("coverImage")
                    val imageUrl = coverObj?.get("large")?.takeIf { !it.isJsonNull }?.asString
                    
                    val genresArray = media.getAsJsonArray("genres")
                    val genres = mutableListOf<String>()
                    genresArray?.forEach { g -> genres.add(g.asString) }
                    
                    val authors = mutableListOf<TrackerAuthor>()
                    val staffEdges = media.getAsJsonObject("staff")?.getAsJsonArray("edges")
                    staffEdges?.forEach { edge ->
                        val e = edge.asJsonObject
                        val role = e.get("role").asString
                        val nameObj = e.getAsJsonObject("node").getAsJsonObject("name")
                        val first = nameObj.get("first")?.takeIf { !it.isJsonNull }?.asString ?: ""
                        val last = nameObj.get("last")?.takeIf { !it.isJsonNull }?.asString ?: ""
                        val full = nameObj.get("full")?.takeIf { !it.isJsonNull }?.asString ?: ""
                        if (first.isNotBlank() || last.isNotBlank()) {
                            authors.add(TrackerAuthor(role, first, last))
                        } else if (full.isNotBlank()) {
                            authors.add(TrackerAuthor(role, full, ""))
                        }
                    }
                    
                    val charactersList = mutableListOf<String>()
                    val charNodes = media.getAsJsonObject("characters")?.getAsJsonArray("nodes")
                    charNodes?.forEach { charNode ->
                        val charName = charNode.asJsonObject.getAsJsonObject("name").get("full").asString
                        charactersList.add(charName)
                    }
                    val charactersStr = if (charactersList.isNotEmpty()) charactersList.joinToString(", ") + "." else null

                    val metadata = TrackerMetadata(
                        id = mediaId,
                        title = romaji.ifBlank { english.ifBlank { native } },
                        alternativeTitles = altTitles,
                        genres = genres,
                        authors = authors,
                        serialization = emptyList(), // AniList does not provide magazines in typical search directly without deep connections
                        url = url,
                        type = format,
                        characters = charactersStr
                    )
                    
                    val btn = JFXButton("Site")
                    btn.styleClass.add("background-White1")
                    btn.setOnAction {
                        try { Desktop.getDesktop().browse(URI(url)) } catch (e: Exception) { mLOG.error(e.message, e) }
                    }
                    
                    var imageView: ImageView? = null
                    if (imageUrl != null) {
                        imageView = ImageView(Image(imageUrl, true)).apply {
                            fitWidth = 170.0
                            fitHeight = 300.0
                            isPreserveRatio = true
                        }
                    }
                    
                    val result = TrackerResult(
                        tracker = TrackerType.ANILIST,
                        id = mediaId,
                        nome = metadata.title,
                        descricao = romaji + "\n" + english,
                        site = btn,
                        imagem = imageView,
                        dados = metadata
                    )
                    lista.add(result)
                }
            } else {
                mLOG.warn("Falha na consulta do AniList. Status: ${response.statusCode()}")
            }
        } catch (e: Exception) {
            mLOG.error("Erro ao realizar consulta AniList: \${e.message}", e)
        }
        return lista
    }
}
