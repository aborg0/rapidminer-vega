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
package com.rapidminer.operator.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import java.util.List;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.ConnectionProvider;


/**
 * <p>This operator reads an {@link com.rapidminer.example.ExampleSet} from an SQL
 * database. The SQL query can be passed to RapidMiner via a parameter or, in case of
 * long SQL statements, in a separate file. Please note that column names are
 * often case sensitive. Databases may behave differently here.</p>
 * 
 * <p>The most convenient way of defining the necessary parameters is the 
 * configuration wizard. The most important parameters (database URL and user name) will
 * be automatically determined by this wizard and it is also possible to define
 * the special attributes like labels or ids.</p>
 * 
 * <p>Please note that this operator supports two basic working modes:</p>
 * <ol>
 * <li>reading the data from the database and creating an example table in main memory</li>
 * <li>keeping the data in the database and directly working on the database table </li>
 * </ol>
 * <p>The latter possibility will be turned on by the parameter &quot;work_on_database&quot;.
 * Please note that this working mode is still regarded as experimental and errors might
 * occur. In order to ensure proper data changes the database working mode is only allowed
 * on a single table which must be defined with the parameter &quot;table_name&quot;. 
 * IMPORTANT: If you encounter problems during data updates (e.g. messages that the result set is not 
 * updatable) you probably have to define a primary key for your table.</p>
 * 
 * <p>If you are not directly working on the database, the data will be read with an arbitrary
 * SQL query statement (SELECT ... FROM ... WHERE ...) defined by &quot;query&quot; or &quot;query_file&quot;.
 * The memory mode is the recommended way of using this operator. This is especially important for
 * following operators like learning schemes which would often load (most of) the data into main memory
 * during the learning process. In these cases a direct working on the database is not recommended
 * anyway.</p>
 * 
 * <h5>Warning</h5>
 * As the java <code>ResultSetMetaData</code> interface does not provide
 * information about the possible values of nominal attributes, the internal
 * indices the nominal values are mapped to will depend on the ordering
 * they appear in the table. This may cause problems only when processes are
 * split up into a training process and an application or testing process.
 * For learning schemes which are capable of handling nominal attributes, this
 * is not a problem. If a learning scheme like a SVM is used with nominal data,
 * RapidMiner pretends that nominal attributes are numerical and uses indices for the
 * nominal values as their numerical value. A SVM may perform well if there are
 * only two possible values. If a test set is read in another process, the
 * nominal values may be assigned different indices, and hence the SVM trained
 * is useless. This is not a problem for label attributes, since the classes can
 * be specified using the <code>classes</code> parameter and hence, all
 * learning schemes intended to use with nominal data are safe to use.
 * 
 * @rapidminer.todo Fix the above problem. This may not be possible effeciently since
 *            it is not supported by the Java ResultSet interface.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class DatabaseDataReader extends AbstractDataReader implements ConnectionProvider {
	
	/** The database connection handler. */
	private DatabaseHandler databaseHandler;

	/** This is only used for the case that the data is read into memory. */
	private Statement statement;
	
	
	public DatabaseDataReader(OperatorDescription description) {
		super(description);
	}

	public void tearDown() {
		if (this.statement != null) {
			try {
				this.statement.close();
			} catch (SQLException e) {
				logWarning("Cannot close statement.");
			}
			this.statement = null;
		}
	}
	
	private String getQuery() throws OperatorException {
		switch (getParameterAsInt(DatabaseHandler.PARAMETER_DEFINE_QUERY)) {
		case DatabaseHandler.QUERY_QUERY:
		{
			String query = getParameterAsString(DatabaseHandler.PARAMETER_QUERY);
			if (query != null) {
				query = query.trim();
			}
			return query;
		}
		case DatabaseHandler.QUERY_FILE:
		{
			File queryFile = getParameterAsFile(DatabaseHandler.PARAMETER_QUERY_FILE);
			if (queryFile != null) {
				String query = null;
				try {
					query = Tools.readTextFile(queryFile);
				} catch (IOException ioe) {
					throw new UserError(this, ioe, 302, new Object[] { queryFile, ioe.getMessage() });
				}
				if ((query == null) || (query.trim().length() == 0)) {
					throw new UserError(this, 205, queryFile);
				}
				return query;
			}
		}
		case DatabaseHandler.QUERY_TABLE:
			return "SELECT * FROM " + getParameterAsString(DatabaseHandler.PARAMETER_TABLE_NAME);
		}
		return null;
	}

	/**
	 * This method reads the file whose name is given, extracts the database
	 * access information and the query from it and executes the query. The
	 * query result is returned as a ResultSet.
	 */
	public ResultSet getResultSet() throws OperatorException {
		ResultSet rs = null;
		try {
			databaseHandler = DatabaseHandler.getConnectedDatabaseHandler(this);
			String query = getQuery();
			if (query == null) {
				throw new UserError(this, 202, new Object[] { "query", "query_file", "table_name" });
			}
			log("Executing query: '" + query + "'");
			this.statement = databaseHandler.createStatement(false);
			rs = this.statement.executeQuery(query);
			log("Query executed.");
		} catch (SQLException sqle) {
			throw new UserError(this, sqle, 304, sqle.getMessage());
		}
		return rs;
	}

	@Override
	public void processFinished() {
	    disconnect();
	}

    private void disconnect() {
    	// close statement
    	tearDown();
    	
    	// close database connection
        if (databaseHandler != null) {
            try {
                databaseHandler.disconnect();
                databaseHandler = null;
            } catch (SQLException e) {
                logWarning("Cannot disconnect from database: " + e);
            }
        }        
    }
    
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();
		list.addAll(DatabaseHandler.getConnectionParameterTypes(this));
		list.addAll(DatabaseHandler.getQueryParameterTypes(this, false));
		return list;
	}
	
	// TODO unify with DatabaseHandler.getValueType(..) - check if date types have been implemented
	protected static int getValueType(int sqlType) {
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
				return Ontology.DATE_TIME;

			default:
				return Ontology.NOMINAL;
		}
	}
	
	@Override
	protected DataSet getDataSet() throws OperatorException {
		return new DataSet() {
			private ResultSet resultSet = getResultSet();
			
			private ResultSetMetaData metaData = null; 
			{
				try {
					metaData = resultSet.getMetaData(); 
					int numberOfColumns = metaData.getColumnCount();
					String[] columnNames = new String[numberOfColumns];
					for (int i = 0; i < numberOfColumns; i++) {
						columnNames[i] = metaData.getColumnLabel(i + 1);
					}
					setColumnNames(columnNames);
				} catch (SQLException e) {
					throw new OperatorException("Could not read result set meta data.");
				}
			}
			
			private Object[] values = new Object[getColumnCount()];

			@Override
			// TODO throw operator exception in case of SQL exception
			public boolean next() {
				try {
					if (resultSet.next()) {
						for (int i = 0; i < getColumnCount(); i++) {
							if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseDataReader.getValueType(metaData.getColumnType(i + 1)), Ontology.NUMERICAL)) {
								values[i] = Double.valueOf(resultSet.getDouble(i + 1));
							} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseDataReader.getValueType(metaData.getColumnType(i + 1)), Ontology.DATE_TIME)) {
								values[i] = resultSet.getTimestamp(i + 1);
							} else if (metaData.getColumnType(i + 1) == Types.CLOB) {
								Clob clob = resultSet.getClob(i + 1);
								Reader reader = clob.getCharacterStream();
								BufferedReader in = new BufferedReader(reader);
								String line = null;
								try {
									StringBuffer buffer = new StringBuffer();
									while ((line = in.readLine()) != null) {
										buffer.append(line + "\n");
									}
									values[i] = buffer.toString();
								} catch (IOException e) {
									values[i] = null;
								}
							} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseDataReader.getValueType(metaData.getColumnType(i + 1)), Ontology.NOMINAL)) {
								values[i] = resultSet.getString(i + 1);
							}
							if (resultSet.wasNull()) {
								values[i] = null;
							}
						}
						return true;
					}
					return false;					
				} catch (SQLException e) {
					return false;
				}
			}

			@Override
			public int getNumberOfColumnsInCurrentRow() {
				// we can rely on columnCount here since it was already set
				return getColumnCount();
			}

			@Override
			public boolean isMissing(int columnIndex) {
				return values[columnIndex] == null;
			}

			@Override
			public Number getNumber(int columnIndex) {
				try {
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseDataReader.getValueType(metaData.getColumnType(columnIndex + 1)), Ontology.NUMERICAL)) {
						return (Double) values[columnIndex];
					}
				} catch (SQLException e) {
				}
				return null;
			}
			
			@Override
			public Date getDate(int columnIndex) {
				try {
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseDataReader.getValueType(metaData.getColumnType(columnIndex + 1)), Ontology.DATE_TIME)) {
						return (Date) values[columnIndex];
					}
				} catch (SQLException e) {
				}
				return null;
			}

			@Override
			public String getString(int columnIndex) {
				try {
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseDataReader.getValueType(metaData.getColumnType(columnIndex + 1)), Ontology.NOMINAL)) {
						return (String) values[columnIndex];
					}
				} catch (SQLException e) {
				}
				return null;					
			}
			
			@Override
			public void close() throws OperatorException {
				tearDown();
			}
		};
	}

	@Override
	public ConnectionEntry getConnectionEntry() {
		return DatabaseHandler.getConnectionEntry(this);
	}
}
