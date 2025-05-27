package com.fenix.ordenararquivos.model.entities.comet

import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicPageType
import com.fenix.ordenararquivos.model.entities.comicinfo.Manga
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "comet")
data class CoMet(
    @field:XmlElement(name = "title")
    var title: String = "",
    @field:XmlElement(name = "description")
    var description: String? = null,
    @field:XmlElement(name = "series")
    var series: String? = null,
    @field:XmlElement(name = "issue")
    var issue: Int = 0,
    @field:XmlElement(name = "Volume")
    var volume: Int = 0,
    @field:XmlElement(name = "publisher")
    var publisher: String? = null,
    @field:XmlElement(name = "date")
    var date: String? = null,
    @field:XmlElement(name = "genre")
    var genre: Array<String>? = null,
    @field:XmlElement(name = "character")
    var character: Array<String>? = null,
    @field:XmlElement(name = "isVersionOf")
    var isVersionOf: String? = null,
    @field:XmlElement(name = "price")
    var price: Float? = null,
    @field:XmlElement(name = "language")
    var language: String? = null,
    @field:XmlElement(name = "rating")
    var rating: String? = null,
    @field:XmlElement(name = "identifier")
    var identifier: String? = null,
    @field:XmlElement(name = "pages")
    var pages: Int = 0,
    @field:XmlElement(name = "creator")
    var creator: Array<String>? = null,
    @field:XmlElement(name = "writer")
    var writer: Array<String>? = null,
    @field:XmlElement(name = "penciller")
    var penciller: Array<String>? = null,
    @field:XmlElement(name = "editor")
    var editor: Array<String>? = null,
    @field:XmlElement(name = "coverDesigner")
    var coverDesigner: String? = null,
    @field:XmlElement(name = "letterer")
    var letterer: Array<String>? = null,
    @field:XmlElement(name = "inker")
    var inker: Array<String>? = null,
    @field:XmlElement(name = "colorist")
    var colorist: Array<String>? = null,
    @field:XmlElement(name = "coverImage")
    var coverImage: String? = null,
    @field:XmlElement(name = "lastMark")
    var lastMark: Int? = null,
    @field:XmlElement(name = "readingDirection")
    var readingDirection: String? = null
) {
    /**
     * Transforma um ComicInfo em um CoMet.
     * @param comic Arquivo ComicInfo para ser convertido
     * @param paths Lista de endereços do arquivo winrar das imagens embutida composta do nome e pasta. Ex: pasta/arquivo.jpg
     */
    constructor(comic: ComicInfo, paths : List<String>? = null): this() {
        this.toCoMet(comic, paths)
    }

    private fun toStringArray(value: String?, delimiter : String = ","): Array<String>? {
        return value?.replace("$delimiter ", delimiter)?.split(delimiter)?.toTypedArray()
    }

    /**
     * Transforma um ComicInfo em um CoMet.
     * @param comic Arquivo ComicInfo para ser convertido
     * @param paths Lista de endereços do arquivo winrar das imagens embutida composta do nome e pasta. Ex: pasta/arquivo.jpg
     */
    fun toCoMet(comic: ComicInfo, paths : List<String>? = null) {
        this.title = comic.title
        this.series = comic.series
        this.issue = comic.number.toInt()
        this.volume = comic.volume
        this.publisher = comic.publisher
        this.pages = comic.pageCount ?: 0
        this.writer = toStringArray(comic.writer)
        this.penciller = toStringArray(comic.penciller)
        this.inker = toStringArray(comic.inker)
        this.coverDesigner = comic.coverArtist
        this.colorist = toStringArray(comic.colorist)
        this.letterer = toStringArray(comic.letterer)
        this.editor = toStringArray(comic.editor)
        this.character = toStringArray(comic.characters)
        this.creator = toStringArray(comic.writer)
        this.rating = comic.ageRating?.descricao
        this.language = comic.languageISO
        this.identifier = comic.gtin
        this.description = comic.summary

        if (!paths.isNullOrEmpty()) {
            this.coverImage = null
            val cover = comic.pages?.firstOrNull { it.type != null && it.type == ComicPageType.FrontCover } ?: comic.pages?.firstOrNull()
            if (cover != null)
                this.coverImage = paths[cover.image!!]
        }

        this.isVersionOf = null
        this.lastMark = null
        this.price = null

        if (comic.year == null && comic.month == null && comic.day == null)
            this.date = "${comic.year}-${comic.month}-${comic.day}"

        this.readingDirection = when (comic.manga) {
            Manga.Yes, Manga.No -> "ltr"
            Manga.YesAndRightToLeft -> "rtl"
            else -> null
        }

        if (comic.genre != null) {
            if (comic.genre!!.contains(";", ignoreCase = true))
                this.genre = toStringArray(comic.genre, ";")
            else
                this.genre = toStringArray(comic.genre)
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CoMet

        if (title != other.title) return false
        if (series != other.series) return false
        if (volume != other.volume) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + series.hashCode()
        result = 31 * result + volume
        return result
    }
}