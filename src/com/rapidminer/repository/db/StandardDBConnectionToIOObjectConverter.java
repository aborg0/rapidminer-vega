package com.rapidminer.repository.db;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.DatabaseDataReader;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.jdbc.ColumnIdentifier;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;

/** Simply converts the table to an {@link ExampleSet}.
 * 
 * @author Simon Fischer
 *
 */
public class StandardDBConnectionToIOObjectConverter implements DBConnectionToIOObjectConverter {

	@Override
	public IOObject convert(ConnectionEntry connection, String tableName) throws OperatorException {
		DatabaseDataReader reader;
		try {
			reader = OperatorService.createOperator(DatabaseDataReader.class);
		} catch (OperatorCreationException e) {
			throw new OperatorException("Failed to create database reader: "+e, e);
		}
		reader.setParameter(DatabaseHandler.PARAMETER_CONNECTION, connection.getName());
		reader.setParameter(DatabaseHandler.PARAMETER_DEFINE_CONNECTION, DatabaseHandler.CONNECTION_MODES[DatabaseHandler.CONNECTION_MODE_PREDEFINED]);
		reader.setParameter(DatabaseHandler.PARAMETER_TABLE_NAME, tableName);
		reader.setParameter(DatabaseHandler.PARAMETER_DEFINE_QUERY, DatabaseHandler.QUERY_MODES[DatabaseHandler.QUERY_TABLE]);

		return reader.read();
	}

	@Override
	public MetaData convertMetaData(ConnectionEntry connection, String tableName, List<ColumnIdentifier> columns) {
		ExampleSetMetaData metaData = new ExampleSetMetaData();
		for (ColumnIdentifier column : columns) {
			metaData.addAttribute(new AttributeMetaData(column.getColumnName(), 
					DatabaseHandler.getRapidMinerTypeIndex(column.getSqlType())));
		}
		return metaData;
	}
	
	@Override
	public String getSuffix() {
		return "Example Sets";
	}

}
