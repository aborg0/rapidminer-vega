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

import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.io.AbstractReader;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;

/**
 * 
 * <p>
 * This operator can be used to load data from Microsoft Excel spreadsheets. This operator is able to reads data from
 * Excel 95, 97, 2000, XP, and 2003. The user has to define which of the spreadsheets in the workbook should be used as
 * data table. The table must have a format so that each line is an example and each column represents an attribute.
 * Please note that the first line might be used for attribute names which can be indicated by a parameter.
 * </p>
 * 
 * <p>
 * The data table can be placed anywhere on the sheet and is allowed to contain arbitrary formatting instructions, empty
 * rows, and empty columns. Missing data values are indicated by empty cells or by cells containing only &quot;?&quot;.
 * </p>
 * 
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Loh, Sebastian Land
 */
public class ExcelExampleSource extends AbstractDataResultSetReader {
	
	public static final OperatorVersion CHANGE_5_0_11_NAME_SCHEMA = new OperatorVersion(5, 0, 11);
	
	/**
	 * The parameter name for &quot;The Excel spreadsheet file which should be loaded.&quot;
	 */
	public static final String PARAMETER_EXCEL_FILE = "excel_file";

	/**
	 * The parameter name for &quot;The number of the sheet which should be imported.&quot;
	 */
	public static final String PARAMETER_SHEET_NUMBER = "sheet_number";

	/**
	 * The parameter name for &quot;Indicates which column should be used for the label attribute (0: no label)&quot;
	 */
	public static final String PARAMETER_LABEL_COLUMN = "label_column";

	/**
	 * The parameter name for &quot;Indicates which column should be used for the Id attribute (0: no id)&quot;
	 */
	public static final String PARAMETER_ID_COLUMN = "id_column";

	public static final String PARAMETER_COLUMN_OFFSET = "column_offset";

	public static final String PARAMETER_ROW_OFFSET = "row_offset";

	public static final String PARAMETER_CREATE_LABEL = "create_label";

	public static final String PARAMETER_CREATE_ID = "create_id";

	public static final String PARAMETER_IMPORTED_CELL_RANGE = "imported_cell_range";

	static {
		AbstractReader.registerReaderDescription(new ReaderDescription("xls", ExcelExampleSource.class, PARAMETER_EXCEL_FILE));
	}	
	
	public ExcelExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	protected DataResultSetFactory getDataResultSetFactory() throws OperatorException {
		return new ExcelResultSetConfiguration(this);
	}

	@Override
	protected NumberFormat getNumberFormat() throws OperatorException {
		return null;
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeConfiguration(ExcelExampleSourceConfigurationWizardCreator.class, this);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeFile(PARAMETER_EXCEL_FILE, "Name of the excel file to read the data from.", "xls", false));
		types.add(new ParameterTypeInt(PARAMETER_SHEET_NUMBER, "The number of the sheet which should be imported.", 1, Integer.MAX_VALUE, 1, false));
		types.add(new ParameterTypeInt(PARAMETER_ROW_OFFSET, "The number of rows to skip at top of sheet as they contain no usable data.", 0, 65535, 0, true));
		types.add(new ParameterTypeInt(PARAMETER_COLUMN_OFFSET, "The number of columns to skip at left side of sheet as they contain no usable data.", 0, 255, 0, true));
		types.add(new ParameterTypeString(PARAMETER_IMPORTED_CELL_RANGE, "Cells to import, in Excel notation, e.g.B2:D25.", true));
		
		types.addAll(super.getParameterTypes());
		return types;
	}
	
	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] {new OperatorVersion(5,0,4), CHANGE_5_0_11_NAME_SCHEMA};
	}
}
