package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.database.DataBase.closeResultSet
import com.fenix.ordenararquivos.database.DataBase.closeStatement
import com.fenix.ordenararquivos.database.DataBase.instancia
import com.fenix.ordenararquivos.model.Caminhos
import com.fenix.ordenararquivos.model.Manga
import org.slf4j.LoggerFactory
import java.sql.*
import java.time.LocalDateTime

class MangaServices {

    private val mLOG = LoggerFactory.getLogger(MangaServices::class.java)

    private val mUPDATE_MANGA = "UPDATE Manga SET nome = ?, volume = ?, capitulo = ?, arquivo = ?, quantidade = ?, capitulos = ?, atualizacao = ? WHERE id = ?"
    private val mINSERT_MANGA = "INSERT INTO Manga (nome, volume, capitulo, arquivo, quantidade, capitulos, criacao, atualizacao) VALUES (?,?,?,?,?,?,?,?)"
    private val mSELECT_MANGA = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE nome LIKE ? AND volume LIKE ? AND capitulo LIKE ? LIMIT 1"
    private val mINSERT_CAMINHO = "INSERT INTO Caminho (id_manga, capitulo, pagina, pasta) VALUES (?,?,?,?)"
    private val mSELECT_CAMINHO = "SELECT id, capitulo, pagina, pasta FROM Caminho WHERE id_manga = ?"
    private val mDELETE_CAMINHO = "DELETE FROM Caminho WHERE id_manga = ?"

    private var conn: Connection = instancia

    fun find(manga: Manga): Manga? {
        return find(manga.nome, manga.volume, manga.capitulo)
    }

    fun find(nome: String, volume: String, capitulo: String): Manga? {
        return try {
            select(nome, volume, capitulo)
        } catch (e: SQLException) {
            mLOG.warn("Erro ao buscar o manga.")
            null
        }
    }

    fun save(manga: Manga) {
        manga.atualizacao = LocalDateTime.now()
        try {
            if (manga.id == 0L)
                insert(manga)
            else
                update(manga)

            delete(manga.id)
            for (caminho in manga.caminhos)
                insert(manga.id, caminho)
        } catch (e: Exception) {
            mLOG.warn("Erro ao salvar o manga.")
        }
    }

    @Throws(SQLException::class)
    private fun select(nome: String, volume: String, capitulo: String): Manga? {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_MANGA)
            st.setString(1, nome)
            st.setString(2, volume)
            st.setString(3, capitulo)
            rs = st.executeQuery()
            var manga: Manga? = null
            if (rs.next()) {
                manga = Manga(
                    rs.getLong("id"), rs.getString("nome"), rs.getString("volume"),
                    rs.getString("capitulo"), rs.getString("arquivo"), rs.getInt("quantidade"),
                    rs.getString("capitulos"), toDateTime(rs.getString("atualizacao"))
                )
                manga.caminhos = select(manga)
            }
            manga
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar o manga.", e)
            throw e
        } finally {
            closeStatement(st)
            closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    private fun update(manga: Manga) {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mUPDATE_MANGA, Statement.RETURN_GENERATED_KEYS)
            st.setString(1, manga.nome)
            st.setString(2, manga.volume)
            st.setString(3, manga.capitulo)
            st.setString(4, manga.arquivo)
            st.setInt(5, manga.quantidade)
            st.setString(6, manga.capitulos)
            st.setString(7, fromDateTime(manga.atualizacao))
            st.setLong(8, manga.id)
            val rowsAffected = st.executeUpdate()
            if (rowsAffected < 1) {
                println(st.toString())
                println("Nenhum registro atualizado.")
            }
        } catch (e: SQLException) {
            mLOG.error("Erro ao atualizar o manga.", e)
            throw e
        } finally {
            closeStatement(st)
        }
    }

    @Throws(Exception::class)
    private fun insert(manga: Manga): Long? {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mINSERT_MANGA, Statement.RETURN_GENERATED_KEYS)
            var index = 0;
            st.setString(++index, manga.nome)
            st.setString(++index, manga.volume)
            st.setString(++index, manga.capitulo)
            st.setString(++index, manga.arquivo)
            st.setInt(++index, manga.quantidade)
            st.setString(++index, manga.capitulos)
            st.setString(++index, fromDateTime(LocalDateTime.now()))
            st.setString(++index, fromDateTime(manga.atualizacao))
            val rowsAffected = st.executeUpdate()
            if (rowsAffected < 1) {
                mLOG.info("Nenhum registro encontrado.")
                throw Exception("Nenhum registro foi inserido.")
            } else {
                val rs = st.generatedKeys
                if (rs.next()) {
                    manga.id = rs.getLong(1)
                    return manga.id
                }
            }
        } catch (e: SQLException) {
            mLOG.error("Erro ao inserir o manga.", e)
            throw e
        } finally {
            closeStatement(st)
        }
        return null
    }

    @Throws(SQLException::class)
    private fun select(manga: Manga): ArrayList<Caminhos> {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_CAMINHO)
            st.setLong(1, manga.id)
            rs = st.executeQuery()
            val list = ArrayList<Caminhos>()
            while (rs.next()) list.add(
                Caminhos(
                    rs.getLong("id"), manga, rs.getString("capitulo"), rs.getInt("pagina"),
                    rs.getString("pasta")
                )
            )
            list
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar os caminhos.", e)
            throw e
        } finally {
            closeStatement(st)
            closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    private fun delete(idCaminho: Long) {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mDELETE_CAMINHO)
            st.setLong(1, idCaminho)
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
            mLOG.error("Erro ao deletar os caminhos.", e)
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

    @Throws(Exception::class)
    private fun insert(idManga: Long, caminho: Caminhos): Long? {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mINSERT_CAMINHO, Statement.RETURN_GENERATED_KEYS)
            var index = 0
            st.setLong(++index, idManga)
            st.setString(++index, caminho.capitulo)
            st.setInt(++index, caminho.numero)
            st.setString(++index, caminho.nomePasta)
            val rowsAffected = st.executeUpdate()
            if (rowsAffected < 1) {
                mLOG.info("Nenhum caminho foi inserido.")
                throw Exception("Nenhum registro foi inserido.")
            } else {
                val rs = st.generatedKeys
                if (rs.next()) {
                    caminho.id = rs.getLong(1)
                    return caminho.id
                }
            }
        } catch (e: SQLException) {
            mLOG.error("Erro ao inserir os caminhos.", e)
            throw e
        } finally {
            closeStatement(st)
        }
        return null
    }

    companion object {
        private fun toDateTime(dateTime: String): LocalDateTime {
            return if (dateTime.isEmpty()) LocalDateTime.MIN else LocalDateTime.parse(dateTime)
        }

        private fun fromDateTime(dateTime: LocalDateTime): String {
            return dateTime.toString()
        }
    }
}