/**
 * 
 */
package com.rapidminer.repository.remote;

/** Enum of (made up) content types for RapidMiner objects and processes.
 * 
 * @author Simon Fischer
 *
 */
public enum RMContentType {		
	IOOBJECT("application/vnd.rapidminer.ioo"),
	METADATA("application/vnd.rapidminer.md"),
	PROCESS("application/vnd.rapidminer.rmp+xml");
	
	private String typeString;
	RMContentType(String typeString) {
		this.typeString = typeString;
	}
	public String getContentTypeString() {
		return typeString;
	}
}