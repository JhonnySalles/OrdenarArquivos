package com.fenix.ordenararquivos.model.entities.comicinfo

import jakarta.xml.bind.annotation.*
import java.util.*

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ComicInfo")
data class ComicInfo(
    @field:XmlElement(name = "id")
    var id: UUID? = null,
    @XmlTransient
    var idMal: Long? = null,
    @field:XmlElement(name = "comic")
    var comic: String = "",
    @field:XmlElement(name = "Title")
    var title: String = "",
    @field:XmlElement(name = "Series")
    var series: String = "",
    @field:XmlElement(name = "Number")
    var number: Float = 0f,
    @field:XmlElement(name = "Volume")
    var volume: Int = 0,
    @field:XmlElement(name = "Notes")
    var notes: String? = null,
    @field:XmlElement(name = "Year")
    var year: Int? = null,
    @field:XmlElement(name = "Month")
    var month: Int? = null,
    @field:XmlElement(name = "Day")
    var day: Int? = null,
    @field:XmlElement(name = "Writer")
    var writer: String? = null,
    @field:XmlElement(name = "Penciller")
    var penciller: String? = null,
    @field:XmlElement(name = "Inker")
    var inker: String? = null,
    @field:XmlElement(name = "CoverArtist")
    var coverArtist: String? = null,
    @field:XmlElement(name = "Colorist")
    var colorist: String? = null,
    @field:XmlElement(name = "Letterer")
    var letterer: String? = null,
    @field:XmlElement(name = "Publisher")
    var publisher: String? = null,
    @field:XmlElement(name = "Tags")
    var tags: String? = null,
    @field:XmlElement(name = "Web")
    var web: String? = null,
    @field:XmlElement(name = "Editor")
    var editor: String? = null,
    @field:XmlElement(name = "Translator")
    var translator: String? = null,
    @field:XmlElement(name = "PageCount")
    var pageCount: Int? = null,
    @XmlElementWrapper(name = "Pages")
    @field:XmlElement(name = "Page")
    var pages: List<Pages>? = null,
    @field:XmlElement(name = "Count")
    var count: Int = 0,
    @field:XmlElement(name = "AlternateSeries")
    var alternateSeries: String? = null,
    @field:XmlElement(name = "AlternateNumber")
    var alternateNumber: Float? = null,
    @field:XmlElement(name = "StoryArc")
    var storyArc: String? = null,
    @field:XmlElement(name = "StoryArcNumber")
    var storyArcNumber: String? = null,
    @field:XmlElement(name = "SeriesGroup")
    var seriesGroup: String? = null,
    @field:XmlElement(name = "AlternateCount")
    var alternateCount: Int? = null,
    @field:XmlElement(name = "Summary")
    var summary: String? = null,
    @field:XmlElement(name = "Imprint")
    var imprint: String? = null,
    @field:XmlElement(name = "Genre")
    var genre: String? = null,
    @field:XmlElement(name = "LanguageISO")
    var languageISO: String = "",
    @field:XmlElement(name = "Format")
    var format: String? = null,
    @field:XmlElement(name = "AgeRating")
    var ageRating: AgeRating? = null,
    @field:XmlElement(name = "CommunityRating")
    var communityRating: Float? = null,
    @field:XmlElement(name = "BlackAndWhite")
    var blackAndWhite: YesNo? = null,
    @field:XmlElement(name = "Manga")
    var manga: Manga = Manga.Yes,
    @field:XmlElement(name = "Characters")
    var characters: String? = null,
    @field:XmlElement(name = "Teams")
    var teams: String? = null,
    @field:XmlElement(name = "Locations")
    var locations: String? = null,
    @field:XmlElement(name = "ScanInformation")
    var scanInformation: String? = null,
    @field:XmlElement(name = "MainCharacterOrTeam")
    var mainCharacterOrTeam: String? = null,
    @field:XmlElement(name = "Review")
    var review: String? = null,
    @field:XmlElement(name = "GTIN")
    var gtin: String? = null
) {

    constructor(
        id: UUID?, idMal: Long?, comic: String, title: String, series: String, publisher: String?, alternateSeries: String?,
        storyArc: String?, seriesGroup: String?, imprint: String?, genre: String?, languageISO: String,
        ageRating: AgeRating?
    ) : this(id, idMal, comic, title, series) {
        this.publisher = publisher
        this.alternateSeries = alternateSeries
        this.storyArc = storyArc
        this.seriesGroup = seriesGroup
        this.imprint = imprint
        this.genre = genre
        this.languageISO = languageISO
        this.ageRating = ageRating
    }

    constructor(comic: ComicInfo) : this(
        comic.id, comic.idMal, comic.comic, comic.title, comic.series, comic.number, comic.volume,
        comic.notes, comic.year, comic.month, comic.day, comic.writer, comic.penciller, comic.inker, comic.coverArtist, comic.colorist,
        comic.letterer, comic.publisher, comic.tags, comic.web, comic.editor, comic.translator, comic.pageCount, comic.pages, comic.count,
        comic.alternateSeries, comic.alternateNumber, comic.storyArc, comic.storyArcNumber, comic.seriesGroup, comic.alternateCount,
        comic.summary, comic.imprint, comic.genre, comic.languageISO, comic.format, comic.ageRating, comic.communityRating,
        comic.blackAndWhite, comic.manga, comic.characters, comic.teams, comic.locations, comic.scanInformation,
        comic.mainCharacterOrTeam, comic.review, comic.gtin
    )

    fun merge(comic: ComicInfo) {
        this.id = comic.id
        this.idMal = comic.idMal
        this.comic = comic.comic
        this.title = comic.title
        this.series = comic.series
        this.publisher = comic.publisher
        this.alternateSeries = comic.alternateSeries
        this.storyArc = comic.storyArc
        this.seriesGroup = comic.seriesGroup
        this.imprint = comic.imprint
        this.genre = comic.genre
        this.languageISO = comic.languageISO
        this.ageRating = comic.ageRating
    }

}