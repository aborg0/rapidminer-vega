package com.rapidminer.operator.io.test;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.DatabaseDataReader;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.test.TestUtils;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.jdbc.DatabaseHandler;

/**
 * 
 * @author Simon Fischer
 *
 */
public class DatabaseWriteTest {

	private static final String TABLE_NAME = "unit_test_table";
	private static final String TEST_DATA_LOCATION = "//Samples/data/Labor-Negotiations";
	private ExampleSet exampleSet;

	private static class DatabaseRef {
		private final String url, user, password;
		private String driverClass;
		private DatabaseRef(String url, String user, String password, String driverClass) {
			super();
			this.url = url;
			this.user = user;
			this.password = password;
			this.setDriverClass(driverClass);
		}
		public String getUrl() {
			return url;
		}
		public String getPassword() {
			return password;
		}
		public String getUser() {
			return user;
		}
		public void setDriverClass(String driverClass) {
			this.driverClass = driverClass;
		}
		/** May be null for bundled drivers. */
		public String getDriverClass() {
			return driverClass;
		}
	}
	
	private static final DatabaseRef DB_SQL_SERVER = new DatabaseRef("jdbc:jtds:sqlserver://192.168.1.8:1433/rapidanalytics", "rapidrepository", "rapidrepository", null);// "net.sourceforge.jtds.jdbc.Driver");
	private static final DatabaseRef DB_MY_SQL = new DatabaseRef("jdbc:mysql://192.168.1.7:3306/test", "rapidi", "rapidi", null); // "com.mysql.jdbc.Driver");
	private static final DatabaseRef DB_ORACLE = new DatabaseRef("jdbc:oracle:thin:@192.168.1.8:1521", "rapidi", "rapidi", "oracle.jdbc.driver.OracleDriver");
	private static final DatabaseRef DB_INGRES = new DatabaseRef("jdbc:sqlserver://192.168.1.8", "rapidi", "rapidi", null);
	
	@Before
	public void setUp() throws Exception {
		TestUtils.initRapidMiner(); // for read database operator
		final Entry entry = new RepositoryLocation(TEST_DATA_LOCATION).locateEntry();
		this.exampleSet = (ExampleSet) ((IOObjectEntry)entry).retrieveData(null);
	}
	
	@Test
	public void testCreateTableMicrosoftSQLServer() throws SQLException, OperatorException, ClassNotFoundException, OperatorCreationException {
		testCreateTable(DB_SQL_SERVER);
	}

	@Test
	public void testCreateTableMySQL() throws SQLException, OperatorException, ClassNotFoundException, OperatorCreationException {
		testCreateTable(DB_MY_SQL);
	}

	@Test
	public void testCreateTableOracle() throws SQLException, OperatorException, ClassNotFoundException, OperatorCreationException {
		testCreateTable(DB_ORACLE);
	}

	@Ignore
	@Test
	public void testCreateTableIngres() throws SQLException, OperatorException, ClassNotFoundException, OperatorCreationException {
		testCreateTable(DB_INGRES);
	}

	private void testCreateTable(DatabaseRef connection) throws SQLException, OperatorException, ClassNotFoundException, OperatorCreationException {
		final String driverClass = connection.getDriverClass();
		if (driverClass != null) {
			Class.forName(driverClass);
		}
		DatabaseHandler handler = DatabaseHandler.getConnectedDatabaseHandler(connection.getUrl(), connection.getUser(), connection.getPassword());
		handler.createTable(exampleSet, TABLE_NAME, DatabaseHandler.OVERWRITE_MODE_OVERWRITE, true, -1);
		
		DatabaseDataReader readOp = OperatorService.createOperator(DatabaseDataReader.class);
		readOp.setParameter(DatabaseHandler.PARAMETER_DEFINE_CONNECTION, DatabaseHandler.CONNECTION_MODES[DatabaseHandler.CONNECTION_MODE_URL]);
		readOp.setParameter(DatabaseHandler.PARAMETER_DATABASE_URL, connection.getUrl());
		readOp.setParameter(DatabaseHandler.PARAMETER_USERNAME, connection.getUser());
		readOp.setParameter(DatabaseHandler.PARAMETER_PASSWORD, connection.getPassword());
		readOp.setParameter(DatabaseHandler.PARAMETER_DEFINE_QUERY, DatabaseHandler.QUERY_MODES[DatabaseHandler.QUERY_TABLE]);
		readOp.setParameter(DatabaseHandler.PARAMETER_TABLE_NAME, TABLE_NAME);
		ExampleSet result = readOp.read();
		
		TestUtils.assertEquals("example set", exampleSet, result, -1);
	}	
}
