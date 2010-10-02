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
		UNPARSEABLE_REAL,
		MORE_THAN_TWO_VALUES
	}
	
	/** The row number in which this error occurred. */
	private final int row;

	/** The example to which this {@link #row} is mapped. E.g., if rows
	 *  are used as annotations, example index and row do not match. */
	private int exampleIndex;

	/** The column (cell index) in which this error occurred. */
	private final int column;
	
	/** The original value that was unparseable. Most of the time, this will be a string. */
	private final Object originalValue;
	
	private final ErrorCode errorCode;

	private final Throwable cause;
	
	public ParsingError(int row, int column, ErrorCode errorCode, Object originalValue) {
		this(row, column, errorCode, originalValue, null);
	}
	
	public ParsingError(int row, int column, ErrorCode errorCode, Object originalValue, Throwable cause) {
		super();
		this.row = row;
		this.column = column;
		this.originalValue = originalValue;
		this.errorCode = errorCode;
		this.setExampleIndex(row);
		this.cause = cause;
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

	public void setExampleIndex(int exampleIndex) {
		this.exampleIndex = exampleIndex;
	}

	public int getExampleIndex() {
		return exampleIndex;
	}

	public Throwable getCause() {
		return cause;
	}
}
