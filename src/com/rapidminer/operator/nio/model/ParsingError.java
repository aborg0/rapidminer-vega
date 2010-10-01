package com.rapidminer.operator.nio.model;

/** An error that occurred during parsing.
 * 
 * @author Simon Fischer
 *
 */
public class ParsingError {

	public static enum ErrorCode {
		UNPARSEABLE_DATE,
		UNPARSEABLE_INTEGER,
		UNPARSEABLE_REAL
	}
	
	/** The row number in which this error occurred. */
	private final int row;
	
	/** The column (cell index) in which this error occurred. */
	private final int column;
	
	/** The original value that was unparseable. Most of the time, this will be a string. */
	private final Object originalValue;
	
	private final ErrorCode errorCode;

	public ParsingError(int row, int column, ErrorCode errorCode, Object originalValue) {
		super();
		this.row = row;
		this.column = column;
		this.originalValue = originalValue;
		this.errorCode = errorCode;
	}

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}

	public Object getOriginalValue() {
		return originalValue;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}	
}
