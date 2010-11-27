package com.rapidminer.repository.db;

import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.jdbc.ColumnIdentifier;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;

/** Converts a reference to a table in a database to an {@link IOObject}, e.g. an example set.
 * 
 * @author Simon Fischer
 *
 */
public interface DBConnectionToIOObjectConverter {

	/** Retrieves the actual data and returns it. */
	public IOObject convert(ConnectionEntry connection, String tableName) throws OperatorException;
	
	/** Returns meta data describing the entry. */
	public MetaData convertMetaData(ConnectionEntry connection, String tableName, List<ColumnIdentifier> columns);

	/** Returns a suffix to be appended to entries in the repository tree to identify the converter. */
	public String getSuffix();
	
}
