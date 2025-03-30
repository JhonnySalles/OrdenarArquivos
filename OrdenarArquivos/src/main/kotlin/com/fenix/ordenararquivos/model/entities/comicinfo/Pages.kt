package com.fenix.ordenararquivos.model.entities.comicinfo

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlRootElement


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Page")
data class Pages(
    @XmlAttribute(name = "Bookmark")
    var bookmark: String? = null,
    @XmlAttribute(name = "Image")
    var image: Int? = null,
    @XmlAttribute(name = "ImageHeight")
    var imageHeight: Int? = null,
    @XmlAttribute(name = "ImageWidth")
    var imageWidth: Int? = null,
    @XmlAttribute(name = "ImageSize")
    var imageSize: Long? = null,
    @XmlAttribute(name = "Type")
    var type: ComicPageType? = null,
    @XmlAttribute(name = "DoublePage")
    var doublePage: Boolean? = null,
    @XmlAttribute(name = "Key")
    var key: String? = null
) { }