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

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.MacroHandler;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;
/**
 * @author Sebastian Land
 */
public class ParameterTypeTupel extends CombinedParameterType {

//	// only one character allowed
//	private static final String ESCAPE_CHAR = "\\";
//	private static final String ESCAPE_CHAR_REGEX = "\\\\";
//	// only one character allowed
//	private static final String SEPERATOR_CHAR_REGEX = "\\.";
	private static final char ESCAPE_CHAR = '\\';
	private static final char XML_SEPERATOR_CHAR = '.';
	private static final char[] XML_SPECIAL_CHARACTERS = new char[] { XML_SEPERATOR_CHAR };
	private static final char INTERNAL_SEPERATOR_CHAR = '.'; //Parameters.PAIR_SEPARATOR; //'.';
	private static final char[] INTERNAL_SPECIAL_CHARACTERS = new char[] { INTERNAL_SEPERATOR_CHAR };


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
		String[] tupel;
		if (value == null) {
			Pair<String, String> defltValue = (Pair<String, String>)getDefaultValue();
			tupel = new String[] { defltValue.getFirst(), defltValue.getSecond() };			
		} else { 
			tupel = transformString2Tupel(value);
		}
		StringBuilder valueString = new StringBuilder();
		boolean first = true;
		for (String part : tupel) {
			if (!first) {
				valueString.append(XML_SEPERATOR_CHAR);
			} else {
				first = false;
			}
			if (part == null) {
				part = "";
			}
			valueString.append(Tools.escape(part, ESCAPE_CHAR, XML_SPECIAL_CHARACTERS));
		}
//		String valueString = value;
//		if (value == null) {
//			valueString = transformTupel2String((Pair<String, String>)getDefaultValue());
//		}		
		element.setAttribute("value", valueString.toString());
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
		if ((parameterValue == null) || parameterValue.isEmpty()){
			return new String[2];
		}
		List<String> split = Tools.unescape(parameterValue, ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS, INTERNAL_SEPERATOR_CHAR);
		return split.toArray(new String[split.size()]);
	}

	public static String transformTupel2String(String firstValue, String secondValue) {
		return 
			Tools.escape(firstValue, ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS) + 
			INTERNAL_SEPERATOR_CHAR + 
			Tools.escape(secondValue, ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS);
	}
	
	public static String transformTupel2String(Pair<String, String> pair) {
		return transformTupel2String(pair.getFirst(), pair.getSecond());
	}
	
	public static String transformTupel2String(String[] tupel) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < tupel.length; i++) {
			if (i > 0) {
				builder.append(INTERNAL_SEPERATOR_CHAR);
			}
			builder.append(Tools.escape(tupel[i], ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS));
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
	
	@Override
	public String transformNewValue(String value) {
		List<String> split = Tools.unescape(value, ESCAPE_CHAR, XML_SPECIAL_CHARACTERS, XML_SEPERATOR_CHAR);
		StringBuilder internalEncoded = new StringBuilder();
		boolean first = true;
		for (String part : split) {
			if (!first) {
				internalEncoded.append(INTERNAL_SEPERATOR_CHAR);
			} else {
				first = false;
			}
			internalEncoded.append(Tools.escape(part, ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS));
		}
		return internalEncoded.toString();
	}
	
	public static String escapeForInternalRepresentation(String string) {
		return Tools.escape(string, ESCAPE_CHAR, INTERNAL_SPECIAL_CHARACTERS);
	}
	
	public String substituteMacros(String parameterValue, MacroHandler mh) {
		if (parameterValue.indexOf("%{") == -1) {
			return parameterValue;
		}
		String[] tupel = transformString2Tupel(parameterValue);
		String[] result = new String[tupel.length];
		for (int i = 0; i < tupel.length; i++) {
			result[i] = types[i].substituteMacros(tupel[i], mh);
		}
		return transformTupel2String(result);	
	}		
}
