package ordenarArquivos.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBase {
	
	private static final Logger LOG = LoggerFactory.getLogger(DataBase.class);
	
	private static final String DATABASE = "ordena.db";
	private static Connection connection;
	
	public static Connection getInstancia() {
		if (connection == null)
			iniciaBanco();
		return connection;
	}
	
	private static void iniciaBanco() {
		try {
            Class.forName("org.sqlite.JDBC");
            DriverManager.registerDriver(new org.sqlite.JDBC());

            Flyway flyway = Flyway.configure()
                    .dataSource("jdbc:sqlite:" + DATABASE, "", "")
                    .locations("filesystem:db/migration")
                    .load();
            flyway.migrate();
            
            connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE);
        } catch (ClassNotFoundException e) { // Driver n�o encontrado
        	LOG.error("O driver de conex�o expecificado nao foi encontrado.", e);
		} catch (SQLException e) {
			LOG.error("Não foi possivel conectar ao Banco de Dados.", e);
		}
	}
	
	public static void closeConnection() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void closeStatement(Statement st) {
		if (st != null) {
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
}
