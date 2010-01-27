package com.rapidminer.repository.gui.process;

/** Required to wrap around string for instanceof check in tree model.
 * 
 * @author Simon Fischer
 *
 */
public class OutputLocation {
	private String location;

	public OutputLocation(String location) {
		super();
		this.location = location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getLocation() {
		return location;
	}
	
	@Override
	public String toString() {
		return location;
	}
}
