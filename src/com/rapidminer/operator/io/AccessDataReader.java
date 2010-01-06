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

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDatabaseTable;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypeSQLQuery;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.connection.AccessConnectionEntry;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.ConnectionProvider;


/**
 * This operator can be used to simplify the reading of MS Access databases. Instead of
 * this operator, the operator DatabaseExampleSource can also be used but would have to 
 * be properly initialized. This work is performed by this operator so you simply have 
 * to specify the basic database file together with the desired table.
 *  
 * @author Sebastian Land, Tobias Malbrecht
 */
public class AccessDataReader extends AbstractDataReader implements ConnectionProvider {

	public static final String PARAMETER_DATABASE_FILE = "database_file";
	
	public static final String PARAMETER_USERNAME = "username";
	
	public  static final String PARAMETER_PASSWORD = "password";
		
	private static final String DATABASE_URL_PREFIX = "jdbc:odbc:DRIVER={Microsoft Access Driver (*.mdb)};DBQ=";
	
	private static final String DEFAULT_USER_NAME = "noUser";
	
	private static final String DEFAULT_PASSWORD = "noPassword";
	

	public AccessDataReader(OperatorDescription description) throws OperatorCreationException {
		super(description);
	}
	
	@Override
	protected DataSet getDataSet() throws OperatorException {
		DatabaseDataReader reader = null;
		try {
			reader = OperatorService.createOperator(DatabaseDataReader.class);
		} catch (OperatorCreationException e) {
			throw new OperatorException("Could not create Read Database operator:", e.getCause());
		}
		reader.setParameter(DatabaseHandler.PARAMETER_DEFINE_CONNECTION, DatabaseHandler.CONNECTION_MODES[DatabaseHandler.CONNECTION_MODE_URL]);
		reader.setParameter(DatabaseHandler.PARAMETER_DATABASE_SYSTEM, "ODBC Bridge (e.g. Access)");

		File databaseFile = getParameterAsFile(PARAMETER_DATABASE_FILE);
		reader.setParameter(DatabaseHandler.PARAMETER_DATABASE_URL, DATABASE_URL_PREFIX + databaseFile.getAbsolutePath());
		reader.setParameter(DatabaseHandler.PARAMETER_DEFINE_QUERY, getParameterAsString(DatabaseHandler.PARAMETER_DEFINE_QUERY));
		reader.setParameter(DatabaseHandler.PARAMETER_QUERY, getParameterAsString(DatabaseHandler.PARAMETER_QUERY));
		reader.setParameter(DatabaseHandler.PARAMETER_QUERY_FILE, getParameterAsString(DatabaseHandler.PARAMETER_QUERY_FILE));
		reader.setParameter(DatabaseHandler.PARAMETER_TABLE_NAME, getParameterAsString(DatabaseHandler.PARAMETER_TABLE_NAME));

		String userName = getParameterAsString(PARAMETER_USERNAME);
		if (userName == null) {
			userName = DEFAULT_USER_NAME;
		}
		String password = getParameterAsString(PARAMETER_PASSWORD);
		if (password == null) {
			password = DEFAULT_PASSWORD;
		}
		reader.setParameter(DatabaseHandler.PARAMETER_USERNAME, userName);
		reader.setParameter(DatabaseHandler.PARAMETER_PASSWORD, password);
		
		DataSet dataSet = reader.getDataSet();
		// column names are available from reader as DataSet object was already generated
		setColumnNames(reader.getColumnNames());
		return dataSet;
	}
	

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeFile(PARAMETER_DATABASE_FILE, "The mdb file containing the Access database which should be read from.", "mdb", false, false));
		types.add(new ParameterTypeString(PARAMETER_USERNAME, "The username for the Access database.", true, false));
		ParameterType type = new ParameterTypePassword(PARAMETER_PASSWORD, "The password for the Access database.");
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(DatabaseHandler.PARAMETER_DEFINE_QUERY, "Specifies whether the database query should be defined directly, through a file or implicitely by a given table name.", DatabaseHandler.QUERY_MODES, DatabaseHandler.QUERY_TABLE);
	    type.setExpert(false);
	    types.add(type);
		
		type = new ParameterTypeSQLQuery(DatabaseHandler.PARAMETER_QUERY, "An SQL query.");
		type.registerDependencyCondition(new EqualTypeCondition(this, DatabaseHandler.PARAMETER_DEFINE_QUERY, DatabaseHandler.QUERY_MODES, true, DatabaseHandler.QUERY_QUERY));
	    type.setExpert(false);
		types.add(type);
		
		type = new ParameterTypeFile(DatabaseHandler.PARAMETER_QUERY_FILE, "A file containing an SQL query.", null, true);
		type.registerDependencyCondition(new EqualTypeCondition(this, DatabaseHandler.PARAMETER_DEFINE_QUERY, DatabaseHandler.QUERY_MODES, true, DatabaseHandler.QUERY_FILE));
	    type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDatabaseTable(DatabaseHandler.PARAMETER_TABLE_NAME, "The name of a single table within the Access database which should be read.");
		type.registerDependencyCondition(new EqualTypeCondition(this, DatabaseHandler.PARAMETER_DEFINE_QUERY, DatabaseHandler.QUERY_MODES, true, DatabaseHandler.QUERY_TABLE));
	    type.setExpert(false);
		types.add(type);
		return types;
	}

	@Override
	public ConnectionEntry getConnectionEntry() {
		try {
			File file = getParameterAsFile(PARAMETER_DATABASE_FILE);
			if (file != null) {
				return new AccessConnectionEntry(file);
			}
		} catch (UndefinedParameterError e) {
		}
		return null;
	}
}
