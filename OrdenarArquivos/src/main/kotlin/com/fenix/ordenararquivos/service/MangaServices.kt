package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.database.DataBase.closeResultSet
import com.fenix.ordenararquivos.database.DataBase.closeStatement
import com.fenix.ordenararquivos.database.DataBase.instancia
import com.fenix.ordenararquivos.model.entities.Caminhos
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.util.Utils
import org.slf4j.LoggerFactory
import java.sql.*
import java.time.LocalDateTime

class MangaServices {

    private val mLOG = LoggerFactory.getLogger(MangaServices::class.java)

    private val mUPDATE_MANGA = "UPDATE Manga SET nome = ?, volume = ?, capitulo = ?, arquivo = ?, quantidade = ?, capitulos = ?, atualizacao = ? WHERE id = ?"
    private val mINSERT_MANGA = "INSERT INTO Manga (nome, volume, capitulo, arquivo, quantidade, capitulos, criacao, atualizacao) VALUES (?,?,?,?,?,?,?,?)"
    private val mSELECT_MANGA_ATUAL = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE nome LIKE ? AND volume LIKE ? AND capitulo LIKE ? LIMIT 1"
    private val mSELECT_MANGA_ANTERIOR = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE nome LIKE ? AND capitulo LIKE ? ORDER BY volume DESC LIMIT 1"
    private val mINSERT_CAMINHO = "INSERT INTO Caminho (id_manga, capitulo, pagina, pasta) VALUES (?,?,?,?)"
    private val mSELECT_CAMINHO = "SELECT id, capitulo, pagina, pasta FROM Caminho WHERE id_manga = ?"
    private val mDELETE_CAMINHO = "DELETE FROM Caminho WHERE id_manga = ?"
    private val mSELECT_ALL_MANGA = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE nome LIKE ?"

    private val mSELECT_ENVIO = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE atualizacao >= ?"
    private val mLIST_MANGA = "SELECT nome FROM Manga GROUP BY nome ORDER BY nome"

    private var conn: Connection = instancia

    fun find(manga: Manga, anterior : Boolean = false): Manga? {
        return find(manga.nome, manga.volume, manga.capitulo, anterior)
    }

    fun find(nome: String, volume: String, capitulo: String, anterior : Boolean = false): Manga? {
        return try {
            if (anterior)
                selectAnterior(nome, capitulo)
            else
                selectAtual(nome, volume, capitulo)
        } catch (e: SQLException) {
            mLOG.warn("Erro ao buscar o manga.")
            null
        }
    }

    fun findEnvio(envio: LocalDateTime) : List<Manga> = select(envio)

    fun save(manga: Manga, isSendCloud : Boolean = true, atualizacao : LocalDateTime = LocalDateTime.now()) {
        manga.atualizacao = atualizacao
        try {
            if (manga.id == 0L)
                insert(manga)
            else
                update(manga)

            delete(manga.id)
            for (caminho in manga.caminhos)
                insert(manga.id, caminho)

            if (isSendCloud)
                SincronizacaoServices.enviar(manga)
        } catch (e: Exception) {
            mLOG.warn("Erro ao salvar o manga.")
        }
    }

    @Throws(SQLException::class)
    fun findAll(nome: String): List<Manga> {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_ALL_MANGA)
            st.setString(1, nome)
            rs = st.executeQuery()
            val mangas = mutableListOf<Manga>()
            while (rs.next()) {
                val manga = Manga(rs.getLong("id"), rs.getString("nome"), rs.getString("volume"),
                    rs.getString("capitulo"), rs.getString("arquivo"), rs.getInt("quantidade"),
                    rs.getString("capitulos"), Utils.toDateTime(rs.getString("atualizacao"))
                )
                manga.caminhos = select(manga)
                mangas.add(manga)
            }
            mangas
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar o manga.", e)
            throw e
        } finally {
            closeStatement(st)
            closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    private fun selectAtual(nome: String, volume: String, capitulo: String): Manga? {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_MANGA_ATUAL)
            st.setString(1, nome)
            st.setString(2, volume)
            st.setString(3, capitulo)
            rs = st.executeQuery()
            var manga: Manga? = null
            if (rs.next()) {
                manga = Manga(
                    rs.getLong("id"), rs.getString("nome"), rs.getString("volume"),
                    rs.getString("capitulo"), rs.getString("arquivo"), rs.getInt("quantidade"),
                    rs.getString("capitulos"), Utils.toDateTime(rs.getString("atualizacao"))
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
    private fun selectAnterior(nome: String, capitulo: String): Manga? {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_MANGA_ANTERIOR)
            st.setString(1, nome)
            st.setString(2, capitulo)
            rs = st.executeQuery()
            var manga: Manga? = null
            if (rs.next()) {
                manga = Manga(
                        rs.getLong("id"), rs.getString("nome"), rs.getString("volume"),
                        rs.getString("capitulo"), rs.getString("arquivo"), rs.getInt("quantidade"),
                        rs.getString("capitulos"), Utils.toDateTime(rs.getString("atualizacao"))
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
            st.setString(7, Utils.fromDateTime(manga.atualizacao))
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
            st.setString(++index, Utils.fromDateTime(LocalDateTime.now()))
            st.setString(++index, Utils.fromDateTime(manga.atualizacao))
            val rowsAffected = st.executeUpdate()
            if (rowsAffected < 1) {
                mLOG.info("Nenhum registro foi inserido.")
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
            while (rs.next())
                list.add(
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

    @Throws(SQLException::class)
    private fun select(envio: LocalDateTime): List<Manga> {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_ENVIO)
            st.setString(1, Utils.fromDateTime(envio))
            rs = st.executeQuery()
            val list = ArrayList<Manga>()
            while (rs.next()) {
                val manga = Manga(rs.getLong("id"), rs.getString("nome"), rs.getString("volume"),
                    rs.getString("capitulo"), rs.getString("arquivo"), rs.getInt("quantidade"),
                    rs.getString("capitulos"), Utils.toDateTime(rs.getString("atualizacao"))
                )
                manga.caminhos = select(manga)
                list.add(manga)
            }
            list
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar os envios.", e)
            throw e
        } finally {
            closeStatement(st)
            closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    fun listar(): List<String> {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mLIST_MANGA)
            rs = st.executeQuery()
            val list = ArrayList<String>()
            while (rs.next())
                list.add(rs.getString("nome"))
            list
        } catch (e: SQLException) {
            mLOG.error("Erro ao listar os mangas.", e)
            throw e
        } finally {
            closeStatement(st)
            closeResultSet(rs)
        }
    }

}
