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
 * A parameter type for String values. Operators ask for the value with
 * {@link com.rapidminer.operator.Operator#getParameterAsString(String)}.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class ParameterTypeString extends ParameterTypeSingle {

	private static final long serialVersionUID = 6451584265725535856L;

	private String defaultValue = null;

	private boolean optional = false;

	public ParameterTypeString(String key, String description, boolean optional, boolean expert) {
		this(key, description, optional);
		setExpert(expert);
	}

	public ParameterTypeString(String key, String description, boolean optional) {
		super(key, description);
		this.defaultValue = null;
		this.optional = optional;
		if (!optional)
			setExpert(false);
	}

	public ParameterTypeString(String key, String description) {
		this(key, description, true);
	}

	public ParameterTypeString(String key, String description, String defaultValue, boolean expert) {
		this(key, description, defaultValue);
		setExpert(expert);
	}
	public ParameterTypeString(String key, String description, String defaultValue) {
		this(key, description);
		this.defaultValue = defaultValue;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
		if (!optional)
			setExpert(false);
	}

	@Override
	public boolean isOptional() {
		if (super.isOptional())
			return optional;
		return false;
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = (String)defaultValue;
	}

	/** Returns false. */
	@Override
	public boolean isNumerical() { return false; }

	@Override
	public String getRange() {
		return "string" + ((defaultValue != null) ? ("; default: '" + defaultValue + "'") : "");
	}
}
