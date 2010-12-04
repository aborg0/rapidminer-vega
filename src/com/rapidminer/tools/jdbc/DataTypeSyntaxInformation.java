package com.rapidminer.tools.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Type information as reported by database meta data {@link DatabaseMetaData#getTypeInfo()}. */
public class DataTypeSyntaxInformation {
	private final String literalPrefix;
	private final String literalSuffix;
	private final int dataType;
	private final String typeName;
	private String createParams;
	private long precision;

	public DataTypeSyntaxInformation(ResultSet typesResult) throws SQLException {
		typeName = typesResult.getString("TYPE_NAME");
		dataType = typesResult.getInt("DATA_TYPE");
		literalPrefix = typesResult.getString("LITERAL_PREFIX");
		literalSuffix = typesResult.getString("LITERAL_SUFFIX");
		precision = typesResult.getLong("PRECISION");
		createParams = typesResult.getString("CREATE_PARAMS");
	}

	public String getTypeName() {
		return typeName;
	}

	public int getDataType() {
		return dataType;
	}

	@Override
	public String toString() {
		return getTypeName() + " (prec=" + precision + "; params=" + createParams + ")";
	}

	public long getPrecision() {
		return precision;
	}

	public String getLiteralPrefix() {
		return literalPrefix;
	}

	public String getLiteralSuffix() {
		return literalSuffix;
	}
}