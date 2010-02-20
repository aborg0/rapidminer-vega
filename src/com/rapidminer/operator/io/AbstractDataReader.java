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
package com.rapidminer.operator.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.container.Range;

/**
 * Abstract super class of all example sources reading from files.
 * 
 *  @author Tobias Malbrecht
 */
public abstract class AbstractDataReader extends AbstractExampleSource {
	
	private static final int PREVIEW_LINES = 1000;

	protected abstract class DataSet {
		/**
		 * Proceed to the next row if existent. Should return
		 * true if such a row exists or false, if no such next
		 * row exists.
		 * 
		 * @return
		 */
		public abstract boolean next();
		
		/**
		 * Returns the number of columns in the current row, i.e.
		 * the length of the row.
		 * 
		 * @return
		 */
		public abstract int getNumberOfColumnsInCurrentRow();
		
		/**
		 * Returns whether the value in the specified column
		 * in the current row is missing.
		 * 
		 * @param columnIndex index of the column
		 * @return
		 */
		public abstract boolean isMissing(int columnIndex);
		
		/**
		 * Returns a numerical value contained in the specified column
		 * in the current row. Should return null if the value
		 * is not a numerical or if the value is missing.
		 * 
		 * @param columnIndex
		 * @return
		 */
		public abstract Number getNumber(int columnIndex);

		/**
		 * Returns a nominal value contained in the specified column
		 * in the current row. Should return null if the value
		 * is not a nominal or a kind of string type or if the value
		 * is missing.
		 * 
		 * @param columnIndex
		 * @return
		 */
		public abstract String getString(int columnIndex);

		/**
		 * Returns a date, time or date_time value contained in the
		 * specified column in the current row. Should return null
		 * if the value is not a date or time value or if the value
		 * is missing.
		 * 
		 * @param columnIndex
		 * @return
		 */
		public abstract Date getDate(int columnIndex);
		
		/**
		 * Closes the data source. May tear down a database connection
		 * or close a file which is read from.
		 * 
		 * @throws OperatorException
		 */
		public abstract void close() throws OperatorException;
	}
	
	private int       rowCount          = 0;
	
	private int       columnCount       = 0;
	
	private String[]  columnNames       = null;

	private boolean[] canParseDouble    = null;
	
	private boolean[] canParseInteger   = null;
	
	private boolean[] canParseDate      = null;
	
	private Date[]    lastDates         = null;
	
	private boolean[] shouldBeDate      = null;
	
	private boolean[] shouldBeTime      = null;
	
	private double[]  minValues         = null;
	
	private double[]  maxValues         = null;
	
	private int[]     numberOfMissings  = null;
	
	private ArrayList<LinkedHashSet<String>> valueSets = new ArrayList<LinkedHashSet<String>>();
	
	private int[]     valueTypes        = null;
	
	private boolean   complete          = false;
	
	private boolean   cachePreview      = false;
	
	private LinkedList<Object[]> previewValues = null;
	

	public AbstractDataReader(OperatorDescription description) {
		super(description);
	}
	
	@Override
	protected boolean isMetaDataCacheable() {
		return true;
	}
	
	public void setCachePreview(boolean cachePreview) {
		this.cachePreview = cachePreview;
	}

	protected boolean forceGuessingStop() {
		return (rowCount >= PREVIEW_LINES);
	}

	private boolean isMetaDataGuessComplete() {
		return !complete;
	}

	public LinkedList<Object[]> getPreview() {
		return previewValues;
	}
	
	protected void setColumnNames(String[] columnNames) {
		if (columnCount < columnNames.length) {
			this.columnNames = new String[columnNames.length];
			for (int i = 0; i < columnNames.length; i++) {
				this.columnNames[i] = columnNames[i];
			}
			extendToLength(columnNames.length);
		} else {
			this.columnNames = columnNames;
		}
	}
	
	protected void setValueTypes(int[] valueTypes) {
		if (columnCount < valueTypes.length) {
			this.valueTypes = new int[valueTypes.length];
			for (int i = 0; i < valueTypes.length; i++) {
				this.valueTypes[i] = valueTypes[i];
			}
			extendToLength(valueTypes.length);
		} else {
			this.valueTypes = valueTypes;
		}
	}
	
	protected void init() throws OperatorException {
		rowCount          = 0;
		columnCount       = 0;
		columnNames       = null;
		canParseDouble    = new boolean[columnCount];
		canParseInteger   = new boolean[columnCount];
		canParseDate      = new boolean[columnCount];
		shouldBeDate      = new boolean[columnCount];
		shouldBeTime      = new boolean[columnCount];
		minValues         = new double[columnCount];
		maxValues         = new double[columnCount];
		numberOfMissings  = new int[columnCount];
		valueTypes        = null;
		valueSets.clear();
		complete          = false;
		Calendar lastDateCalendar = Calendar.getInstance();
		Calendar currDateCalendar = Calendar.getInstance();
		
		if (cachePreview) {
			previewValues = new LinkedList<Object[]>();
		}
		
		DataSet set = null;
		try {
			set = getDataSet();
		} catch (IOException e) {
			// TODO add user error
		}
		int maxNumberOfNominalMetaValues = AttributeMetaData.getMaximumNumerOfNominalValues();
		while (set.next()) {
			rowCount++;
			if (columnCount < set.getNumberOfColumnsInCurrentRow()) {
				extendToLength(set.getNumberOfColumnsInCurrentRow());
			}
			Object[] values = new Object[columnCount];
			for (int i = 0; i < set.getNumberOfColumnsInCurrentRow(); i++) {
				if (set.isMissing(i)) {
					values[i] = null;
					numberOfMissings[i]++;
					continue;
				}
				if (canParseDouble[i]) {
					Number number = set.getNumber(i);
					if (number != null) {
						if (Double.isNaN(number.doubleValue())) {
							numberOfMissings[i]++;
							continue;
						}
						if (minValues[i] > number.doubleValue()) {
							minValues[i] = number.doubleValue();
						}
						if (maxValues[i] < number.doubleValue()) {
							maxValues[i] = number.doubleValue(); 
						}
						if (canParseInteger[i]) {
							if (!Tools.isEqual(Math.round(number.doubleValue()), number.intValue())) {
								canParseInteger[i] = false;
							}
						}
						values[i] = number;
						if (valueSets.get(i).size() <= maxNumberOfNominalMetaValues) {
							valueSets.get(i).add(number.toString());
						}
						continue;
					} else {
						canParseDouble[i] = false;
						canParseInteger[i] = false;
					}
				}
				if (canParseDate[i]) {
					Date date = set.getDate(i);
					if (date != null) {
						values[i] = date;
						if (lastDates[i] != null) {
							lastDateCalendar.setTime(lastDates[i]);
							currDateCalendar.setTime(date);
							if (!shouldBeDate[i]) {
								if (lastDateCalendar.get(Calendar.DAY_OF_MONTH) != currDateCalendar.get(Calendar.DAY_OF_MONTH) ||
									lastDateCalendar.get(Calendar.MONTH) != currDateCalendar.get(Calendar.MONTH) ||
									lastDateCalendar.get(Calendar.YEAR) != currDateCalendar.get(Calendar.YEAR)) {
									shouldBeDate[i] = true;
								}
							}
							if (!shouldBeTime[i]) {
								if (lastDateCalendar.get(Calendar.HOUR_OF_DAY) != currDateCalendar.get(Calendar.HOUR_OF_DAY) ||
									lastDateCalendar.get(Calendar.MINUTE) != currDateCalendar.get(Calendar.MINUTE) ||
									lastDateCalendar.get(Calendar.SECOND) != currDateCalendar.get(Calendar.SECOND) ||
									lastDateCalendar.get(Calendar.MILLISECOND) != currDateCalendar.get(Calendar.MILLISECOND)) {
									shouldBeTime[i] = true;
								}
							}
						}
						lastDates[i] = date;
						
						if (minValues[i] > date.getTime()) {
							minValues[i] = date.getTime();
						}
						if (maxValues[i] < date.getTime()) {
							maxValues[i] = date.getTime(); 
						}
						if (valueSets.get(i).size() <= 2) {
							valueSets.get(i).add(date.toString());
						}
						continue;
					} else {
						canParseDate[i] = false;
					}
				}
				String string = set.getString(i);
				if (string != null && !string.isEmpty()) {
					values[i] = string;
					if (valueSets.get(i).size() <= 2) {
						valueSets.get(i).add(string);
					}
				} else {
					numberOfMissings[i]++;
					continue;
				}
			}

			if (cachePreview) {
				previewValues.add(values);
			}
			
			if (forceGuessingStop()) {
				set.close();
				finish(false);
				return;
			}
		}
		
		set.close();
		finish(true);
	}
	
	private void extendToLength(int length) {
		boolean[] newCanParseDouble  = new boolean[length];
		boolean[] newCanParseInteger = new boolean[length];
		boolean[] newCanParseDate    = new boolean[length];
		Date[]    newLastDates          = new Date[length];
		boolean[] newShouldBeDate    = new boolean[length];
		boolean[] newShouldBeTime    = new boolean[length];
		double[] newMinValues = new double[length];
		double[] newMaxValues = new double[length];
		int[] newNumberOfMissings = new int[length];
		for (int i = 0; i < length; i++) {
			newCanParseDouble[i]  = true;
			newCanParseInteger[i] = true;
			newCanParseDate[i]    = true;
			newLastDates[i]       = null; 
			newShouldBeDate[i]    = false;
			newShouldBeTime[i]    = false;
			newMinValues[i] = Double.MAX_VALUE;
			newMaxValues[i] = Double.MIN_VALUE;
			newNumberOfMissings[i] = 0;
		}
		for (int i = 0; i < columnCount; i++) {
			newCanParseDouble[i]  = canParseDouble[i];
			newCanParseInteger[i] = canParseInteger[i];
			newCanParseDate[i]    = canParseDate[i];
			newLastDates[i]       = lastDates[i];
			newShouldBeDate[i]    = shouldBeDate[i];
			newShouldBeTime[i]    = shouldBeTime[i];
			newMinValues[i] = minValues[i];
			newMaxValues[i] = maxValues[i];
			newNumberOfMissings[i] = numberOfMissings[i];
		}
		canParseDouble  = newCanParseDouble;
		canParseInteger = newCanParseInteger;
		canParseDate    = newCanParseDate;
		lastDates       = newLastDates;
		shouldBeDate    = newShouldBeDate;
		shouldBeTime    = newShouldBeTime;
		minValues = newMinValues;
		maxValues = newMaxValues;
		numberOfMissings = newNumberOfMissings;
		int difference = length - valueSets.size();
		for (int i = 0; i < difference; i++) {
			valueSets.add(new LinkedHashSet<String>());
		}
		columnCount = length;
	}
	
	private void finish(boolean complete) throws OperatorException {
		this.complete = complete;
		if (columnNames == null) {
			columnNames = new String[columnCount];
		} else if (columnCount > columnNames.length) {
			String[] newColumnNames = new String[columnCount];
			for (int i = 0; i < columnNames.length; i++) {
				newColumnNames[i] = columnNames[i];
			}
			columnNames = newColumnNames;
		}
		for (int i = 0; i < columnNames.length; i++) {
			if (columnNames[i] == null || columnNames[i].isEmpty()) {
				columnNames[i] = getGenericColumnName(i);
			}
		}
		if (valueTypes == null) {
			valueTypes = new int[columnCount];
			for (int i = 0; i < columnCount; i++) {
				if (numberOfMissings[i] == rowCount) {
					valueTypes[i] = Ontology.NOMINAL;
					continue;
				}
				if (canParseInteger[i]) {
					valueTypes[i] = Ontology.INTEGER;
					continue;
				}
				if (canParseDouble[i]) {
					valueTypes[i] = Ontology.REAL;
					continue;
				}
				if (canParseDate[i]) {
					if (shouldBeDate[i] && shouldBeTime[i]) {
						valueTypes[i] = Ontology.DATE_TIME;
						continue;
					}
					if (shouldBeDate[i]) {
						valueTypes[i] = Ontology.DATE;
						continue;
					}
					if (shouldBeTime[i]) {
						valueTypes[i] = Ontology.TIME;
						continue;
					}
					valueTypes[i] = Ontology.DATE_TIME;
					continue;
				}
				// TODO save value type guessing might be better
//				if (valueSets.get(i).size() <= 2) {
//					valueTypes[i] = Ontology.BINOMINAL;
//					continue;
//				}
				valueTypes[i] = Ontology.NOMINAL;
			}
		}
	}
	
	protected String[] getColumnNames() {
		return columnNames;
	}
	
	protected int getColumnCount() {
		return columnCount;
	}
	
	protected int getRowCount() {
		return rowCount;
	}
	
	protected int[] getValueTypes() {
		return valueTypes;
	}
	
	protected int[] getNumberOfMissings() {
		return numberOfMissings;
	}
	
	protected Set<String> getValueSet(int column) {
		return valueSets.get(column);
	}
	
	protected abstract DataSet getDataSet() throws OperatorException, IOException;

	/**
	 * May be overridden, but should in always return a unique column name
	 * for each column.
	 * 
	 * @param column
	 * @return a unique column name
	 * @throws OperatorException
	 */
	protected String getGenericColumnName(int column) throws OperatorException {
		return "attribute_" + (column + 1);
	}
	
	@Override
	public synchronized MetaData getGeneratedMetaData() throws OperatorException {
		init();
		ExampleSetMetaData metaData = new ExampleSetMetaData();
		for (int i = 0; i < getColumnCount(); i++) {
			String name = getColumnNames()[i];
			if (metaData.getAttributeByName(name) != null) {
				name = getGenericColumnName(i);
			}
			AttributeMetaData amd = new AttributeMetaData(name, getValueTypes()[i]);
			MDInteger missings = new MDInteger(getNumberOfMissings()[i]);
			SetRelation relation = SetRelation.EQUAL;
			if (isMetaDataGuessComplete()) {
				relation = SetRelation.SUPERSET;
				missings.increaseByUnknownAmount();
			}
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(getValueTypes()[i], Ontology.NUMERICAL) ||
				Ontology.ATTRIBUTE_VALUE_TYPE.isA(getValueTypes()[i], Ontology.DATE_TIME)) {
				amd.setValueRange(new Range(minValues[i], maxValues[i]), relation);
			} else {
				amd.setValueSet(getValueSet(i), relation);
			}
			amd.setNumberOfMissingValues(missings);
			metaData.addAttribute(amd);
		}
		metaData.setNumberOfExamples(new MDInteger(getRowCount()));
		if (isMetaDataGuessComplete()) {
			metaData.getNumberOfExamples().increaseByUnknownAmount();
			metaData.attributesAreSuperset();
		}
		columnNames       = null;
		canParseDouble    = null;
		canParseInteger   = null;
		canParseDate      = null;
		minValues         = null;
		maxValues         = null;
		numberOfMissings  = null;
		valueTypes        = null;
		valueSets.clear();
		return metaData;
	}

	protected DataRow generateDataRow(DataSet set, Attribute[] attributes) {
		double[] values = new double[getColumnCount()];
		for (int i = 0; i < getColumnCount(); i++) {
			values[i] = Double.NaN;
		}
		for (int i = 0; i < set.getNumberOfColumnsInCurrentRow(); i++) {
			if (set.isMissing(i)) {
				continue;
			}
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[i].getValueType(), Ontology.NUMERICAL)) {
				values[i] = set.getNumber(i).doubleValue();
				continue;
			}
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[i].getValueType(), Ontology.DATE_TIME)) {
				values[i] = set.getDate(i).getTime();
				continue;
			}
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[i].getValueType(), Ontology.NOMINAL)) {
				values[i] = attributes[i].getMapping().mapString(set.getString(i));
				continue;
			}
		}
		return new DoubleArrayDataRow(values);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		ExampleSetMetaData metaData = (ExampleSetMetaData) getGeneratedMetaData();
		return createExampleSet(metaData);
	}
	
	public ExampleSet createExampleSet(ExampleSetMetaData metaData) throws OperatorException {
		ArrayList<Attribute> attributesList = new ArrayList<Attribute>(metaData.getAllAttributes().size());
		for (AttributeMetaData amd : metaData.getAllAttributes()) {
			attributesList.add(AttributeFactory.createAttribute(amd.getName(), amd.getValueType()));
		}
		Attribute[] attributesArray = new Attribute[attributesList.size()];
		attributesArray = attributesList.toArray(attributesArray);
		MemoryExampleTable table = new MemoryExampleTable(attributesList);
		DataSet set = null;
		try {
			set = getDataSet();
		} catch (IOException e) {
			// TODO throw user error
		}
		while (set.next()) {
			table.addDataRow(generateDataRow(set, attributesArray));
		}
		ExampleSet exampleSet = table.createExampleSet();
		for (AttributeMetaData amd : metaData.getAllAttributes()) {
			if (amd.isSpecial()) {
				exampleSet.getAttributes().setSpecialAttribute(exampleSet.getAttributes().get(amd.getName()), amd.getRole());
			}
		}
		return exampleSet;
	}
}
