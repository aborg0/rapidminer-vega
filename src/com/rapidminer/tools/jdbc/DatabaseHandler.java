/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDatabaseConnection;
import com.rapidminer.parameter.ParameterTypeDatabaseTable;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypeSQLQuery;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;


/**
 * <p>This class hides the database. Using
 * {@link DatabaseHandler#connect(String,String,boolean)}, you can extablish a
 * connection to the database. Once connected, queries and updates are possible.</p>
 * 
 * <p>Please note that the queries does not end with the statement terminator (e.g. 
 * ; for Oracle or GO for Sybase). The JDBC driver will automatically add the correct
 * terminator.</p>
 * 
 * @author Ingo Mierswa
 */
public class DatabaseHandler {
	
	public static final String PARAMETER_DEFINE_CONNECTION = "define_connection";
	
	public static final String[] CONNECTION_MODES = { "predefined" , "url" };
	
	public static final int CONNECTION_MODE_PREDEFINED = 0;
	
	public static final int CONNECTION_MODE_URL = 1;
	
	public static final String PARAMETER_CONNECTION = "connection";
	
	/** The parameter name for &quot;Indicates the used database system&quot; */
	public static final String PARAMETER_DATABASE_SYSTEM = "database_system";

	/** The parameter name for &quot;The complete URL connection string for the database, e.g. 'jdbc:mysql://foo.bar:portnr/database'&quot; */
	public static final String PARAMETER_DATABASE_URL = "database_url";

	/** The parameter name for &quot;Database username.&quot; */
	public static final String PARAMETER_USERNAME = "username";

	/** The parameter name for &quot;Password for the database.&quot; */
	public static final String PARAMETER_PASSWORD = "password";

	public static final String PARAMETER_DEFINE_QUERY = "define_query";
	
	public static final String[] QUERY_MODES = { "query" , "query file" , "table name" };
	
	public static final int QUERY_QUERY = 0;
	
	public static final int QUERY_FILE  = 1;
	
	public static final int QUERY_TABLE = 2;
	
	/** The parameter name for &quot;SQL query. If not set, the query is read from the file specified by 'query_file'.&quot; */
	public static final String PARAMETER_QUERY = "query";
	
	/** The parameter name for &quot;File containing the query. Only evaluated if 'query' is not set.&quot; */
	public static final String PARAMETER_QUERY_FILE = "query_file";

	/** The parameter name for &quot;Use this table if work_on_database is true or no other query is specified.&quot; */
	public static final String PARAMETER_TABLE_NAME = "table_name";
	
	public static final String[] OVERWRITE_MODES = new String[] {
		"none",
		"overwrite first, append then",
		"overwrite",
		"append"
	};
	
	public static final int OVERWRITE_MODE_NONE            = 0;
	
	public static final int OVERWRITE_MODE_OVERWRITE_FIRST = 1;
	
	public static final int OVERWRITE_MODE_OVERWRITE       = 2;
	
	public static final int OVERWRITE_MODE_APPEND          = 3;

	
	/** Used for logging purposes. */
	private String databaseURL;

    /** The properties of this JDBC driver and connection. */
    private JDBCProperties properties;
    
	/** The 'singleton' connection. Each database handler can handle one single connection to 
	 *  a database. Will be null before the connection is established and will also be null
	 *  after {@link #disconnect()} was invoked. */
	private Connection connection;
    
	
	/**
	 * Constructor of the database handler. This constructor expects the URL definition
     * of the database which is needed by the System DriverManager to create an appropriate
     * driver. Please note that this database handler still must be connected via invoking
     * the method {@link #connect(String, String, boolean)}. If you want to directly use
     * a connected database handler you might use the static method 
     * {@link #getConnectedDatabaseHandler(String, String, String, JDBCProperties, LoggingHandler)} instead.
	 */
	public DatabaseHandler(String databaseURL, JDBCProperties properties) {
		this.databaseURL = databaseURL;
        this.properties = properties;
		connection = null;
	}
    
	/** Returns a connected database handler instance from the given connection data. If the password
	 *  is null, it will be queries by the user during this method. */
	public static DatabaseHandler getConnectedDatabaseHandler(String databaseURL, String username, String password, JDBCProperties properties, LoggingHandler logging) throws OperatorException, SQLException {
		if (password == null) {
			password = RapidMiner.getInputHandler().inputPassword("Password for user '" + username + "' required");
		}

		DatabaseHandler databaseHandler = new DatabaseHandler(databaseURL, properties);
		logging.log("Connecting to '" + databaseURL + "'.");
		databaseHandler.connect(username, password, true);
		return databaseHandler;
	}
	
	/** Returns the JDBC properties associated with this handler. */
	public JDBCProperties getProperties() {
		return this.properties;
	}
	
	/**
	 * Establishes a connection to the database. Afterwards, queries and updates
	 * can be executed using the methods this class provides.
	 * 
	 * @param username
	 *            Name with which to log in to the database. Might be null.
	 * @param passwd
	 *            Password with which to log in to the database. Might be null.
	 * @param autoCommit
	 *            If TRUE, all changes to the database will be committed
	 *            automatically. If FALSE, the commit()-Method has to be called
	 *            to make changes permanent.
	 */
	public void connect(String username, String passwd, boolean autoCommit) throws SQLException {
		if (connection != null) {
			throw new SQLException("connect: Connection to database '" + databaseURL + "' already exists!");
		}
		if (username == null) {
			connection = DriverManager.getConnection(databaseURL);
		} else {
			connection = DriverManager.getConnection(databaseURL, username, passwd);
		}
		connection.setAutoCommit(autoCommit);
	}

	/** Closes the connection to the database. */
	public void disconnect() throws SQLException {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	/** Returns the connection. Might be used in order to create statements. The return
	 *  value might be null if this database handler is not connected. */
	public Connection getConnection() {
		return connection;
	}
	
	/** Create a statement where result sets will have the properties 
	 *  TYPE_SCROLL_SENSITIVE and CONCUR_UPDATABLE. This means that the
	 *  ResultSet is scrollable and also updatable. It will also directly show 
	 *  all changes to the database made by others after this ResultSet was obtained.
	 *  Will throw an {@link SQLException} if the handler is not connected. */
	public Statement createStatement(boolean scrollableAndUpdatable) throws SQLException {
		if (connection == null) {
			throw new SQLException("Could not create a statement for '" + databaseURL + "': not connected.");
		}
		Statement statement = null;
		
		if (scrollableAndUpdatable)
			statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		else
			statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		return statement;
	}

	/** Create a prepared statement where result sets will have the properties 
	 *  TYPE_SCROLL_SENSITIVE and CONCUR_UPDATABLE. This means that the
	 *  ResultSet is scrollable and also updatable. It will also directly show 
	 *  all changes to the database made by others after this ResultSet was obtained.
	 *  Will throw an {@link SQLException} if the handler is not connected. */
	public PreparedStatement createPreparedStatement(String sqlString, boolean scrollableAndUpdatable) throws SQLException {
		if (connection == null) {
			throw new SQLException("Could not create a prepared statement for '" + databaseURL + "': not connected.");
		}
		PreparedStatement statement = null;
		if (scrollableAndUpdatable)
			statement = connection.prepareStatement(sqlString, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		else
			statement = connection.prepareStatement(sqlString, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		return statement;
	}
	
    /**
     * Makes all changes to the database permanent. Invoking this method explicit is
     * usually not necessary if the connection was created with AUTO_COMMIT set to true.
     */
    public void commit() throws SQLException {
        if ((connection == null) || connection.isClosed()) {
            throw new SQLException("Could not commit: no open connection to database '" + databaseURL + "' !");
        }
        connection.commit();
    }
    
	/**
	 * Executes the given SQL-Query. Only SQL-statements starting with "SELECT"
	 * (disregarding case) will be accepted. Any errors will result in an
	 * SQL-Exception being thrown.
	 * 
	 * @param sqlQuery An SQL-String.
	 * @return A ResultSet-Object with the results of the query. The ResultSet
	 *         is scrollable, but not updatable. It will not show changes to
	 *         the database made by others after this ResultSet was obtained.
	 *
	 * @deprecated Use the method {@link #createStatement(boolean)} instead and perform the queries explicitely since this method would not allow closing the statement
	 */
	@Deprecated
	public ResultSet query(String sqlQuery) throws SQLException {
		if (!sqlQuery.toLowerCase().startsWith("select")) {
			throw new SQLException("Query: Only SQL-Statements starting with SELECT are allowed: " + sqlQuery);
		}

		Statement st = createStatement(true);
		ResultSet rs = st.executeQuery(sqlQuery);
		return rs;
	}

    
	/** Adds a column for the given attribute to the table with name tableName. */
	public void addColumn(Attribute attribute, String tableName) throws SQLException {
		// drop the column if necessary 
		Statement statement = createStatement(false);
		boolean exists = false;
		try {
            // check if column already exists (no exception and more than zero rows :-)
			ResultSet existingResultSet = statement.executeQuery("SELECT " + properties.getIdentifierQuoteOpen() + attribute.getName() + properties.getIdentifierQuoteClose() + " FROM " + properties.getIdentifierQuoteOpen() + tableName + properties.getIdentifierQuoteClose() + " WHERE 0 = 1");
            if (existingResultSet.getMetaData().getColumnCount() > 0)
                exists = true;
			existingResultSet.close();
		} catch (SQLException e) {
			// exception will be thrown if the column does not exist
		}
		statement.close();
		
        if (exists) {
        	removeColumn(attribute, tableName);
        }
        
        // create new column
		Statement st = null;
		try {
			st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			String query = 
				"ALTER TABLE " + 
				properties.getIdentifierQuoteOpen() + tableName + properties.getIdentifierQuoteClose() + 
				" ADD COLUMN " + 
				properties.getIdentifierQuoteOpen() + attribute.getName() + properties.getIdentifierQuoteClose() + 
				" " + (attribute.isNominal() ? (properties.getVarcharName() + "(256)") : properties.getRealName());
			st.execute(query);
		} catch (SQLException e) {
			throw e;
		} finally {
			if (st != null)
				st.close();
		}
	}

	/**
	 * Removes the column of the given attribute from the table with name
	 * tableName.
	 */
	public void removeColumn(Attribute attribute, String tableName) throws SQLException {
        Statement st = null;
        try {
        	st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        	String query = 
        		"ALTER TABLE " + 
        		properties.getIdentifierQuoteOpen() + tableName + properties.getIdentifierQuoteClose() + 
        		" DROP COLUMN " + 
        		properties.getIdentifierQuoteOpen() + attribute.getName() + properties.getIdentifierQuoteClose();
        	st.execute(query);
        } catch (SQLException e) {
        	throw e;
        } finally {
        	if (st != null)
                st.close();
        }
	}

	/** Creates a new table in this connection and fills it with the provided data. 
	 * 
	 *  @throws SQLException if the table should be overwritten but a table with this name already exists
	 */
	public void createTable(ExampleSet exampleSet, String tableName, int overwriteMode, boolean firstAttempt, int defaultVarcharLength) throws SQLException {
		// either drop the table or throw an exception (depending on the parameter 'overwrite')
		Statement statement = createStatement(true);
		boolean exists = false;
		try {
            // check if table already exists (no exception and more than zero columns :-)
			ResultSet existingResultSet = statement.executeQuery("SELECT * FROM " + properties.getIdentifierQuoteOpen() + tableName + properties.getIdentifierQuoteClose() + " WHERE 0 = 1");
            if (existingResultSet.getMetaData().getColumnCount() > 0)
                exists = true;
			existingResultSet.close();
		} catch (SQLException e) {
			// exception will be throw if table does not exist
//TODO : Remove
			e.printStackTrace();
		}
		
		// drop table?
        if (exists) {
        	switch (overwriteMode) {
        	case OVERWRITE_MODE_NONE:
            	throw new SQLException("Table with name '"+tableName+"' already exists and overwriting mode is not activated." + Tools.getLineSeparator() + 
            	"Please change table name or activate overwriting mode.");
        	case OVERWRITE_MODE_OVERWRITE:
            	statement.executeUpdate("DROP TABLE " + properties.getIdentifierQuoteOpen() + tableName + properties.getIdentifierQuoteClose());
            	
            	// create new table
            	exampleSet.recalculateAllAttributeStatistics(); // necessary for updating the possible nominal values
            	String createTableString = getCreateTableString(exampleSet, tableName, defaultVarcharLength);
            	statement.executeUpdate(createTableString);
            	statement.close();
            	break;
        	case OVERWRITE_MODE_OVERWRITE_FIRST:
        		if (firstAttempt) {
                	statement.executeUpdate("DROP TABLE " + properties.getIdentifierQuoteOpen() + tableName + properties.getIdentifierQuoteClose());
                	
                	// create new table
                	exampleSet.recalculateAllAttributeStatistics(); // necessary for updating the possible nominal values
                	createTableString = getCreateTableString(exampleSet, tableName, defaultVarcharLength);
                	statement.executeUpdate(createTableString);
                	statement.close();        			
        		}
        		break;
        	default:
        		break;
        	}
        } else {
        	// create new table
        	exampleSet.recalculateAllAttributeStatistics(); // necessary for updating the possible nominal values
        	String createTableString = getCreateTableString(exampleSet, tableName, defaultVarcharLength);
        	statement.executeUpdate(createTableString);
        	statement.close();
        }
        
        // fill table
		PreparedStatement insertStatement = getInsertIntoTableStatement(tableName, exampleSet);
		for (Example example : exampleSet) {
			applyInsertIntoTable(insertStatement, example, exampleSet.getAttributes().allAttributeRoles());
		}
		insertStatement.close();		
	}

	private PreparedStatement getInsertIntoTableStatement(String tableName, ExampleSet exampleSet) throws SQLException {
		StringBuffer result = new StringBuffer("INSERT INTO ");
		result.append(properties.getIdentifierQuoteOpen() + tableName + properties.getIdentifierQuoteClose());
		result.append("(");
		Iterator<Attribute> a = exampleSet.getAttributes().allAttributes();
		boolean first = true;
		while (a.hasNext()) {
			Attribute attribute = a.next();
			if (!first)
				result.append(", ");
			result.append(properties.getIdentifierQuoteOpen() + attribute.getName() + properties.getIdentifierQuoteClose());
			first = false;
		}
		result.append(")");
		result.append(" VALUES (");
		int size = exampleSet.getAttributes().allSize();
		for (int i = 0; i < size; i++) {
			if (i != 0)
				result.append(", ");
			result.append("?");
		}
		result.append(")");
		return createPreparedStatement(result.toString(), true);
	}

	private void applyInsertIntoTable(PreparedStatement statement, Example example, Iterator<AttributeRole> attributes) throws SQLException {
		int counter = 1;
		while (attributes.hasNext()) {
			Attribute attribute = attributes.next().getAttribute();
			double value = example.getValue(attribute);
			if (Double.isNaN(value)) {
				int sqlType;
				if (!attribute.isNominal()) {
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.INTEGER)) {
						sqlType = Types.INTEGER;
					} else {
						sqlType = Types.REAL;
					}
				} else {
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.STRING)) {
						sqlType = Types.BLOB;
					} else {
						sqlType = Types.VARCHAR;
					}
				}
				statement.setNull(counter, sqlType);
			} else {
				if (attribute.isNominal()) {
					String valueString = attribute.getMapping().mapIndex((int)value);
					
					while (valueString.indexOf(properties.getIdentifierQuoteOpen()) >= 0)
						valueString = valueString.replace(properties.getIdentifierQuoteOpen(), "_");
					
					while (valueString.indexOf(properties.getIdentifierQuoteClose()) >= 0)
						valueString = valueString.replace(properties.getIdentifierQuoteClose(), "_");
					
					while (valueString.indexOf(properties.getValueQuoteOpen()) >= 0)
						valueString = valueString.replace(properties.getValueQuoteOpen(), "_");
					
					while (valueString.indexOf(properties.getValueQuoteClose()) >= 0)
						valueString = valueString.replace(properties.getValueQuoteClose(), "_");
					
					statement.setString(counter, valueString);
				} else {
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.TIME)) {
							statement.setTime(counter, new Time((long) value));
						} else {
							statement.setTimestamp(counter, new Timestamp((long) value));
						}
					} else { 
						statement.setDouble(counter, value);
					}
				}
			}
			counter++;
		}
		statement.executeUpdate();
	}
	
	private String getCreateTableString(ExampleSet exampleSet, String tableName, int defaultVarcharLength) {
		// define all attribute names and types
		StringBuffer result = new StringBuffer();
		result.append("CREATE TABLE " + properties.getIdentifierQuoteOpen() + tableName + properties.getIdentifierQuoteClose() + "(");
		Iterator<AttributeRole> a = exampleSet.getAttributes().allAttributeRoles();
		boolean first = true;
		while (a.hasNext()) {
			if (!first)
				result.append(", ");
			first = false;
			AttributeRole attributeRole = a.next();
			result.append(getCreateAttributeString(attributeRole, defaultVarcharLength));
		}
		
		// set primary key
		Attribute idAttribute = exampleSet.getAttributes().getId(); 
		if (idAttribute != null) {
			result.append(", PRIMARY KEY( " + properties.getIdentifierQuoteOpen() + idAttribute.getName() + properties.getIdentifierQuoteClose() + " )");
		}
		
		result.append(")");
		return result.toString();
	}
	
	/** Creates the name and type string for the given attribute. Id attributes
	 *  must not be null.
	 */
	private String getCreateAttributeString(AttributeRole attributeRole, int defaultVarcharLength) {
		Attribute attribute = attributeRole.getAttribute();
		StringBuffer result = new StringBuffer(properties.getIdentifierQuoteOpen() + attribute.getName() + properties.getIdentifierQuoteClose() + " ");
		if (attribute.isNominal()) {
			int varCharLength = 1; // at least length 1
			if (defaultVarcharLength != -1) {
				varCharLength = defaultVarcharLength;
			} else {
				for (String value : attribute.getMapping().getValues()) {
					varCharLength = Math.max(varCharLength, value.length());
				}
			}
			if (attribute.getValueType() != Ontology.STRING)
				result.append(properties.getVarcharName() + "(" + varCharLength + ")");
			else
				result.append(properties.getTextName());
		} else {
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.INTEGER)) {
				result.append(properties.getIntegerName());
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)){
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE)){
					result.append(properties.getDateName());
				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.TIME)){
					result.append(properties.getTimeName());
				} else { // Date_time
					result.append(properties.getDateTimeName());
				}
			} else {
				result.append(properties.getRealName());
			}
		}
		
		// id must not be null
		if (attributeRole.isSpecial())
			if (attributeRole.getSpecialName().equals(Attributes.ID_NAME))
				result.append(" NOT NULL");
		
		return result.toString();
	}
    
	/**
	 * Returns for the given SQL-type the name of the corresponding RapidMiner-Type
	 * from com.rapidminer.tools.Ontology.
	 */
	public static int getRapidMinerTypeIndex(int sqlType) {
		switch (sqlType) {
			case Types.BIGINT:
			case Types.INTEGER:
			case Types.TINYINT:
			case Types.SMALLINT:
				return Ontology.INTEGER;

			case Types.FLOAT:
			case Types.REAL:
			case Types.DECIMAL:
			case Types.DOUBLE:
				return Ontology.REAL;

			case Types.NUMERIC:
				return Ontology.NUMERICAL;
			case Types.BLOB:
			case Types.CLOB:
				return Ontology.STRING;
				
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.BINARY:
			case Types.BIT:
			case Types.LONGVARBINARY:
			case Types.JAVA_OBJECT:
			case Types.STRUCT:
			case Types.VARBINARY:
			case Types.LONGVARCHAR:
				return Ontology.NOMINAL;

			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
				return Ontology.NOMINAL;

			default:
				return Ontology.NOMINAL;
		}
	}

	/**
	 * Creates a list of attributes reflecting the result set's column meta
	 * data.
	 */
	public static List<Attribute> createAttributes(ResultSet rs) throws SQLException {
		List<Attribute> attributes = new LinkedList<Attribute>();

		if (rs == null) {
			throw new IllegalArgumentException("Cannot create attributes: ResultSet must not be null!");
		}

		ResultSetMetaData metadata;
		try {
			metadata = rs.getMetaData();
		} catch (NullPointerException npe) {
			throw new RuntimeException("Could not create attribute list: ResultSet object seems closed.");
		}

		int numberOfColumns = metadata.getColumnCount();

		for (int column = 1; column <= numberOfColumns; column++) {
			String name = metadata.getColumnLabel(column);
			Attribute attribute = AttributeFactory.createAttribute(name, getRapidMinerTypeIndex(metadata.getColumnType(column)));
			attributes.add(attribute);
		}

		return attributes;
	}

	/** 
	 * @deprecated Use the open and close quotes for identifiers from the properties instead 
	 */
	@Deprecated
	public static String getDatabaseName(Attribute attribute) {
		String name = attribute.getName();
		//name = name.toUpperCase();
        //name = name.replaceAll("\\\\s", "_");
		//name = name.replaceAll("\\(", "_");
		//name = name.replaceAll("\\)", "_");
        name = name.replaceAll("\\W", "_"); // replace non-word characters
		return name;
	}

    public Map<String, List<ColumnIdentifier>> getAllTableMetaData() throws SQLException {
        if ((connection == null) || connection.isClosed()) {
            throw new SQLException("Could not retrieve all table names: no open connection to database '" + databaseURL + "' !");
        }

        DatabaseMetaData metaData = connection.getMetaData();
        String[] types = new String[] { "TABLE" };
        ResultSet tableNames = metaData.getTables(null, null, "%", types);
        List<String> tableNameList = new LinkedList<String>();
        while (tableNames.next()) {
            String tableName    = tableNames.getString(3);
            tableNameList.add(tableName);
        }
        
        Map<String, List<ColumnIdentifier>> result = new LinkedHashMap<String, List<ColumnIdentifier>>();
        Iterator<String> i = tableNameList.iterator();
        while (i.hasNext()) {
            String tableName = i.next();
            try {
                // test: will fail if user can not use this table
                List<ColumnIdentifier> columnNames = getAllColumnNames(tableName);
                result.put(tableName, columnNames);
            } catch (SQLException e) {
            	// does nothing
            } 
        }
        return result;
    }
    
    private List<ColumnIdentifier> getAllColumnNames(String tableName) throws SQLException {
        if (tableName == null) {
            throw new SQLException("Cannot read column names: table name must not be null!");
        }

        Statement statement = null;
        try {
        	statement = createStatement(false);
        	ResultSet rs = statement.executeQuery("SELECT * FROM " + properties.getIdentifierQuoteOpen() + tableName + properties.getIdentifierQuoteClose() + " WHERE 0 = 1");
        	List<ColumnIdentifier> result = new LinkedList<ColumnIdentifier>();

        	ResultSetMetaData metadata;
        	try {
        		metadata = rs.getMetaData();
        	} catch (NullPointerException npe) {
        		throw new SQLException("Could not create column name list: ResultSet object seems closed.");
        	}

        	int numberOfColumns = metadata.getColumnCount();

        	for (int column = 1; column <= numberOfColumns; column++) {
        		String name = metadata.getColumnLabel(column);
        		result.add(new ColumnIdentifier(tableName, name));
        	}
        	
            return result;
        } catch (SQLException e) {
        	throw e;
        } finally {
        	if (statement != null)
                statement.close();
        }
    }
    
	public static DatabaseHandler getConnectedDatabaseHandler(Operator operator) throws OperatorException, SQLException {
		switch (operator.getParameterAsInt(PARAMETER_DEFINE_CONNECTION)) {
		case CONNECTION_MODE_PREDEFINED:
			ConnectionEntry entry = DatabaseConnectionService.getConnectionEntry(operator.getParameterAsString(PARAMETER_CONNECTION));
			if (entry == null) {
				throw new UserError(operator, 318, operator.getParameterAsString(PARAMETER_CONNECTION));
			}
			return getConnectedDatabaseHandler(entry.getURL(), entry.getUser(), new String(entry.getPassword()), entry.getProperties(), operator); 
		case DatabaseHandler.CONNECTION_MODE_URL:
		default:
			return getConnectedDatabaseHandler(operator.getParameterAsString(PARAMETER_DATABASE_URL),
											   operator.getParameterAsString(PARAMETER_USERNAME),
											   operator.getParameterAsString(PARAMETER_PASSWORD),
											   DatabaseService.getJDBCProperties().get(operator.getParameterAsInt(PARAMETER_DATABASE_SYSTEM)),
											   operator);
		}
	}
	
	public static ConnectionEntry getConnectionEntry(Operator operator) {
		try {
			switch (operator.getParameterAsInt(DatabaseHandler.PARAMETER_DEFINE_CONNECTION)) {
			case DatabaseHandler.CONNECTION_MODE_PREDEFINED:
				return DatabaseConnectionService.getConnectionEntry(operator.getParameterAsString(DatabaseHandler.PARAMETER_CONNECTION));
			case DatabaseHandler.CONNECTION_MODE_URL:
				final String connectionUrl = operator.getParameterAsString(DatabaseHandler.PARAMETER_DATABASE_URL);
				final String connectionUsername = operator.getParameterAsString(DatabaseHandler.PARAMETER_USERNAME);
				final char[] connectionPassword = operator.getParameterAsString(DatabaseHandler.PARAMETER_PASSWORD).toCharArray();
				return new ConnectionEntry("urlConnection", DatabaseService.getJDBCProperties().get(operator.getParameterAsInt(DatabaseHandler.PARAMETER_DATABASE_SYSTEM))) {
					@Override
					public String getURL() {
						return connectionUrl;
					}
					
					@Override
					public String getUser() {
						return connectionUsername;
					}
					
					@Override
					public char[] getPassword() {
						return connectionPassword;
					}
				};
			}
		} catch (UndefinedParameterError e) {
		}
		return null;
	}
    
    public static List<ParameterType> getConnectionParameterTypes(ParameterHandler handler) {
    	List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeCategory(PARAMETER_DEFINE_CONNECTION, "Indicates how the database connection should be specified.", CONNECTION_MODES, CONNECTION_MODE_PREDEFINED);
		type.setExpert(false);
		types.add(type);
		
		type = new ParameterTypeDatabaseConnection(PARAMETER_CONNECTION, "A predefined database connection.");
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_PREDEFINED));
		type.setExpert(false);
		types.add(type);
		
		type = new ParameterTypeCategory(PARAMETER_DATABASE_SYSTEM, "The used database system.", DatabaseService.getDBSystemNames(), 0);
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_URL));
        type.setExpert(false);
        types.add(type);
        
        type = new ParameterTypeString(PARAMETER_DATABASE_URL, "The URL connection string for the database, e.g. 'jdbc:mysql://foo.bar:portnr/database'");
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_URL));
        type.setExpert(false);
        types.add(type);
		type = new ParameterTypeString(PARAMETER_USERNAME, "The database username.");
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_URL));
        type.setExpert(false);
        types.add(type);
		type = new ParameterTypePassword(PARAMETER_PASSWORD, "The password for the database.");
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_URL));
        type.setExpert(false);
        types.add(type);
        return types;
    }
    
    public static List<ParameterType> getQueryParameterTypes(ParameterHandler handler, boolean tableOnly) {
    	List<ParameterType> types = new LinkedList<ParameterType>();
    	ParameterType type = null;
        if (!tableOnly) {
	        type = new ParameterTypeCategory(PARAMETER_DEFINE_QUERY, "Specifies whether the database query should be defined directly, through a file or implicitely by a given table name.", QUERY_MODES, QUERY_QUERY);
	        type.setExpert(false);
	        types.add(type);
			
			type = new ParameterTypeSQLQuery(PARAMETER_QUERY, "An SQL query.");
			type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_QUERY, QUERY_MODES, true, QUERY_QUERY));
	        type.setExpert(false);
			types.add(type);
			
			type = new ParameterTypeFile(PARAMETER_QUERY_FILE, "A file containing an SQL query.", null, true);
			type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_QUERY, QUERY_MODES, true, QUERY_FILE));
	        type.setExpert(false);
			types.add(type);
        }

		type = new ParameterTypeDatabaseTable(PARAMETER_TABLE_NAME, "A database table.");
		if (!tableOnly) {
			type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_QUERY, QUERY_MODES, true, QUERY_TABLE));
		}
        type.setExpert(false);
		types.add(type);
    	return types;
    }
}
