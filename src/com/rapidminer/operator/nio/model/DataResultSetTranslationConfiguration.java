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
package com.rapidminer.operator.nio.model;

import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.ANNOTATION_NAME;
import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.PARAMETER_ANNOTATIONS;
import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.PARAMETER_DATE_FORMAT;
import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.PARAMETER_FIRST_ROW_AS_NAMES;
import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.PARAMETER_LOCALE;
import static com.rapidminer.operator.nio.model.AbstractDataResultSetReader.PARAMETER_META_DATA;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.ExcelExampleSource;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.preprocessing.filter.AbstractDateDataProcessing;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

/**
 * This class holds information how a DataResultSet is translated into an ExampleSet. Therefore it holds information
 * about the final name, the value type, role and if the column is selected at all.
 * 
 * @author Sebastian Land, Simon Fischer
 */
public class DataResultSetTranslationConfiguration {

	private ColumnMetaData[] columnMetaData;

	private Locale locale = Locale.getDefault();
	private String datePattern = "";

	private final SortedMap<Integer, String> annotationsMap = new TreeMap<Integer, String>();
	private boolean faultTolerant = true;

	private DateFormat dateFormat;

	/**
	 * This constructor can be used to generate an empty configuration just depending on the given resultSet
	 * 
	 * @param resultSet
	 * @throws OperatorException 
	 */
	public DataResultSetTranslationConfiguration(AbstractDataResultSetReader readerOperator) {
		this(readerOperator, null);
	}

	/**
	 * Creates the configuration based on the parameter values stored in the given reader. If these parameters aren't
	 * present they are derived from the data result set delivered and everything will just be passed. This means, names
	 * are identically as delivered from the underlying result set, value type will be the one fitting, everything is
	 * selected, no roles are defined.
	 * 
	 * @throws OperatorException
	 */
	private DataResultSetTranslationConfiguration(AbstractDataResultSetReader readerOperator, DataResultSet dataResultSet) {
		reconfigure(dataResultSet);
		reconfigure(readerOperator);
	}

	public void reconfigure(AbstractDataResultSetReader readerOperator) {	
		// reading parameter settings		
		if (readerOperator != null) {
			List<String[]> annotations;
			try {
				annotations = readerOperator.getParameterList(PARAMETER_ANNOTATIONS);
			} catch (UndefinedParameterError e) {
				annotations = Collections.emptyList();
			}
			for (String[] annotation : annotations) {
				annotationsMap.put(Integer.parseInt(annotation[0]), annotation[1]);
			}
			boolean firstRowAsNames = readerOperator.getParameterAsBoolean(PARAMETER_FIRST_ROW_AS_NAMES);
			if (firstRowAsNames) {
				annotationsMap.put(0, ANNOTATION_NAME);
			}			

			// reading date format settings
			try {
				setDatePattern(readerOperator.getParameterAsString(PARAMETER_DATE_FORMAT));
			} catch (UndefinedParameterError e) {
				setDatePattern("");
			}

			try {
				int localeIndex;
				localeIndex = readerOperator.getParameterAsInt(PARAMETER_LOCALE);
				if ((localeIndex >= 0) && (localeIndex < AbstractDateDataProcessing.availableLocales.size()))
					locale = AbstractDateDataProcessing.availableLocales.get(localeIndex);
			} catch (UndefinedParameterError e) {
				locale = Locale.getDefault();
			}

			// initializing data structures
			List<String[]> metaDataSettings;
			if (readerOperator.isParameterSet(PARAMETER_META_DATA)) {
				try {
					metaDataSettings = readerOperator.getParameterList(PARAMETER_META_DATA);
				} catch (UndefinedParameterError e) {
					metaDataSettings = Collections.emptyList();
				}
			} else {
				metaDataSettings = Collections.emptyList();
			}

			columnMetaData = new ColumnMetaData[metaDataSettings.size()];
			for (String[] metaDataDefinition : metaDataSettings) {
				int currentColumn = Integer.parseInt(metaDataDefinition[0]);
				String[] metaDataDefintionValues = ParameterTypeTupel.transformString2Tupel(metaDataDefinition[1]);
				columnMetaData[currentColumn] = new ColumnMetaData();
				final ColumnMetaData cmd = columnMetaData[currentColumn];
				cmd.setSelected(Boolean.parseBoolean(metaDataDefintionValues[1]));
				if (cmd.isSelected()) { // otherwise details don't matter
					cmd.setRole(metaDataDefintionValues[3].trim());
					cmd.setUserDefinedAttributeName(metaDataDefintionValues[0].trim());
					cmd.setAttributeValueType(Integer.parseInt(metaDataDefintionValues[2]));
				}
			}
			
			setFaultTolerant(readerOperator.getParameterAsBoolean(AbstractDataResultSetReader.PARAMETER_ERROR_TOLERANT));
		}
	}

	public void reconfigure(DataResultSet dataResultSet) {
		if (dataResultSet != null) {
			int numberOfColumns = dataResultSet.getNumberOfColumns();
			columnMetaData = new ColumnMetaData[numberOfColumns];
			final String[] originalColumnNames = dataResultSet.getColumnNames();
			int[] attributeValueTypes = dataResultSet.getValueTypes();
			for (int i = 0; i < numberOfColumns; i++) {
				columnMetaData[i] = new ColumnMetaData(originalColumnNames[i],
						originalColumnNames[i],					 
						attributeValueTypes[i],
						Attributes.ATTRIBUTE_NAME,
						true);			
			}
		}		
	}	

	/** Sets the parameters in the given operator to describe this configuration. */
	public void setParameters(AbstractDataResultSetReader operator) {
		operator.getParameters().setParameter(PARAMETER_DATE_FORMAT, getDatePattern());
		List<String[]> metaDataList = new LinkedList<String[]>();
		int index = 0;
		for (ColumnMetaData cmd : getColumnMetaData()) {
			String[] tupel = new String[4];
			tupel[0] = cmd.getUserDefinedAttributeName();
			tupel[1] = String.valueOf(cmd.isSelected());
			tupel[2] = String.valueOf(cmd.getAttributeValueType());
			tupel[3] = cmd.getRole();
			String encodedTupel = ParameterTypeTupel.transformTupel2String(tupel);
			metaDataList.add(new String[] { String.valueOf(index), encodedTupel} );
			index++;
		}
		operator.getParameters().setParameter(PARAMETER_META_DATA, ParameterTypeList.transformList2String(metaDataList));
		operator.getParameters().setParameter(AbstractDataResultSetReader.PARAMETER_ERROR_TOLERANT, String.valueOf(isFaultTolerant()));
	}

	public ColumnMetaData getColumnMetaData(int col) {
		return columnMetaData[col];
	}

	/**
	 * This will return all indices of each selected column
	 */
	public int[] getSelectedIndices() {
		int numberOfSelected = 0;
		int[] selectedIndices = new int[columnMetaData.length];
		for (int i = 0; i < selectedIndices.length; i++) {
			if (columnMetaData[i].isSelected()) {
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

	public SortedSet<Integer> getAnnotatedRowIndices() {
		SortedSet<Integer> result = new TreeSet<Integer>();
		result.addAll(annotationsMap.keySet());
		return result;
	}

	public Map<Integer, String> getAnnotationsMap() {		
		return annotationsMap;
	}

	//	public void setAnnotationsMap(TreeMap<Integer, String> annotationsMap) {
	//		this.annotationsMap = annotationsMap;
	//	}

	/** Returns the row annotated to be used as the name of the attribute or -1
	 *  if no such row was selected. */
	public int getNameRow() {
		if (annotationsMap == null) {
			return -1;
		} else {
			for (Entry<Integer, String> entry : annotationsMap.entrySet()) {
				if (Annotations.ANNOTATION_NAME.equals(entry.getValue())) {
					return entry.getKey();
				}
			}
			return -1;
		}
	}

	public int getNumerOfColumns() {
		return columnMetaData.length;
	}

	public ColumnMetaData[] getColumnMetaData() {
		return columnMetaData;
	}

	public void setFaultTolerant(boolean faultTolerant) {
		this.faultTolerant = faultTolerant;
	}

	public boolean isFaultTolerant() {
		return faultTolerant;
	}

	public int getLastAnnotatedRowIndex() {
		if ((annotationsMap == null) || annotationsMap.isEmpty()) {
			return -1;
		}
		SortedSet<Integer> annotatedRows = getAnnotatedRowIndices();
		return annotatedRows.last();
	}

	public void resetValueTypes() {
		for (ColumnMetaData cmd : columnMetaData) {
			cmd.setAttributeValueType(Ontology.ATTRIBUTE_VALUE);
		}		
	}

	public DateFormat getDateFormat() {
		if (dateFormat == null) {
			if ((getDatePattern() != null) && !getDatePattern().isEmpty()) {
				this.dateFormat = new SimpleDateFormat(getDatePattern(), locale);
			} else {
				this.dateFormat = DateFormat.getDateTimeInstance();
			}
		}
		return this.dateFormat;
	}

	public String getDatePattern() {
		return datePattern;
	}

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
		dateFormat = null;
	}
	
	@Override
	public String toString() {
		return "Annotations: "+annotationsMap+"; columns: "+Arrays.toString(columnMetaData);
	}

	public void addColumnMetaData(ExampleSetMetaData emd) {
		MDInteger numberOfExamples = emd.getNumberOfExamples();
		numberOfExamples.subtract(annotationsMap.size());
		for (ColumnMetaData cmd : columnMetaData) {
			emd.addAttribute(cmd.getAttributeMetaData());
		}
	}

	/** Returns true if meta data is manually set. */
	public boolean isComplete() {
		return (columnMetaData != null) && (columnMetaData.length > 0);
	}
}
