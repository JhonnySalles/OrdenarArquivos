package ordenarArquivos.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ordenarArquivos.database.DataBase;
import ordenarArquivos.model.Caminhos;
import ordenarArquivos.model.Manga;

public class MangaServices {

	private static final Logger LOG = LoggerFactory.getLogger(MangaServices.class);

	private Connection conn;

	final private String UPDATE_MANGA = "UPDATE Manga SET nome = ?, volume = ?, capitulo = ?, arquivo = ?, quantidade = ?, capitulos = ?, atualizacao = ? WHERE id = ?";
	final private String INSERT_MANGA = "INSERT INTO Manga (nome, volume, capitulo, arquivo, quantidade, capitulos, criacao, atualizacao) VALUES (?,?,?,?,?,?,?,?)";
	final private String SELECT_MANGA = "SELECT id, nome, volume, capitulo, arquivo, quantidade, capitulos, atualizacao FROM Manga WHERE nome LIKE ? AND volume LIKE ? AND capitulo LIKE ? LIMIT 1";

	final private String INSERT_CAMINHO = "INSERT INTO Caminho (id_manga, capitulo, pagina, pasta) VALUES (?,?,?,?)";
	final private String SELECT_CAMINHO = "SELECT id, capitulo, pagina, pasta FROM Caminho WHERE id_manga = ?";
	final private String DELETE_CAMINHO = "DELETE FROM Caminho WHERE id_manga = ?";

	public MangaServices() {
		this.conn = DataBase.getInstancia();
	}

	private static LocalDateTime toDateTime(String dateTime) {
		if (dateTime == null)
			return null;

		return LocalDateTime.parse(dateTime);
	}

	private static String fromDateTime(LocalDateTime dateTime) {
		if (dateTime == null)
			return null;

		return dateTime.toString();
	}

	public Manga find(Manga manga) {
		return find(manga.getNome(), manga.getVolume(), manga.getCapitulo());
	}

	public Manga find(String nome, String volume, String capitulo) {
		try {
			return select(nome, volume, capitulo);
		} catch (SQLException e) {
			LOG.warn("Erro ao buscar o manga.");
			return null;
		}
	}

	public void save(Manga manga) {
		if (manga == null)
			return;

		manga.setAtualizacao(LocalDateTime.now());

		try {
			if (manga.getId() == null)
				insert(manga);
			else
				update(manga);

			delete(manga.getId());
			for (Caminhos caminho : manga.getCaminhos())
				insert(manga.getId(), caminho);

		} catch (Exception e) {
			LOG.warn("Erro ao salvar o manga.");
		}
	}

	private Manga select(String nome, String volume, String capitulo) throws SQLException {
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement(SELECT_MANGA);
			st.setString(1, nome);
			st.setString(2, volume);
			st.setString(3, capitulo);
			rs = st.executeQuery();

			Manga manga = null;

			if (rs.next()) {
				manga = new Manga(rs.getLong("id"), rs.getString("nome"), rs.getString("volume"),
						rs.getString("capitulo"), rs.getString("arquivo"), rs.getInt("quantidade"),
						rs.getString("capitulos"), toDateTime(rs.getString("atualizacao")));
				manga.setCaminhos(select(manga));
			}

			return manga;
		} catch (SQLException e) {
			LOG.error("Erro ao buscar o manga.", e);
			throw e;
		} finally {
			DataBase.closeStatement(st);
			DataBase.closeResultSet(rs);
		}
	}

	private void update(Manga manga) throws SQLException {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(UPDATE_MANGA, Statement.RETURN_GENERATED_KEYS);

			st.setString(1, manga.getNome());
			st.setString(2, manga.getVolume());
			st.setString(3, manga.getCapitulo());
			st.setString(4, manga.getArquivo());
			st.setInt(5, manga.getQuantidade());
			st.setString(6, manga.getCapitulos());
			st.setString(7, fromDateTime(manga.getAtualizacao()));

			int rowsAffected = st.executeUpdate();

			if (rowsAffected < 1) {
				System.out.println(st.toString());
				System.out.println("Nenhum registro atualizado.");
			}
		} catch (SQLException e) {
			LOG.error("Erro ao atualizar o manga.", e);
			throw e;
		} finally {
			DataBase.closeStatement(st);
		}
	}

	private Long insert(Manga manga) throws Exception {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(INSERT_MANGA, Statement.RETURN_GENERATED_KEYS);

			st.setString(1, manga.getNome());
			st.setString(2, manga.getVolume());
			st.setString(3, manga.getCapitulo());
			st.setString(4, manga.getArquivo());
			st.setInt(5, manga.getQuantidade());
			st.setString(6, manga.getCapitulos());
			st.setString(7, fromDateTime(LocalDateTime.now()));
			st.setString(8, fromDateTime(manga.getAtualizacao()));

			int rowsAffected = st.executeUpdate();

			if (rowsAffected < 1) {
				LOG.info("Nenhum registro encontrado.");
				throw new Exception("Nenhum registro foi inserido.");
			} else {
				ResultSet rs = st.getGeneratedKeys();
				if (rs.next()) {
					manga.setId(rs.getLong(1));
					return manga.getId();
				}
			}

		} catch (SQLException e) {
			LOG.error("Erro ao inserir o manga.", e);
			throw e;
		} finally {
			DataBase.closeStatement(st);
		}
		return null;
	}

	private ArrayList<Caminhos> select(Manga manga) throws SQLException {
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement(SELECT_CAMINHO);
			st.setLong(1, manga.getId());
			rs = st.executeQuery();

			ArrayList<Caminhos> list = new ArrayList<>();

			while (rs.next())
				list.add(new Caminhos(rs.getLong("id"), manga, rs.getString("capitulo"), rs.getInt("pagina"),
						rs.getString("pasta")));

			return list;
		} catch (SQLException e) {
			LOG.error("Erro ao buscar os caminhos.", e);
			throw e;
		} finally {
			DataBase.closeStatement(st);
			DataBase.closeResultSet(rs);
		}
	}

	private void delete(Long idCaminho) throws SQLException {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(DELETE_CAMINHO);
			st.setLong(1, idCaminho);
			conn.setAutoCommit(false);
			conn.beginRequest();
			st.executeUpdate();
			conn.commit();
		} catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException e1) {
				LOG.error("Erro ao realizar o rollback.", e);
			}

			LOG.error("Erro ao deletar os caminhos.", e);
			throw e;
		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e) {
				LOG.error("Erro ao atualizar o commit.", e);
			}
			DataBase.closeStatement(st);
		}
	}

	private Long insert(Long idManga, Caminhos caminho) throws Exception {
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(INSERT_CAMINHO, Statement.RETURN_GENERATED_KEYS);

			st.setLong(1, idManga);
			st.setString(2, caminho.getCapitulo());
			st.setInt(3, caminho.getNumero());
			st.setString(4, caminho.getNomePasta());

			int rowsAffected = st.executeUpdate();

			if (rowsAffected < 1) {
				LOG.info("Nenhum caminho foi inserido.");
				throw new Exception("Nenhum registro foi inserido.");
			} else {
				ResultSet rs = st.getGeneratedKeys();
				if (rs.next()) {
					caminho.setId(rs.getLong(1));
					return caminho.getId();
				}
			}
		} catch (SQLException e) {
			LOG.error("Erro ao inserir os caminhos.", e);
			throw e;
		} finally {
			DataBase.closeStatement(st);
		}
		return null;
	}

}
