package com.fenix.ordenararquivos.model.firebase;

import com.fenix.ordenararquivos.model.comicinfo.AgeRating;

import java.util.HashMap;
import java.util.UUID;

public class ComicInfo {

    private String id;
    private Long idMal;
    private String comic;
    private String title;
    private String series;
    private String publisher;
    private String alternateSeries;
    private String storyArc;
    private String seriesGroup;
    private String imprint;
    private String genre;
    private String languageISO;
    private String ageRating;
    private String sincronizacao;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getIdMal() {
        return idMal;
    }

    public void setIdMal(Long idMal) {
        this.idMal = idMal;
    }

    public String getComic() {
        return comic;
    }

    public void setComic(String comic) {
        this.comic = comic;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getAlternateSeries() {
        return alternateSeries;
    }

    public void setAlternateSeries(String alternateSeries) {
        this.alternateSeries = alternateSeries;
    }

    public String getStoryArc() {
        return storyArc;
    }

    public void setStoryArc(String storyArc) {
        this.storyArc = storyArc;
    }

    public String getSeriesGroup() {
        return seriesGroup;
    }

    public void setSeriesGroup(String seriesGroup) {
        this.seriesGroup = seriesGroup;
    }

    public String getImprint() {
        return imprint;
    }

    public void setImprint(String imprint) {
        this.imprint = imprint;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getLanguageISO() {
        return languageISO;
    }

    public void setLanguageISO(String languageISO) {
        this.languageISO = languageISO;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public String getSincronizacao() {
        return sincronizacao;
    }

    public void setSincronizacao(String sincronizacao) {
        this.sincronizacao = sincronizacao;
    }

    public ComicInfo(String id, Long idMal, String comic, String title, String series, String publisher, String alternateSeries, String storyArc, String seriesGroup, String imprint,
                     String genre, String languageISO, String ageRating, String sincronizacao) {
        this.id = id;
        this.idMal = idMal;
        this.comic = comic;
        this.title = title;
        this.series = series;
        this.publisher = publisher;
        this.alternateSeries = alternateSeries;
        this.storyArc = storyArc;
        this.seriesGroup = seriesGroup;
        this.imprint = imprint;
        this.genre = genre;
        this.languageISO = languageISO;
        this.ageRating = ageRating;
        this.sincronizacao = sincronizacao;
    }

    public ComicInfo(com.fenix.ordenararquivos.model.comicinfo.ComicInfo comic) {
        this.id = comic.getId().toString();
        this.comic = comic.getComic();
        this.title = comic.getTitle();
        this.series = comic.getSeries();
        this.languageISO = comic.getLanguageISO();

        if (comic.getIdMal() != null)
            this.idMal = comic.getIdMal();
        if (comic.getPublisher() != null)
            this.publisher = comic.getPublisher();
        if (comic.getAlternateSeries() != null)
            this.alternateSeries = comic.getAlternateSeries();
        if (comic.getStoryArc() != null)
            this.storyArc = comic.getStoryArc();
        if (comic.getSeriesGroup() != null)
            this.seriesGroup = comic.getSeriesGroup();
        if (comic.getImprint() != null)
            this.imprint = comic.getImprint();
        if (comic.getGenre() != null)
            this.genre = comic.getGenre();
        if (comic.getAgeRating() != null)
            this.ageRating = comic.getAgeRating().toString();
    }

    public static com.fenix.ordenararquivos.model.comicinfo.ComicInfo toComicInfo(HashMap<String, ?> obj) {
        AgeRating rating = null;
        if (obj.containsKey("ageRating"))
            rating = AgeRating.valueOf((String) obj.get("ageRating"));
        return new com.fenix.ordenararquivos.model.comicinfo.ComicInfo(UUID.fromString((String) obj.get("id")), (Long) obj.get("idMal"), (String) obj.get("comic"), (String) obj.get("title"),
                (String) obj.get("series"), (String) obj.get("publisher"), (String) obj.get("alternateSeries"), (String) obj.get("storyArc"), (String) obj.get("seriesGroup"),
                (String) obj.get("imprint"), (String) obj.get("genre"), (String) obj.get("languageISO"), rating);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComicInfo comicInfo = (ComicInfo) o;

        if (title != null ? !title.equals(comicInfo.title) : comicInfo.title != null) return false;
        if (series != null ? !series.equals(comicInfo.series) : comicInfo.series != null) return false;
        return languageISO != null ? languageISO.equals(comicInfo.languageISO) : comicInfo.languageISO == null;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (series != null ? series.hashCode() : 0);
        result = 31 * result + (languageISO != null ? languageISO.hashCode() : 0);
        return result;
    }
}
