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
package com.rapidminer.operator.nio;

import static com.rapidminer.operator.nio.AbstractDataResultSetReader.ANNOTATION_NAME;
import static com.rapidminer.operator.nio.AbstractDataResultSetReader.PARAMETER_ANNOTATIONS;
import static com.rapidminer.operator.nio.AbstractDataResultSetReader.PARAMETER_COLUMN_META_DATA;
import static com.rapidminer.operator.nio.AbstractDataResultSetReader.PARAMETER_DATE_FORMAT;
import static com.rapidminer.operator.nio.AbstractDataResultSetReader.PARAMETER_FIRST_ROW_AS_NAMES;
import static com.rapidminer.operator.nio.AbstractDataResultSetReader.PARAMETER_LOCALE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.preprocessing.filter.AbstractDateDataProcessing;
import com.rapidminer.parameter.ParameterTypeTupel;

/**
 * This class holds information how a DataResultSet is translated into an ExampleSet. Therefore it holds information
 * about the final name, the value type, role and if the column is selected at all.
 * 
 * @author Sebastian Land
 */
public class DataResultSetTranslationConfiguration {

	private String[] attributeNames;
	private String[] userDefinedAttributeNames;
	private int[] attributeValueTypes;
	private String[] roleIds;
	private boolean[] areSelected;

	private Locale locale = Locale.US;
	private String datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	private TreeMap<Integer, String> annotationsMap = new TreeMap<Integer, String>();

	/**
	 * Constructor for filling the complete data structure at once.
	 * 
	 * @param attributeNames
	 * @param attributeValueTypes
	 * @param roleIds
	 * @param areSelected
	 */
	public DataResultSetTranslationConfiguration(String[] attributeNames, int[] attributeValueTypes, String[] roleIds, boolean[] areSelected) {
		super();
		this.attributeNames = attributeNames;
		this.attributeValueTypes = attributeValueTypes;
		this.roleIds = roleIds;
		this.areSelected = areSelected;
	}

	/**
	 * Creates the configuration based on the parameter values stored in the given reader. If these parameters aren't
	 * present they are derived from the data result set delivered and everything will just be passed. This means, names
	 * are identically as delivered from the underlying result set, value type will be the one fitting, everything is
	 * selected, no roles are defined.
	 * 
	 * @throws OperatorException
	 */
	public DataResultSetTranslationConfiguration(AbstractDataResultSetReader readerOperator, DataResultSet dataResultSet) throws OperatorException {
		// reading parameter settings
		List<String[]> metaDataSettings = Collections.emptyList();
		if (readerOperator != null) {
			boolean firstRowAsNames = readerOperator.getParameterAsBoolean(PARAMETER_FIRST_ROW_AS_NAMES);
			if (firstRowAsNames) {
				annotationsMap.put(0, ANNOTATION_NAME);
			} else {
				List<String[]> annotations = readerOperator.getParameterList(PARAMETER_ANNOTATIONS);
				for (String[] annotation : annotations) {
					annotationsMap.put(Integer.parseInt(annotation[0]), annotation[1]);
				}
			}

			// initializing data structures
			if (readerOperator.isParameterSet(PARAMETER_COLUMN_META_DATA)) {
				metaDataSettings = readerOperator.getParameterList(PARAMETER_COLUMN_META_DATA);
			}
		
			// reading date format settings
			datePattern = readerOperator.getParameterAsString(PARAMETER_DATE_FORMAT);
			int localeIndex = readerOperator.getParameterAsInt(PARAMETER_LOCALE);
			if ((localeIndex >= 0) && (localeIndex < AbstractDateDataProcessing.availableLocales.size()))
				locale = AbstractDateDataProcessing.availableLocales.get(readerOperator.getParameterAsInt(PARAMETER_LOCALE));
		}
		
		int numberOfColumns = dataResultSet.getNumberOfColumns();
		attributeNames = dataResultSet.getColumnNames();
		roleIds = new String[numberOfColumns];
		areSelected = new boolean[numberOfColumns];
		userDefinedAttributeNames = new String[numberOfColumns];

		Arrays.fill(areSelected, true);
		Arrays.fill(roleIds, Attributes.ATTRIBUTE_NAME);
		attributeValueTypes = dataResultSet.getValueTypes();

		for (String[] metaDataDefinition : metaDataSettings) {
			int currentColumn = Integer.parseInt(metaDataDefinition[0]);
			String[] metaDataDefintionValues = ParameterTypeTupel.transformString2Tupel(metaDataDefinition[1]);
			areSelected[currentColumn] = Boolean.parseBoolean(metaDataDefintionValues[1]);
			if (areSelected[currentColumn]) {
				// otherwise everything else doesn't matter at all
				roleIds[currentColumn] = metaDataDefintionValues[3].trim();
				userDefinedAttributeNames[currentColumn] = metaDataDefintionValues[0].trim();

				// TODO: Might introduce checking if value type matches guessed type
				attributeValueTypes[currentColumn] = Integer.parseInt(metaDataDefintionValues[2]);
			}
		}
	}

	/**
	 * This constructor can be used to generate an empty configuration just depending on the given resultSet
	 * 
	 * @param resultSet
	 * @throws OperatorException 
	 */
	public DataResultSetTranslationConfiguration(DataResultSet resultSet) throws OperatorException {
		this(null, resultSet);
	}

	/**
	 * This returns the user defined name or null if no such name exists.
	 */
	public String getUserDefinedName(int i) {
		return userDefinedAttributeNames[i];
	}

	public String getAttributeName(int i) {
		return attributeNames[i];
	}

	public void setAttributeName(int columnIndex, String attributeName) {
		this.attributeNames[columnIndex] = attributeName;
	}

	public int getAttributeValueType(int i) {
		return attributeValueTypes[i];
	}

	public int[] getAttributeValueTypes() {
		return attributeValueTypes;
	}

	public void setAttributeValueTypes(int[] attributeValueType) {
		this.attributeValueTypes = attributeValueType;
	}

	public String getRoleId(int i) {
		return roleIds[i];
	}

	public void setRoleId(String[] roleId) {
		this.roleIds = roleId;
	}

	public boolean[] getSelected() {
		return areSelected;
	}

	public void setSelected(boolean[] selected) {
		this.areSelected = selected;
	}

	public String[] getAttributeNames() {
		return attributeNames;
	}

	public boolean isSelected(int i) {
		return areSelected[i];
	}

	/**
	 * This will return all indices of each selected column
	 */
	public int[] getSelectedIndices() {
		int numberOfSelected = 0;
		int[] selectedIndices = new int[areSelected.length];
		for (int i = 0; i < areSelected.length; i++) {
			if (areSelected[i]) {
				selectedIndices[numberOfSelected] = i;
				numberOfSelected++;
			}
		}
		return selectedIndices;
	}

	/**
	 * This method returns true if either the names have been user specified or were retrieved from a column
	 */
	public boolean hasNames() {
		return annotationsMap.containsKey(ExcelExampleSource.ANNOTATION_NAME);
	}

	/**
	 * This returns the annotation of a line or null if no present
	 */
	public String getAnnotation(int line) {
		return annotationsMap.get(line);
	}

	public void setAnnotationsMap(TreeMap<Integer, String> annotationsMap) {
		this.annotationsMap = annotationsMap;
	}

	public String getDatePattern() {
		return datePattern;
	}

	// building attributes according to configuration: Using default system dependent attribute names
	// int localeIndex = ;
	// Locale selectedLocale = Locale.US;
	// if ((localeIndex >= 0) && (localeIndex < availableLocales.size()))
	// selectedLocale = availableLocales.get(getParameterAsInt(PARAMETER_LOCALE));
	public Locale getLocale() {
		return locale;
	}

}
