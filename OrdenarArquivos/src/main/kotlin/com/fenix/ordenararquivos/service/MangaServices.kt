package com.fenix.ordenararquivos.service

import com.fenix.ordenararquivos.database.*
import com.fenix.ordenararquivos.model.entities.*
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.util.*
import org.slf4j.LoggerFactory
import java.sql.*
import java.time.LocalDateTime

class MangaServices {

    private val mLOG = LoggerFactory.getLogger(MangaServices::class.java)

    private val mUPDATE_MANGA = "UPDATE Manga SET nome = ?, volume = ?, capitulo = ?, arquivo = ?, quantidade = ?, capitulos = ?, atualizacao = ? WHERE id = ?"
    private val mINSERT_MANGA = "INSERT INTO Manga (nome, volume, capitulo, arquivo, quantidade, capitulos, criacao, atualizacao) VALUES (?,?,?,?,?,?,?,?)"
    private val mSELECT_MANGA_ATUAL = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE nome LIKE ? AND volume LIKE ? AND capitulo LIKE ? LIMIT 1"
    private val mSELECT_MANGA_ANTERIOR = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE nome LIKE ? AND capitulo LIKE ? ORDER BY volume DESC LIMIT 1"
    private val mINSERT_CAMINHO = "INSERT INTO Caminho (id_manga, capitulo, pagina, pasta, tag) VALUES (?,?,?,?,?)"
    private val mSELECT_CAMINHO = "SELECT id, capitulo, pagina, pasta, tag FROM Caminho WHERE id_manga = ?"
    private val mDELETE_CAMINHO = "DELETE FROM Caminho WHERE id_manga = ?"
    private val mSELECT_ALL_MANGA = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE nome LIKE ?"
    private val mSELECT_ALL_MANGA_PAGINATED = "SELECT m.id, m.nome, m.volume, m.capitulo, m.arquivo, m.quantidade, m.capitulos, m.atualizacao, c.comic FROM Manga m LEFT JOIN ComicInfo c ON m.nome = c.series OR m.nome = c.title OR m.nome = c.comic WHERE m.nome LIKE ? GROUP BY m.id ORDER BY m.nome, m.volume, m.capitulo LIMIT ? OFFSET ?"
    private val mDELETE_MANGA = "DELETE FROM Manga WHERE id = ?"
    private val mSELECT_MANGA_ID = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE id = ?"
    private val mCOUNT_MANGA_BY_NAME = "SELECT count(id) FROM Manga WHERE nome = ?"

    private val mSELECT_ENVIO = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE atualizacao >= ?"
    private val mLIST_MANGA = "SELECT nome FROM Manga GROUP BY nome ORDER BY nome"
    private val mSUGESTAO = "SELECT nome FROM Manga WHERE lower(nome) LIKE ? GROUP BY nome ORDER BY nome"

    private val mSELECT_MANGA_BY_NOME_VOLUME = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE nome = ? AND volume = ?"
    private val mINSERT_EXCLUIDO = "INSERT INTO Excluido (comic, atualizacao) VALUES (?, ?)"
    private val mDELETE_EXCLUIDO = "DELETE FROM Excluido WHERE comic = ?"
    private val mSELECT_EXCLUIDO_ENVIO = "SELECT comic, atualizacao FROM Excluido WHERE atualizacao >= ?"

    private val conn: Connection get() = DataBase.instancia

    fun find(id: Long): Manga? = selectById(id)

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

    fun findEnvio(envio: LocalDateTime) : List<Manga> = selectByEnvio(envio)

    @JvmOverloads
    fun save(manga: Manga, isSendCloud : Boolean = true, atualizacao : LocalDateTime = LocalDateTime.now()) {
        manga.atualizacao = atualizacao
        try {
            deleteExcluido(manga.nome + " - " + manga.volume)
            if (manga.id == 0L) {
                mLOG.info("Inserindo novo manga: ${manga.nome}")
                insert(manga)
            } else {
                mLOG.info("Atualizando manga existente: ${manga.nome} (ID: ${manga.id})")
                update(manga)
            }

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
    fun findAll(nome: String, limit: Int = 1000, offset: Int = 0, isCaminho: Boolean = false): List<Manga> {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_ALL_MANGA_PAGINATED)
            st.setString(1, "%$nome%")
            st.setInt(2, limit)
            st.setInt(3, offset)
            rs = st.executeQuery()
            val mangas = mutableListOf<Manga>()
            while (rs.next()) {
                val manga = Manga(rs.getLong("id"), rs.getString("nome"), rs.getString("volume"),
                    rs.getString("capitulo"), rs.getString("arquivo"), rs.getInt("quantidade"),
                    rs.getString("capitulos"), Utils.toDateTime(rs.getString("atualizacao"))
                )
                manga.comic = rs.getString("comic") ?: ""
                if (isCaminho)
                    manga.caminhos = selectByManga(manga)
                mangas.add(manga)
            }
            mangas
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar o manga.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
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
                manga.caminhos = selectByManga(manga)
            }
            manga
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar o manga.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
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
                manga.caminhos = selectByManga(manga)
            }
            manga
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar o manga.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
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
            DataBase.closeStatement(st)
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
            DataBase.closeStatement(st)
        }
        return null
    }

    @Throws(SQLException::class)
    private fun selectByManga(manga: Manga): ArrayList<Caminhos> {
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
                        rs.getString("pagina"), rs.getString("pasta"), rs.getString("tag") ?: ""
                    )
                )
            list
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar os caminhos.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    fun deleteManga(manga: Manga) {
        var st: PreparedStatement? = null
        try {
            insertExcluido(manga.nome + " - " + manga.volume)
            delete(manga.id)

            val nome = manga.nome
            val total = countMangaByName(nome)
            if (total <= 1) {
                val comicInfo = ComicInfoServices().find(nome)
                if (comicInfo != null)
                   ComicInfoServices().delete(comicInfo.id!!)
            }

            st = conn.prepareStatement(mDELETE_MANGA)
            st.setLong(1, manga.id)
            st.executeUpdate()
        } catch (e: SQLException) {
            mLOG.error("Erro ao deletar o manga.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
        }
    }

    @Throws(SQLException::class)
    private fun countMangaByName(nome: String): Int {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mCOUNT_MANGA_BY_NAME)
            st.setString(1, nome)
            rs = st.executeQuery()
            if (rs.next()) rs.getInt(1) else 0
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    private fun delete(idManga: Long) {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mDELETE_CAMINHO)
            st.setLong(1, idManga)
            conn.autoCommit = false
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
            DataBase.closeStatement(st)
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
            st.setString(++index, caminho.tag)
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
            DataBase.closeStatement(st)
        }
        return null
    }

    @Throws(SQLException::class)
    fun find(nome: String, volume: String): Manga? {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_MANGA_BY_NOME_VOLUME)
            var index = 0
            st.setString(++index, nome)
            st.setString(++index, volume)
            rs = st.executeQuery()
            var manga: Manga? = null
            if (rs.next()) {
                manga = Manga(
                    rs.getLong("id"), rs.getString("nome"), rs.getString("volume"),
                    rs.getString("capitulo"), rs.getString("arquivo"), rs.getInt("quantidade"),
                    rs.getString("capitulos"), Utils.toDateTime(rs.getString("atualizacao"))
                )
                manga.caminhos = selectByManga(manga)
            }
            manga
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar o manga por Nome e Volume.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    private fun selectById(id: Long): Manga? {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_MANGA_ID)
            st.setLong(1, id)
            rs = st.executeQuery()
            var manga: Manga? = null
            if (rs.next()) {
                manga = Manga(
                    rs.getLong("id"), rs.getString("nome"), rs.getString("volume"),
                    rs.getString("capitulo"), rs.getString("arquivo"), rs.getInt("quantidade"),
                    rs.getString("capitulos"), Utils.toDateTime(rs.getString("atualizacao"))
                )
                manga.caminhos = selectByManga(manga)
            }
            manga
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar o manga por ID.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    private fun selectByEnvio(envio: LocalDateTime): List<Manga> {
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
                manga.caminhos = selectByManga(manga)
                list.add(manga)
            }
            list
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar os envios.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
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
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    fun sugestao(manga: String): List<String> {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSUGESTAO)
            st.setString(1, "%" + manga.lowercase() + "%")
            rs = st.executeQuery()
            val list = ArrayList<String>()
            while (rs.next())
                list.add(rs.getString("nome"))
            list
        } catch (e: SQLException) {
            mLOG.error("Erro ao obter a lista de sugestão.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
        }
    }

    fun findEnvioExclusao(envio: LocalDateTime): List<Pair<String, LocalDateTime>> {
        var st: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            st = conn.prepareStatement(mSELECT_EXCLUIDO_ENVIO)
            st.setString(1, Utils.fromDateTime(envio))
            rs = st.executeQuery()
            val list = mutableListOf<Pair<String, LocalDateTime>>()
            while (rs.next()) {
                list.add(Pair(rs.getString("comic"), Utils.toDateTime(rs.getString("atualizacao"))))
            }
            list
        } catch (e: SQLException) {
            mLOG.error("Erro ao buscar exclusões para envio.", e)
            emptyList()
        } finally {
            DataBase.closeStatement(st)
            DataBase.closeResultSet(rs)
        }
    }

    @Throws(SQLException::class)
    private fun insertExcluido(comic: String, atualizacao: LocalDateTime = LocalDateTime.now()) {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mINSERT_EXCLUIDO)
            st.setString(1, comic)
            st.setString(2, Utils.fromDateTime(atualizacao))
            st.executeUpdate()
        } catch (e: SQLException) {
            mLOG.error("Erro ao inserir na tabela Excluido.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
        }
    }

    @Throws(SQLException::class)
    private fun deleteExcluido(comic: String) {
        var st: PreparedStatement? = null
        try {
            st = conn.prepareStatement(mDELETE_EXCLUIDO)
            st.setString(1, comic)
            st.executeUpdate()
        } catch (e: SQLException) {
            mLOG.error("Erro ao deletar da tabela Excluido.", e)
            throw e
        } finally {
            DataBase.closeStatement(st)
        }
    }

    fun insereExclusao(comic: String, atualizacao: LocalDateTime) {
        try {
            deleteExcluido(comic)
            insertExcluido(comic, atualizacao)
        } catch (e: Exception) {
            mLOG.error("Erro ao registrar exclusão silenciosa.", e)
        }
    }

}
