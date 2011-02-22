/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.parameter;

/**
 * A parameter type for files. Operators ask for the selected file with
 * {@link com.rapidminer.operator.Operator#getParameterAsFile(String)}. The 
 * extension should be defined without the point (separator).
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class ParameterTypeFile extends ParameterTypeString {

	private static final long serialVersionUID = -1350352634043084406L;

	private String extension = null;

	/** Creates a new parameter type for files with the given extension. If the extension is null
	 *  no file filters will be used. If the parameter is not optional, it is set to be not expert.*/
	public ParameterTypeFile(String key, String description, String extension, boolean optional) {
		super(key, description, null);
		setOptional(optional);
		if (!optional)
			setExpert(false);
		this.extension = extension;
	}

	/** Creates a new parameter type for file with the given extension. If the extension is null
	 *  no file filters will be used. The parameter will be optional. */
	public ParameterTypeFile(String key, String description, String extension, String defaultFileName) {
		super(key, description, defaultFileName);
		setOptional(true);
		this.extension = extension;
	}

	public ParameterTypeFile(String key, String description, String extension, boolean optional, boolean expert) {
		this(key, description, extension, optional);
		setExpert(expert);
	}

	public String getExtension() {
		return extension;	
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	@Override
	public String getRange() {
		return "filename";
	}

}
