/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
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
 * A parameter type for categories. These are several Strings and one of these
 * is the default value. Operators ask for the index of the selected value with
 * {@link com.rapidminer.operator.Operator#getParameterAsInt(String)}.
 * 
 * @author Ingo Mierswa, Simon Fischer
 *          ingomierswa Exp $
 */
public class ParameterTypeCategory extends ParameterTypeSingle {

	private static final long serialVersionUID = 5747692587025691591L;

	private int defaultValue = 0;

	private String[] categories = new String[0];

	public ParameterTypeCategory(String key, String description, String[] categories, int defaultValue, boolean expert) {
		this(key, description, categories, defaultValue);
		setExpert(expert);
	}

	public ParameterTypeCategory(String key, String description, String[] categories, int defaultValue) {
		super(key, description);
		this.categories = categories;
		this.defaultValue = defaultValue;
	}

	@Override
	public boolean isOptional() {
		return super.isOptional() || (defaultValue != -1);
	}

	public int getDefault() {
		return defaultValue;
	}

	@Override
	public Object getDefaultValue() {
		if (defaultValue == -1) {
			return null;
		} else {
			return Integer.valueOf(defaultValue);
		}
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = (Integer)defaultValue;
	}

	/** Returns false. */
	@Override
	public boolean isNumerical() { return false; }

	public String getCategory(int index) {
		return categories[index];
	}

	public int getIndex(String string) {
		for (int i = 0; i < categories.length; i++) {
			if (categories[i].equals(string)) {
				return Integer.valueOf(i);
			}
		}
		// try to interpret string as number
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	@Override
	public String toString(Object value) {
		try {
			int index = Integer.parseInt(value.toString());
			if (index >= categories.length)
				return "";
			return super.toString(categories[index]);
		} catch (NumberFormatException e) {
			return super.toString(value);
		}
	}

	public String[] getValues() {
		return categories;
	}

	@Override
	public String getRange() {
		StringBuffer values = new StringBuffer();
		for (int i = 0; i < categories.length; i++) {
			if (i > 0)
				values.append(", ");
			values.append(categories[i]);
		}
		return values.toString() + "; default: " + categories[defaultValue];
	}

	public int getNumberOfCategories() {
		return categories.length;
	}
}
