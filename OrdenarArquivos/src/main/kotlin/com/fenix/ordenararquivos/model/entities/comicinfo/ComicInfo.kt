package com.fenix.ordenararquivos.model.entities.comicinfo

import jakarta.xml.bind.annotation.*
import java.util.*

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ComicInfo")
data class ComicInfo(
    @XmlElement(name = "id")
    var id: UUID? = null,
    @XmlTransient
    @XmlElement(name = "idMal")
    var idMal: Long? = null,
    @XmlElement(name = "comic")
    var comic: String = "",
    @XmlElement(name = "Title")
    var title: String = "",
    @XmlElement(name = "Series")
    var series: String = "",
    @XmlElement(name = "Number")
    var number: Float = 0f,
    @XmlElement(name = "Volume")
    var volume: Int = 0,
    @XmlElement(name = "Notes")
    var notes: String? = null,
    @XmlElement(name = "Year")
    var year: Int? = null,
    @XmlElement(name = "Month")
    var month: Int? = null,
    @XmlElement(name = "Day")
    var day: Int? = null,
    @XmlElement(name = "Writer")
    var writer: String? = null,
    @XmlElement(name = "Penciller")
    var penciller: String? = null,
    @XmlElement(name = "Inker")
    var inker: String? = null,
    @XmlElement(name = "CoverArtist")
    var coverArtist: String? = null,
    @XmlElement(name = "Colorist")
    var colorist: String? = null,
    @XmlElement(name = "Letterer")
    var letterer: String? = null,
    @XmlElement(name = "Publisher")
    var publisher: String? = null,
    @XmlElement(name = "Tags")
    var tags: String? = null,
    @XmlElement(name = "Web")
    var web: String? = null,
    @XmlElement(name = "Editor")
    var editor: String? = null,
    @XmlElement(name = "Translator")
    var translator: String? = null,
    @XmlElement(name = "PageCount")
    var pageCount: Int? = null,
    @XmlElementWrapper(name = "Pages")
    @XmlElement(name = "Page")
    var pages: List<Pages>? = null,
    @XmlElement(name = "Count")
    var count: Int = 0,
    @XmlElement(name = "AlternateSeries")
    var alternateSeries: String? = null,
    @XmlElement(name = "AlternateNumber")
    var alternateNumber: Float? = null,
    @XmlElement(name = "StoryArc")
    var storyArc: String? = null,
    @XmlElement(name = "StoryArcNumber")
    var storyArcNumber: String? = null,
    @XmlElement(name = "SeriesGroup")
    var seriesGroup: String? = null,
    @XmlElement(name = "AlternateCount")
    var alternateCount: Int? = null,
    @XmlElement(name = "Summary")
    var summary: String? = null,
    @XmlElement(name = "Imprint")
    var imprint: String? = null,
    @XmlElement(name = "Genre")
    var genre: String? = null,
    @XmlElement(name = "LanguageISO")
    var languageISO: String = "",
    @XmlElement(name = "Format")
    var format: String? = null,
    @XmlElement(name = "AgeRating")
    var ageRating: AgeRating? = null,
    @XmlElement(name = "CommunityRating")
    var communityRating: Float? = null,
    @XmlElement(name = "BlackAndWhite")
    var blackAndWhite: YesNo? = null,
    @XmlElement(name = "Manga")
    var manga: Manga = Manga.Yes,
    @XmlElement(name = "Characters")
    var characters: String? = null,
    @XmlElement(name = "Teams")
    var teams: String? = null,
    @XmlElement(name = "Locations")
    var locations: String? = null,
    @XmlElement(name = "ScanInformation")
    var scanInformation: String? = null,
    @XmlElement(name = "MainCharacterOrTeam")
    var mainCharacterOrTeam: String? = null,
    @XmlElement(name = "Review")
    var review: String? = null
) {

    constructor(
        id: UUID, idMal: Long?, comic: String, title: String, series: String, publisher: String?, alternateSeries: String?,
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