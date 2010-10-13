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

import static com.rapidminer.operator.nio.ExcelExampleSource.PARAMETER_EXCEL_FILE;
import static com.rapidminer.operator.nio.ExcelExampleSource.PARAMETER_SHEET_NUMBER;

import java.io.File;
import java.io.IOException;

import javax.swing.table.TableModel;

import jxl.Workbook;
import jxl.read.biff.BiffException;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.ExcelExampleSource;
import com.rapidminer.operator.nio.ExcelSheetTableModel;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;

/**
 * A class holding information about configuration of the Excel Result Set
 * 
 * @author Sebastian Land
 */
public class ExcelResultSetConfiguration implements DataResultSetFactory {

	private int rowOffset = -1;
	private int columnOffset = -1;
	private int rowLast = Integer.MAX_VALUE;
	private int columnLast = Integer.MAX_VALUE;
	/** Numbering starts at 0. */
	private int sheet = -1;

	private Workbook preOpenedWorkbook;
	private File workbookFile;

	/**
	 * This constructor must read in all settings from the parameters of the given operator.
	 * @throws OperatorException 
	 */
	public ExcelResultSetConfiguration(ExcelExampleSource excelExampleSource) throws OperatorException {
		if (excelExampleSource.isParameterSet(ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE)) {
			parseExcelRange(excelExampleSource.getParameterAsString(ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE));
		}
//		if (excelExampleSource.isParameterSet(PARAMETER_COLUMN_OFFSET))
//			this.columnOffset = excelExampleSource.getParameterAsInt(PARAMETER_COLUMN_OFFSET);
//		if (excelExampleSource.isParameterSet(PARAMETER_ROW_OFFSET))
//			this.rowOffset = excelExampleSource.getParameterAsInt(PARAMETER_ROW_OFFSET);
		if (excelExampleSource.isParameterSet(PARAMETER_SHEET_NUMBER))
			this.sheet = excelExampleSource.getParameterAsInt(PARAMETER_SHEET_NUMBER) - 1;
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

	/** Returns whether {@link #getWorkbook()} can be called without blocking. */
	public boolean hasWorkbook() {
		return preOpenedWorkbook != null;
	}

	/**
	 * This will return a workbook if already delivered with the configuration. This 
	 * workbook must not be closed!
	 * @throws IOException 
	 * @throws BiffException 
	 */
	public Workbook getWorkbook() throws BiffException, IOException {
		if (preOpenedWorkbook == null) {
			File file = getFile();
			preOpenedWorkbook = Workbook.getWorkbook(file);
		}
		return preOpenedWorkbook;
	}

	/**
	 * This returns the file of the referenced excel file
	 */
	public File getFile() {
		return workbookFile;
	}

	/**
	 * This will set the workbook file. It will assure that an existing preopened workbook will be closed if files
	 * differ.
	 */
	public void setWorkbookFile(File selectedFile) {
		if (selectedFile.equals(this.workbookFile)) {
			return;
		}
		if (hasWorkbook()) {
			preOpenedWorkbook.close();
			preOpenedWorkbook = null;
		}
		workbookFile = selectedFile;
		preOpenedWorkbook = null;
		rowOffset = 0;
		columnOffset = 0;
		rowLast = Integer.MAX_VALUE;
		columnLast = Integer.MAX_VALUE;
		sheet = 0;
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

	@Override
	public TableModel makePreviewTableModel(ProgressListener listener) throws OperatorException {
		try {
			return new ExcelSheetTableModel(this);
		} catch (IndexOutOfBoundsException e) {
			throw new UserError(null, 302, getFile().getPath(), e.getMessage());
		} catch (BiffException e) {
			throw new UserError(null, 302, getFile().getPath(), e.getMessage());
		} catch (IOException e) {
			throw new UserError(null, 302, getFile().getPath(), e.getMessage());
		}
	}

	public void closeWorkbook() {
		if (preOpenedWorkbook == null) {
			preOpenedWorkbook.close();
			preOpenedWorkbook = null;
		}
	}

	public void setParameters(ExcelExampleSource source) {
		String range = 
			Tools.getExcelColumnName(columnOffset) + (rowOffset+1) +
			":" + 
			Tools.getExcelColumnName(columnLast) + (rowLast + 1);
		source.setParameter(ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE, range);
		source.setParameter(PARAMETER_SHEET_NUMBER, String.valueOf(sheet + 1));
		source.setParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE, workbookFile.getAbsolutePath());
	}
	
	public void parseExcelRange(String range) throws OperatorException {
		String[] split = range.split(":", 2);
		int[] topLeft = parseExcelCell(split[0]);
		columnOffset = topLeft[0];
		rowOffset = topLeft[1];
		if (split.length < 2) {
			rowLast = Integer.MAX_VALUE;
			columnLast = Integer.MAX_VALUE;
		} else {
			int[] bottomRight = parseExcelCell(split[1]);
			columnLast = bottomRight[0];
			rowLast    = bottomRight[1];
		}		
	}

	private static int[] parseExcelCell(String string) throws OperatorException {
		int i = 0;
		int column = 0;
		int row = 0;
		while (i < string.length() && (Character.isLetter(string.charAt(i)))) {
			char c = string.charAt(i);
			c = Character.toUpperCase(c);
			column *= 26;
			column += (c - 'A') + 1;
			i++;
		}
		if (i < string.length()) { // at least one digit left
			String columnStr = string.substring(i);
			try {
				row = Integer.parseInt(columnStr);
			} catch (NumberFormatException e) {
				throw new OperatorException("Illegal Excel range format: "+string);
			}
		}		
		return new int[] { column - 1, row - 1};
	}

	@Override
	public String getResourceName() {		
		return workbookFile.getAbsolutePath();
	}
}
