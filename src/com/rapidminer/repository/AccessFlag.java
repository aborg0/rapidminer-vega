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
	REJECT('-', "sign_forbidden.png"), 
	IGNORE('%', "shape_circle.png"), 
	GRANT('+', "ok.png");
	
	private AccessFlag(char symbol, String icon) {
		this.symbol = symbol;
		this.icon = icon;
	}
	
	private char symbol;
	private String icon;
	
	public char getSymbol() {
		return symbol;
	}
	public String getIcon() {
		return icon;
	}
	public AccessFlag rotate() {
		return AccessFlag.values()[(this.ordinal()+1)%AccessFlag.values().length];
	}
}