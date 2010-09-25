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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;

/**
 * This class encapsulates the translation step from a DataResultSet to an ExampleSet which is controlled by the
 * DataResultSetTranslationConfiguration.
 * 
 * @author Sebastian Land
 */
public class DataResultSetTranslator {

	private boolean shouldStop = false;
	private boolean isReading = false;
	private Object isReadingMutex = new Object();

	private DataResultSet dataResultSet;

	public DataResultSetTranslator(DataResultSet resultSet) {
		this.dataResultSet = resultSet;
	}

	/**
	 * This method will start the translation of the actual ResultDataSet to an ExampleSet. 
	 * 
	 */
	public ExampleSet read(DataResultSetTranslationConfiguration configuration, boolean isFaultTolerant, int maxRows, ProgressListener listener) throws OperatorException {
		synchronized (isReadingMutex) {
			isReading = true;
			int[] attributeColumns = configuration.getSelectedIndices();
			int numberOfAttributes = attributeColumns.length;

			Attribute[] attributes = new Attribute[numberOfAttributes];
			for (int i = 0; i < attributes.length; i++) {
				int attributeValueType = configuration.getAttributeValueType(attributeColumns[i]);
				if (attributeValueType == Ontology.ATTRIBUTE_VALUE)  //fallback for uninitialized reading.
					attributeValueType = Ontology.POLYNOMINAL;
				attributes[i] = AttributeFactory.createAttribute(configuration.getAttributeName(attributeColumns[i]), attributeValueType);
			}

			// building example table
			MemoryExampleTable exampleTable = new MemoryExampleTable(attributes);

			// now iterate over complete dataResultSet and copy data
			// TODO: Insert DataRowFactory
			int currentRow = 0;
			dataResultSet.reset(listener);
			while (dataResultSet.hasNext() && !shouldStop && (currentRow < maxRows || maxRows <= 0)) {
				dataResultSet.next(listener);

				// checking for annotation
				String currentAnnotation = configuration.getAnnotation(currentRow);
				if (currentAnnotation != null) {
					// registering annotation on all attributes
					int attributeIndex = 0;
					for (Attribute attribute : attributes) {
						if (AbstractDataResultSetReader.ANNOTATION_NAME.equals(currentAnnotation)) {
							// resetting name
							String newAttributeName = dataResultSet.getString(attributeColumns[attributeIndex]);
							if (newAttributeName != null && !newAttributeName.isEmpty()) {
								attribute.setName(newAttributeName);
							}
						} else {
							// setting annotation
							String annotationValue = dataResultSet.getString(attributeColumns[attributeIndex]);
							if (annotationValue != null && !annotationValue.isEmpty())
								attribute.getAnnotations().put(currentAnnotation, annotationValue);
						}
						attributeIndex++;
					}
				} else {
					// creating data row
					DoubleArrayDataRow row = new DoubleArrayDataRow(new double[attributes.length]);
					exampleTable.addDataRow(row);
					int attributeIndex = 0;
					for (Attribute attribute : attributes) {
						// check for missing
						if (dataResultSet.isMissing(attributeColumns[attributeIndex])) {
							row.set(attribute, Double.NaN);
						} else {
							switch (attribute.getValueType()) {
							case Ontology.INTEGER:
								row.set(attribute, getNumber(dataResultSet, attributeColumns[attributeIndex], isFaultTolerant).intValue());
								break;
							case Ontology.REAL:
								row.set(attribute, getNumber(dataResultSet, attributeColumns[attributeIndex], isFaultTolerant).doubleValue());
								break;
							case Ontology.DATE_TIME:
							case Ontology.TIME:
							case Ontology.DATE:
								row.set(attribute, getDate(dataResultSet, attributeColumns[attributeIndex], isFaultTolerant));
								break;
							default:
								String value = dataResultSet.getString(attributeColumns[attributeIndex]);
								int mapIndex = attribute.getMapping().mapString(value);
								row.set(attribute, mapIndex);
							}
						}
						attributeIndex++;
					}
				}
				currentRow++;
			}

			// derive ExampleSet from exampleTable and assigning roles
			ExampleSet exampleSet = exampleTable.createExampleSet();
			Attributes exampleSetAttributes = exampleSet.getAttributes();
			int attributeIndex = 0;
			for (Attribute attribute : attributes) {
				// if user defined names have been found, rename accordingly
				String userDefinedName = configuration.getUserDefinedName(attributeColumns[attributeIndex]);
				if (userDefinedName != null)
					attribute.setName(userDefinedName);
				String roleId = configuration.getRoleId(attributeColumns[attributeIndex]);
				if (!Attributes.ATTRIBUTE_NAME.equals(roleId))
					exampleSetAttributes.setSpecialAttribute(attribute, roleId);
				attributeIndex++;
			}

			isReading = false;
			if (listener != null)
				listener.complete();
			return exampleSet;
		}
	}

	private double getDate(DataResultSet dataResultSet, int column, boolean isFaultTolerant) throws OperatorException {
		Date result = dataResultSet.getDate(column);
		if (result == null) {
			if (isFaultTolerant)
				return Double.NaN;
			else
				throw new OperatorException("Can't parse");
		}
		return result.getTime();
	}

	private Number getNumber(DataResultSet dataResultSet, int column, boolean isFaultTolerant) throws OperatorException {
		Number result = dataResultSet.getNumber(column);
		if (result == null) {
			if (isFaultTolerant)
				return Double.NaN;
			else
				throw new OperatorException("Can't parse");
		}
		return result;
	}

	public DataResultSetTranslationConfiguration guessValueTypes(DataResultSetTranslationConfiguration configuration, DataResultSet dataResultSet, ProgressListener listener) throws OperatorException {
		int maxProbeRows = 100;
		try {
			maxProbeRows = Integer.parseInt(RapidMiner.getRapidMinerPropertyValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS));
		} catch (NumberFormatException e) {
		}
		return guessValueTypes(configuration, dataResultSet, maxProbeRows, listener);
	}

	public DataResultSetTranslationConfiguration guessValueTypes(DataResultSetTranslationConfiguration configuration, DataResultSet dataResultSet, int maxNumberOfRows, ProgressListener listener) throws OperatorException {
		configuration.setAttributeValueTypes(guessValueTypes(configuration.getAttributeValueTypes(), configuration, dataResultSet, maxNumberOfRows, listener));
		return configuration;
	}

	/**
	 * This method will select the most appropriate value types defined on the first few thousand rows.
	 * 
	 * @throws OperatorException
	 */
	private int[] guessValueTypes(int[] definedTypes, DataResultSetTranslationConfiguration configuration, DataResultSet dataResultSet, int maxProbeRows, ProgressListener listener) throws OperatorException {
		if (listener != null)
			listener.setTotal(1 + maxProbeRows);
		DateFormat dateFormat = new SimpleDateFormat(configuration.getDatePattern(), configuration.getLocale());
		boolean[] needsGuessing = new boolean[definedTypes.length];
		boolean needsGuessingAtAll = false;

		for (int i = 0; i < definedTypes.length; i++) {
			if (definedTypes[i] == Ontology.ATTRIBUTE_VALUE) {
				needsGuessingAtAll = true;
				needsGuessing[i] = true;
			}
		}
		if (listener != null)
			listener.setCompleted(1);
		if (needsGuessingAtAll) {
			int[] columnValueTypes = new int[dataResultSet.getNumberOfColumns()];
			Arrays.fill(columnValueTypes, Ontology.INTEGER);

			// TODO: The following could be made more efficient using an indirect indexing to access the columns: would
			// save array over all
			dataResultSet.reset(null);
			int currentRow = 0;
			String[][] valueBuffer = new String[dataResultSet.getNumberOfColumns()][2];
			while (dataResultSet.hasNext() && (currentRow < maxProbeRows || maxProbeRows <= 0)) {
				dataResultSet.next(null);
				if (listener != null)
					listener.setCompleted(1 + currentRow);

				// skip rows with annotations
				if (configuration.getAnnotation(currentRow) == null) {
					for (int column = 0; column < dataResultSet.getNumberOfColumns(); column++) {
						// don't guess for already defined or polynomial types
						if (needsGuessing[column] && definedTypes[column] != Ontology.POLYNOMINAL && !dataResultSet.isMissing(column)) {
							String value = dataResultSet.getString(column);

							// fill value buffer for binominal assessment
							if (valueBuffer[column] != null) {
								// first check if already more than two values: Encoded by null inner array
								if (!value.equals(valueBuffer[column][0]) && !value.equals(valueBuffer[column][1])) {
									if (valueBuffer[column][0] == null)
										valueBuffer[column][0] = value;
									else if (valueBuffer[column][1] == null)
										valueBuffer[column][1] = value;
									else
										valueBuffer[column] = null;
								}
							}
							definedTypes[column] = guessValueType(definedTypes[column], value, valueBuffer[column] != null, dateFormat);
						}
					}
				}
				currentRow++;
			}
		}
		if (listener != null)
			listener.complete();
		return definedTypes;
	}

	/**
	 * This method tries to guess the value type by taking into account the current guessed type and the string value.
	 * The type will be transformed to more general ones.
	 */
	private int guessValueType(int currentValueType, String value, boolean onlyTwoValues, DateFormat dateFormat) {
		if (currentValueType == Ontology.POLYNOMINAL)
			return currentValueType;
		if (currentValueType == Ontology.BINOMINAL) {
			if (onlyTwoValues)
				return Ontology.BINOMINAL;
			else
				return Ontology.POLYNOMINAL;
		}
		if (currentValueType == Ontology.DATE) {
			try {
				dateFormat.parse(value);
				return currentValueType;
			} catch (ParseException e) {
				return guessValueType(Ontology.BINOMINAL, value, onlyTwoValues, dateFormat);
			}
		}
		if (currentValueType == Ontology.REAL) {
			try {
				Double.parseDouble(value);
				return currentValueType;
			} catch (NumberFormatException e) {
				return guessValueType(Ontology.DATE, value, onlyTwoValues, dateFormat);
			}
		}
		try {
			Integer.parseInt(value);
			return Ontology.INTEGER;
		} catch (NumberFormatException e) {
			return guessValueType(Ontology.REAL, value, onlyTwoValues, dateFormat);
		}
	}

	/**
	 * This method will stop any ongoing read action and close the underlying DataResultSet. It will wait until this has
	 * been successfully performed.
	 * 
	 * @throws OperatorException
	 */
	public void close() throws OperatorException {
		if (isReading) {
			shouldStop = true;
			synchronized (isReadingMutex) {
				shouldStop = false;
			}
		}
		dataResultSet.close();
	}

}
