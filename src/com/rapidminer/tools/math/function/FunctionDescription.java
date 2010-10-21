package com.rapidminer.tools.math.function;

public class FunctionDescription {

	public static final int UNLIMITED_NUMBER_OF_ARGUMENTS = -1;
	
	private String function;
	
	private String functionName;
	
	private String functionDescription;
	
	private int numberOfArguments;
	
	public FunctionDescription(String function, String name, String description, int numberOfArguments) {
		this.function = function;
		this.functionName = name;
		this.functionDescription = description;
		this.numberOfArguments = numberOfArguments;
	}
	
	public String getFunction() {
		return this.function;
	}
	
	public String getName() {
		return this.functionName;
	}
	
	public String getDescription() {
		return this.functionDescription;
	}
	
	public int getNumberOfArguments() {
		return this.numberOfArguments;
	}
}
