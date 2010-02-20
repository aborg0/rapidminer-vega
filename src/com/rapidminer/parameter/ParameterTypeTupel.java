/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;
/**
 * @author Sebastian Land
 */
public class ParameterTypeTupel extends CombinedParameterType {

	// only one character allowed
	private static final String ESCAPE_CHAR = "\\";
	private static final String ESCAPE_CHAR_REGEX = "\\\\";
	// only one character allowed
	private static final String SEPERATOR_CHAR_REGEX = "\\.";
	private static final String SEPERATOR_CHAR = ".";


	private Object[] defaultValues = null;

	private ParameterType[] types;

	public ParameterTypeTupel(String key, String description, ParameterType...parameterTypes) {
		super(key, description, parameterTypes);
		this.types = parameterTypes;
	}

	private static final long serialVersionUID = 7292052301201204321L;

	@Override
	public Object getDefaultValue() {
		if (defaultValues == null) { 
			String[] defaultValues = new String[types.length];
			for (int i = 0; i < types.length; i++) {
				defaultValues[i] = (types[i].getDefaultValue() == null) ? "" : types[i].getDefaultValue() + "";
			}
			return ParameterTypeTupel.transformTupel2String(defaultValues);
		} else {
			String[] defStrings = new String[defaultValues.length];
			for(int i = 0; i < defaultValues.length; i++) {
				defStrings[i] = defaultValues[i] + "";
			}
			return ParameterTypeTupel.transformTupel2String(defStrings);
		}
	}

	@Override
	public String getDefaultValueAsString() {
		String[] defaultStrings = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			Object defaultValue = types[i].getDefaultValue();
			if (defaultValue == null)
				return null;
			defaultValues[i] = types[i].toString(defaultValue);
		}
		return ParameterTypeTupel.transformTupel2String(defaultStrings);
	}
	
	@Override
	public String getRange() {
		return "tupel";
	}

	@SuppressWarnings("unchecked")
	@Override
	public Element getXML(String key, String value, boolean hideDefault, Document doc) {
		Element element = doc.createElement("parameter");
		element.setAttribute("key", key);
		String valueString = value;
		if (value == null) {
			valueString = transformTupel2String((Pair<String, String>)getDefaultValue());
		}
		element.setAttribute("value", valueString);
		return element;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getXML(String indent, String key, String value, boolean hideDefault) {
		StringBuffer result = new StringBuffer();
		String valueString = value;
		if (value == null) {
			valueString = transformTupel2String((Pair<String, String>)getDefaultValue());
		}

		result.append(indent + "<parameter key=\"" + key + "\" value=\"" + valueString + "\" />" + Tools.getLineSeparator());
		return result.toString();
	}

	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		this.defaultValues = (Object[]) defaultValue;
	}

	public ParameterType getFirstParameterType() {
		return types[0];
	}

	public ParameterType getSecondParameterType() {
		return types[1];
	}

	public ParameterType[] getParameterTypes() {
		return types;
	}

	public static String[] transformString2Tupel(String parameterValue) {
		if (parameterValue.equals(SEPERATOR_CHAR)) {
			return new String[] {"", ""};
		}
		String[] unescaped = parameterValue.split("(?<=[^"+ ESCAPE_CHAR_REGEX + "])" + SEPERATOR_CHAR_REGEX, -1);
		for (int i = 0; i < unescaped.length; i++) {
			unescaped[i] = unescape(unescaped[i]);
		}
		return unescaped;
	}

	private static String unescape(String escapedString) {
		escapedString = escapedString.replace(ESCAPE_CHAR + SEPERATOR_CHAR, SEPERATOR_CHAR);
		escapedString = escapedString.replace(ESCAPE_CHAR + ESCAPE_CHAR, ESCAPE_CHAR);
		return escapedString; 
	}

	public static String transformTupel2String(String firstValue, String secondValue) {
		firstValue = firstValue.replace(ESCAPE_CHAR, ESCAPE_CHAR + ESCAPE_CHAR);
		firstValue = firstValue.replace(SEPERATOR_CHAR, ESCAPE_CHAR + SEPERATOR_CHAR);
		secondValue = secondValue.replace(ESCAPE_CHAR, ESCAPE_CHAR + ESCAPE_CHAR);
		secondValue = secondValue.replace(SEPERATOR_CHAR, ESCAPE_CHAR + SEPERATOR_CHAR);
		return firstValue + SEPERATOR_CHAR + secondValue;
	}
	public static String transformTupel2String(Pair<String, String> pair) {
		return transformTupel2String(pair.getFirst(), pair.getSecond());
	}
	public static String transformTupel2String(String[] tupel) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < tupel.length; i++) {
			String value = tupel[i].replace(ESCAPE_CHAR, ESCAPE_CHAR + ESCAPE_CHAR);
			value = value.replace(SEPERATOR_CHAR, ESCAPE_CHAR + SEPERATOR_CHAR);
			if (i > 0)
				builder.append(SEPERATOR_CHAR);
			builder.append(value);
		}
		return builder.toString();
	}

	@Override
	public String notifyOperatorRenaming(String oldOperatorName, String newOperatorName, String parameterValue) {
		String[] tupel = transformString2Tupel(parameterValue);
		for (int i = 0; i < types.length; i++) {
			tupel[i] = types[i].notifyOperatorRenaming(oldOperatorName, newOperatorName, tupel[i]);
		}
		return transformTupel2String(tupel);
	}
}
