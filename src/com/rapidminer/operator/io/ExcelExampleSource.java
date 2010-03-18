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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;

/**
 * <p>This operator can be used to load data from Microsoft Excel spreadsheets. 
 * This operator is able to reads data from Excel 95, 97, 2000, XP, and 2003.
 * The user has to define which of the spreadsheets in the workbook should be
 * used as data table. The table must have a format so that each line is an example
 * and each column represents an attribute. Please note that the first line might
 * be used for attribute names which can be indicated by a parameter.</p>
 * 
 * <p>The data table can be placed anywhere on the sheet and is allowed to
 * contain arbitrary formatting instructions, empty rows, and empty columns. Missing data values
 * are indicated by empty cells or by cells containing only &quot;?&quot;.</p>
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ExcelExampleSource extends AbstractDataReader {

	/** Pseudo-annotation to be used for attribute names. */
	public static final String ANNOTATION_NAME = "Name";

	/** The parameter name for &quot;The Excel spreadsheet file which should be loaded.&quot; */
	public static final String PARAMETER_EXCEL_FILE = "excel_file";

	static {
		AbstractReader.registerReaderDescription(new ReaderDescription("xls", ExcelExampleSource.class, PARAMETER_EXCEL_FILE));
	}

	/** The parameter name for &quot;The number of the sheet which should be imported.&quot; */
	public static final String PARAMETER_SHEET_NUMBER = "sheet_number";

	/** The parameter name for &quot;Indicates if the first row should be used for the attribute names.&quot; */
	public static final String PARAMETER_FIRST_ROW_AS_NAMES = "first_row_as_names";

	/** The parameter name for &quot;Indicates which column should be used for the label attribute (0: no label)&quot; */
	public static final String PARAMETER_LABEL_COLUMN = "label_column";

	/** The parameter name for &quot;Indicates which column should be used for the Id attribute (0: no id)&quot; */
	public static final String PARAMETER_ID_COLUMN = "id_column";

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";

	public static final String PARAMETER_COLUMN_OFFSET = "column_offset";

	public static final String PARAMETER_ROW_OFFSET = "row_offset";

	public static final String PARAMETER_CREATE_LABEL = "create_label";

	public static final String PARAMETER_CREATE_ID = "create_id";

	public static final String PARAMETER_ANNOTATIONS = "annotations";

	public ExcelExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	protected DataSet getDataSet() throws OperatorException {
		List<String[]> allAnnotations = getParameterList(PARAMETER_ANNOTATIONS);
		final Map<Integer,String> annotationsMap = new HashMap<Integer,String>();
		boolean nameFound = false;
		int lastAnnotatedRow = 0;
		int nameRow = -1;
		for (String[] pair : allAnnotations) {
			try {
				final int row = Integer.parseInt(pair[0]);
				if (row > lastAnnotatedRow) {
					lastAnnotatedRow = row;
				}
				annotationsMap.put(row, pair[1]);
				if (ANNOTATION_NAME.equals(pair[1])) {
					nameFound = true;
					nameRow = row;
				}
			} catch (NumberFormatException e) {
				throw new OperatorException("row_number entries in parameter list "+PARAMETER_ANNOTATIONS+" must be integers.", e);
			}
		}
		if (nameFound && getParameterAsBoolean(PARAMETER_FIRST_ROW_AS_NAMES)) {
			throw new OperatorException("If "+PARAMETER_FIRST_ROW_AS_NAMES+" is set to true, you cannot use " + ANNOTATION_NAME +" entries in parameter list "+PARAMETER_ANNOTATIONS+".");
		}
		if (getParameterAsBoolean(PARAMETER_FIRST_ROW_AS_NAMES)) {
			annotationsMap.put(1, ANNOTATION_NAME);
			nameRow = 0;
		}
		final int lastAnnotatedRowF = lastAnnotatedRow;
		final int nameRowF = nameRow;
		
		return new DataSet() {
			private Workbook workbook = null;
			
			private Sheet sheet = null;
			
			private Cell[] cells = null;
			
			private SortedSet<Integer> emptyRows = new TreeSet<Integer>();
			
			private SortedSet<Integer> emptyColumns = new TreeSet<Integer>();
			
			private int rowOffset       = getParameterAsInt(PARAMETER_ROW_OFFSET);
			private int columnOffset    = getParameterAsInt(PARAMETER_COLUMN_OFFSET);
			private int numberOfRows    = 0;
			private int numberOfColumns = 0;
			private int currentRow      = rowOffset + lastAnnotatedRowF;
			
			{			
				try {
					workbook = Workbook.getWorkbook(getParameterAsInputStream(PARAMETER_EXCEL_FILE));
					sheet = workbook.getSheet(getParameterAsInt(PARAMETER_SHEET_NUMBER) - 1);
				} catch (IOException e) {
					throw new UserError(ExcelExampleSource.this, 302, getParameter(PARAMETER_EXCEL_FILE), e.getMessage());
				} catch (BiffException e) {
					throw new UserError(ExcelExampleSource.this, 302, getParameter(PARAMETER_EXCEL_FILE), e.getMessage());
				}
				numberOfColumns = sheet.getColumns();
				numberOfRows = sheet.getRows();
				
				// TODO unifiy offset and emptyness checks in one loop
				// determine offsets
				boolean contentFound = false;
				for (int r = rowOffset; r < numberOfRows; r++) {
					for (int c = columnOffset; c < numberOfColumns; c++) {
						if (sheet.getCell(c, r).getType() != CellType.EMPTY) {
							columnOffset = c;
							contentFound = true;
							break;
						}
					}
					if (contentFound) {
						rowOffset = r;
						break;
					}
				}
				if (!contentFound) {
					throw new UserError(ExcelExampleSource.this, 302, getParameter(PARAMETER_EXCEL_FILE), "spreadsheet seems to be empty");
				}

				// determine empty rows
				for (int r = rowOffset; r < numberOfRows; r++) {
					boolean rowEmpty = true;
					for (int c = columnOffset; c < numberOfColumns; c++) {
						if (sheet.getCell(c, r).getType() != CellType.EMPTY) {
							rowEmpty = false;
							break;
						}
					}
					if (rowEmpty) {
						emptyRows.add(r);
					}
				}

				// determine empty columns
				for (int c = columnOffset; c < numberOfColumns; c++) {
					boolean columnEmpty = true;
					for (int r = rowOffset; r < numberOfRows; r++) {
						if (sheet.getCell(c, r).getType() != CellType.EMPTY) {
							columnEmpty = false;
							break;
						}
					}
					if (columnEmpty) {
						emptyColumns.add(c);
					}
				}				

				// attribute names
				String[] attributeNames = new String[numberOfColumns - columnOffset - emptyColumns.size()];
				if (nameRowF != -1) {
					int columnCounter = 0;
					for (int c = columnOffset; c < numberOfColumns; c++) {
						// skip empty columns
						if (emptyColumns.contains(c))
							continue;
						Cell cell = sheet.getCell(c, rowOffset + nameRowF);
						attributeNames[columnCounter++] = cell.getContents();
					}
				}
				setColumnNames(attributeNames);
				
				// Annotations
				int columnCounter = 0;
				Annotations[] annotationss = new Annotations[numberOfColumns - columnOffset - emptyColumns.size()];
				for (int c = columnOffset; c < numberOfColumns; c++) {
					// skip empty columns
					if (emptyColumns.contains(c))
						continue;							

					annotationss[columnCounter] = new Annotations();
					for (Map.Entry<Integer,String> entry : annotationsMap.entrySet()) {						
						if (ANNOTATION_NAME.equals(entry.getValue())) {
							continue;
						} else {							
							Cell cell = sheet.getCell(c, rowOffset +  entry.getKey());
							annotationss[columnCounter].put(entry.getValue(), cell.getContents());							
						}						
					}
					columnCounter++;
				}	
				setAnnotations(annotationss);
			}

			@Override
			public int getNumberOfColumnsInCurrentRow() {
				return numberOfColumns - columnOffset - emptyColumns.size();
			}
			
			@Override
			public boolean isMissing(int columnIndex) {
				return cells[columnIndex].getType() == CellType.EMPTY || cells[columnIndex].getType() == CellType.ERROR || cells[columnIndex].getType() == CellType.FORMULA_ERROR || cells[columnIndex].getContents() == null || "".equals(cells[columnIndex].getContents().trim());
			}

			@Override
			public Number getNumber(int columnIndex) {
				try {
					return Double.valueOf(((NumberCell) cells[columnIndex]).getValue());
				} catch (ClassCastException e) {
				}
				return null;
			}
			
			@Override
			public Date getDate(int columnIndex) {
				try {
					return ((DateCell) cells[columnIndex]).getDate();
				} catch (ClassCastException e) {
				}
				return null;
			}

			@Override
			public String getString(int columnIndex) {
				return cells[columnIndex].getContents();
			}

			@Override
			public boolean next() {
				while (emptyRows.contains(currentRow) && currentRow < numberOfRows) {
					currentRow++;
				}
				if (currentRow >= numberOfRows) {
					return false;
				}
				cells = new Cell[numberOfColumns - columnOffset - emptyColumns.size()];
				int columnCounter = 0;;
				for (int c = columnOffset; c < numberOfColumns; c++) {
					if (emptyColumns.contains(c)) {
						continue;
					}
					cells[columnCounter] = sheet.getCell(c, currentRow);
					columnCounter++;
				}
				currentRow++;
				return true;
			}

			@Override
			public void close() throws OperatorException {
				workbook.close();	
			}
		};
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_EXCEL_FILE, "The Excel spreadsheet file which should be loaded.", "xls", false));
		types.add(new ParameterTypeInt(PARAMETER_SHEET_NUMBER, "The number of the sheet which should be imported.", 1, Integer.MAX_VALUE, 1, false));
		types.add(new ParameterTypeInt(PARAMETER_ROW_OFFSET, "The number of rows to skip at top of sheet as they contain no usable data.", 0, 65535, 0, false));
		types.add(new ParameterTypeInt(PARAMETER_COLUMN_OFFSET, "The number of columns to skip at left side of sheet as they contain no usable data.", 0, 255, 0, false));
		types.add(new ParameterTypeBoolean(PARAMETER_FIRST_ROW_AS_NAMES, "Indicates if the first row should be used for the attribute names.", true, false));
		
		List<String> annotations = new LinkedList<String>();
		annotations.add(ANNOTATION_NAME);
		annotations.addAll(Arrays.asList(Annotations.ALL_KEYS_ATTRIBUTE));
		types.add(new ParameterTypeList(PARAMETER_ANNOTATIONS, "Maps row numbers to annotation names.", 
				new ParameterTypeInt("row_number", "Row number which contains an annotation", 0, Integer.MAX_VALUE),
				new ParameterTypeCategory("annotation", 
						"Name of the annotation to assign this row.",
						annotations.toArray(new String[annotations.size()]), 0)));
				
//		types.addAll(StrictDecimalFormat.getParameterTypes(this));
//		types.add(new ParameterTypeBoolean(PARAMETER_CREATE_LABEL, "Indicates if the sheet has a label column.", false, false));
//
//		ParameterType type = new ParameterTypeInt(PARAMETER_LABEL_COLUMN, "Indicates which column should be used for the label attribute", 1, Integer.MAX_VALUE, 1, false);
//		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_CREATE_LABEL, false, true));
//		types.add(type);
//
//		types.add(new ParameterTypeBoolean(PARAMETER_CREATE_ID, "Indicates if sheet has a id column.", false, false));
//
//		type = new ParameterTypeInt(PARAMETER_ID_COLUMN, "Indicates which column should be used for the Id attribute", 1, Integer.MAX_VALUE, 1, false);
//		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_CREATE_ID, false, true));
//		types.add(type);
//
//		types.add(new ParameterTypeString(PARAMETER_DECIMAL_POINT_CHARACTER, "Character that is used as decimal point.", "."));
//		types.add(new ParameterTypeCategory(PARAMETER_DATAMANAGEMENT, "Determines, how the data is represented internally.", DataRowFactory.TYPE_NAMES, DataRowFactory.TYPE_DOUBLE_ARRAY));

		return types;
	}
}
