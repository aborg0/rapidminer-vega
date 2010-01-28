/**
 * 
 */
package com.rapidminer.repository;

/**
 * 
 * @author Simon Fischer
 *
 */
public enum AccessFlag {
	REJECT('-'), IGNORE('%'), GRANT('+');		
	private AccessFlag(char symbol) {
		this.symbol = symbol;
	}
	private char symbol;
	public char getSymbol() {
		return symbol;
	}
}