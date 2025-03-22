package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.database.DataBase.closeResultSet
import com.fenix.ordenararquivos.database.DataBase.closeStatement
import com.fenix.ordenararquivos.database.DataBase.instancia
import com.fenix.ordenararquivos.model.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.comicinfo.ComicInfo
import com.fenix.ordenararquivos.util.Utils
import org.slf4j.LoggerFactory
import java.sql.*
import java.time.LocalDateTime
import java.util.*


class ComicInfoServices {

    private val mLOG = LoggerFactory.getLogger(ComicInfoServices::class.java)

    private val mUPDATE_COMIC_INFO = "UPDATE ComicInfo SET comic = ?, idMal = ?, series = ?, title = ?, publisher = ?, genre = ?, imprint = ?, seriesGroup = ?, storyArc = ?, maturityRating = ?, alternativeSeries = ?, language = ?,  atualizacao = ? WHERE id = ?"
    private val mINSERT_COMIC_INFO = "INSERT INTO ComicInfo (id, comic, idMal, series, title, publisher, genre, imprint, seriesGroup, storyArc, maturityRating, alternativeSeries, language, criacao, atualizacao) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
    private val mSELECT_COMIC_INFO = "SELECT id, comic, idMal, series, title, publisher, genre, imprint, seriesGroup, storyArc, maturityRating, alternativeSeries, language FROM ComicInfo WHERE UPPER(comic) LIKE ? or UPPER(series) LIKE ? or UPPER(title) LIKE ? LIMIT 1"
    private val mDELETE_COMIC_INFO = "DELETE FROM ComicInfo WHERE id = ?"

    private val mSELECT_ENVIO = "SELECT id, comic, idMal, series, title, publisher, genre, imprint, seriesGroup, storyArc, maturityRating, alternativeSeries, language, atualizacao FROM Manga WHERE atualizacao >= ?"

    private var conn: Connection = instancia

    fun findEnvio(envio: LocalDateTime) : List<ComicInfo> = select(envio)
    fun find(nome: String) : ComicInfo? = select(nome)

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

    @Throws(SQLException::class)
    fun select(nome: String): ComicInfo? {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_COMIC_INFO)
            st.setString(1, nome)
            st.setString(2, nome)
            st.setString(3, nome)
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
                list.add(ComicInfo(
                    UUID.fromString(rs.getString("id")), if (rs.getLong("idMal") > 0) rs.getLong("idMal") else null,
                    rs.getString("comic"), rs.getString("title"), rs.getString("series"),
                    rs.getString("publisher"),
                    rs.getString("alternativeSeries"), rs.getString("storyArc"), rs.getString("seriesGroup"),
                    rs.getString("imprint"), rs.getString("genre"), rs.getString("language"),
                    if (rs.getString("maturityRating") != null) AgeRating.valueOf(rs.getString("maturityRating")) else null
                ))
            list
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar os envios.", e)
            throw e
        } finally {
            closeStatement(st)
            closeResultSet(rs)
        }
    }

}
