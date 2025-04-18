package com.fenix.ordenararquivos.model.entities.comicinfo

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlRootElement


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Page")
data class Pages(
    @field:XmlAttribute(name = "Bookmark")
    var bookmark: String? = null,
    @field:XmlAttribute(name = "Image")
    var image: Int? = null,
    @field:XmlAttribute(name = "ImageHeight")
    var imageHeight: Int? = null,
    @field:XmlAttribute(name = "ImageWidth")
    var imageWidth: Int? = null,
    @field:XmlAttribute(name = "ImageSize")
    var imageSize: Long? = null,
    @field:XmlAttribute(name = "Type")
    var type: ComicPageType? = null,
    @field:XmlAttribute(name = "DoublePage")
    var doublePage: Boolean? = null,
    @field:XmlAttribute(name = "Key")
    var key: String? = null
) { }