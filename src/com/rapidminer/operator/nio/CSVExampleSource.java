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
import com.rapidminer.operator.io.AbstractReader;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeChar;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.DateParser;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.StrictDecimalFormat;
import com.rapidminer.tools.io.Encoding;

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
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Loh, Sebastian Land, Simon Fischer
 */
public class CSVExampleSource extends AbstractDataResultSetReader {
	
	public static final String PARAMETER_CSV_FILE = "csv_file";
	public static final String PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES = "use_first_row_as_attribute_names";
	public static final String PARAMETER_TRIM_LINES = "trim_lines";
	public static final String PARAMETER_SKIP_COMMENTS = "skip_comments";
	public static final String PARAMETER_COMMENT_CHARS = "comment_characters";
	public static final String PARAMETER_USE_QUOTES = "use_quotes";
	public static final String PARAMETER_QUOTES_CHARACTER = "quotes_character";
	public static final String PARAMETER_COLUMN_SEPARATORS = "column_separators";
	public static final String PARAMETER_ESCAPE_CHARACTER = "escape_character_for_quotes";

	static {
		AbstractReader.registerReaderDescription(new ReaderDescription("csv", CSVExampleSource.class, PARAMETER_CSV_FILE));
	}
	
	
	public CSVExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	protected DataResultSetFactory getDataResultSetFactory() throws OperatorException {
		return new CSVResultSetConfiguration(this);
	}

	@Override
	protected NumberFormat getNumberFormat() throws OperatorException {
		return StrictDecimalFormat.getInstance(this, true);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		
		ParameterType type = new ParameterTypeConfiguration(CSVExampleSourceConfigurationWizardCreator.class, this);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeFile(PARAMETER_CSV_FILE, "Name of the file to read the data from.", "csv", false));

		types.addAll(Encoding.getParameterTypes(this));
		types.add(new ParameterTypeBoolean(PARAMETER_TRIM_LINES,
				"Indicates if lines should be trimmed (empty spaces are removed at the beginning and the end) before the column split is performed. This option might be problematic if TABs are used as a seperator.",
				false));
		types.add(new ParameterTypeBoolean(PARAMETER_SKIP_COMMENTS, "Indicates if a comment character should be used.", true));
		type = new ParameterTypeString(PARAMETER_COMMENT_CHARS, "Lines beginning with these characters are ignored.", "#", true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SKIP_COMMENTS, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES,
				"Read attribute names from file (assumes the attribute names are in the first line of the file).", true);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_USE_QUOTES, "Indicates if quotes should be regarded.", true));
		type = new ParameterTypeChar(PARAMETER_QUOTES_CHARACTER, "The quotes character.", LineParser.DEFAULT_QUOTE_CHARACTER, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_QUOTES, false, true));
		types.add(type);
		type = new ParameterTypeChar(PARAMETER_ESCAPE_CHARACTER, "The charcter that is used to escape quotes", LineParser.DEFAULT_QUOTE_ESCAPE_CHARACTER, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_QUOTES, false, true));
		types.add(type);
		types.add(new ParameterTypeString(PARAMETER_COLUMN_SEPARATORS, "Column separators for data files (regular expression)", ";"));
		
		types.addAll(StrictDecimalFormat.getParameterTypes(this, true));
		types.addAll(DateParser.getParameterTypes(this));
		
		types.addAll(super.getParameterTypes());
		return types;
	}
}
