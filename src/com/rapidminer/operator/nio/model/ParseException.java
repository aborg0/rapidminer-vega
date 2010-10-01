package com.rapidminer.operator.nio.model;

public class ParseException extends Exception {

	private static final long serialVersionUID = 1L;

	private final ParsingError error;

	public ParseException(ParsingError error) {
		super(error.toString());
		this.error = error;
	}

	public ParsingError getError() {
		return error;
	}	
}
