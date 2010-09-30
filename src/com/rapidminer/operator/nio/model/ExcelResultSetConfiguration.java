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

import static com.rapidminer.operator.nio.ExcelExampleSource.PARAMETER_COLUMN_OFFSET;
import static com.rapidminer.operator.nio.ExcelExampleSource.PARAMETER_EXCEL_FILE;
import static com.rapidminer.operator.nio.ExcelExampleSource.PARAMETER_ROW_OFFSET;
import static com.rapidminer.operator.nio.ExcelExampleSource.PARAMETER_SHEET_NUMBER;

import java.io.File;

import jxl.Workbook;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.ExcelExampleSource;
import com.rapidminer.parameter.UndefinedParameterError;

/**
 * A class holding information about configuration of the Excel Result Set
 * 
 * @author Sebastian Land
 */
public class ExcelResultSetConfiguration implements DataResultSetFactory {

	private int rowOffset = 0;
	private int columnOffset = 0;
	private int rowLast = Integer.MAX_VALUE;
	private int columnLast = Integer.MAX_VALUE;
	private int sheet = 0;

	private Workbook preOpenedWorkbook;
	private File workbookFile;

	public ExcelResultSetConfiguration(int rowOffset, int columnOffset, int sheet, File workbookFile) {
		this.rowOffset = rowOffset;
		this.columnOffset = columnOffset;
		this.sheet = sheet;
		this.workbookFile = workbookFile;
		this.preOpenedWorkbook = null;
	}

	/**
	 * This constructor must read in all settings from the parameters of the given operator.
	 * 
	 * @throws UndefinedParameterError
	 */
	public ExcelResultSetConfiguration(ExcelExampleSource excelExampleSource) throws UndefinedParameterError {
		if (excelExampleSource.isParameterSet(PARAMETER_COLUMN_OFFSET))
			this.columnOffset = excelExampleSource.getParameterAsInt(PARAMETER_COLUMN_OFFSET);
		if (excelExampleSource.isParameterSet(PARAMETER_ROW_OFFSET))
			this.rowOffset = excelExampleSource.getParameterAsInt(PARAMETER_ROW_OFFSET);
		if (excelExampleSource.isParameterSet(PARAMETER_SHEET_NUMBER))
			this.sheet = excelExampleSource.getParameterAsInt(PARAMETER_SHEET_NUMBER);
		if (excelExampleSource.isParameterSet(PARAMETER_EXCEL_FILE))
			this.workbookFile = excelExampleSource.getParameterAsFile(PARAMETER_EXCEL_FILE);
	}

	/**
	 * This will create a completely empty result set configuration
	 */
	public ExcelResultSetConfiguration() {

	}

	/**
	 * Returns the RowOffset
	 */
	public int getRowOffset() {
		return rowOffset;
	}

	/**
	 * Returns the ColumnOffset
	 */
	public int getColumnOffset() {
		return columnOffset;
	}

	/**
	 * This returns the number of the sheet that must be returned. Counting starts at 1.
	 */
	public int getSheetNumber() {
		return sheet;
	}

	/**
	 * This returns if there is already a opened workbook present
	 */
	public boolean hasWorkbook() {
		return preOpenedWorkbook != null;
	}

	/**
	 * This will return a workbook if already delivered with the configuration. This 
	 * workbook must not be closed!
	 */
	public Workbook getWorkbook() {
		return preOpenedWorkbook;
	}

	/**
	 * This returns the file of the referenced excel file
	 */
	public File getFile() {
		return workbookFile;
	}

//	public void setPreOpenedWorkbook(Workbook preOpenedWorkbook) {
//		this.preOpenedWorkbook = preOpenedWorkbook;
//	}

	/**
	 * This will set the workbook file. It will assure that an existing preopened workbook will be closed if files
	 * differ.
	 */
	public void setWorkbookFile(File selectedFile) {
		if (hasWorkbook() && !selectedFile.equals(workbookFile)) {
			preOpenedWorkbook.close();
		}
		workbookFile = selectedFile;
	}

	public int getRowLast() {
		return rowLast;
	}

	public void setRowLast(int rowLast) {
		this.rowLast = rowLast;
	}

	public int getColumnLast() {
		return columnLast;
	}

	public void setColumnLast(int columnLast) {
		this.columnLast = columnLast;
	}

	public int getSheet() {
		return sheet;
	}

	public void setSheet(int sheet) {
		this.sheet = sheet;
	}

	public void setRowOffset(int rowOffset) {
		this.rowOffset = rowOffset;
	}

	public void setColumnOffset(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public DataResultSet makeDataResultSet(Operator operator) throws OperatorException {
		return new ExcelResultSet(operator, this);
	}
}
