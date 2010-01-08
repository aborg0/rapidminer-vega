/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
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
import java.io.InputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv.LineReader;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeChar;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.DateParser;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.StrictDecimalFormat;
import com.rapidminer.tools.io.Encoding;


/**
 * A reader for CSV files.
 *  
 * @author Tobias Malbrecht
 */
public class CSVDataReader extends AbstractDataReader {

	public static final String PARAMETER_FILE_NAME = "file_name";
	
	static {
		AbstractReader.registerReaderDescription(new ReaderDescription("csv", CSVDataReader.class, PARAMETER_FILE_NAME));
	}

	public static final String PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES = "use_first_row_as_attribute_names";

	public static final String PARAMETER_TRIM_LINES = "trim_lines";
	
	public static final String PARAMETER_SKIP_COMMENTS = "skip_comments";

	public static final String PARAMETER_COMMENT_CHARS = "comment_characters";

	public static final String PARAMETER_USE_QUOTES = "use_quotes";
	
	public static final String PARAMETER_QUOTES_CHARACTER = "quotes_character";

	public static final String PARAMETER_COLUMN_SEPARATORS = "column_separators";
	
	public CSVDataReader(OperatorDescription description) {
		super(description);
	}

	@Override
	protected String getGenericColumnName(int column) throws OperatorException {
		return getParameterAsFile(PARAMETER_FILE_NAME).getName() + "_" + (column + 1);
	}

	@Override
	protected DataSet getDataSet() throws OperatorException {
		return new DataSet() {
			private boolean first = getParameterAsBoolean(PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES);
			
			private LineReader reader = null;
			{
				InputStream stream = null;
				try {
					stream = getParameterAsInputStream(PARAMETER_FILE_NAME);
				} catch (IOException e) {
					throw new UserError(CSVDataReader.this, 302, stream, e.getMessage());
				}
				reader = new LineReader(stream, Encoding.getEncoding(CSVDataReader.this));
			}
			
			private final LineParser parser = new LineParser();
			{
				parser.setTrimLine(getParameterAsBoolean(PARAMETER_TRIM_LINES));
				parser.setSkipComments(getParameterAsBoolean(PARAMETER_SKIP_COMMENTS));
				parser.setSplitExpression(getParameterAsString(PARAMETER_COLUMN_SEPARATORS));
				parser.setUseQuotes(getParameterAsBoolean(PARAMETER_USE_QUOTES));
				parser.setQuoteCharacter(getParameterAsChar(PARAMETER_QUOTES_CHARACTER));
				parser.setCommentCharacters(getParameterAsString(PARAMETER_COMMENT_CHARS));
			}
			
			private final NumberFormat numberFormat = StrictDecimalFormat.getInstance(CSVDataReader.this);
			
			private final DateFormat   dateFormat   = DateParser.getInstance(CSVDataReader.this);
			
			private String[] parsedLine = null;
			
			@Override
			public boolean next() {
				String line = null;
				try {
					if (first) {
						do {
							line = reader.readLine();
							if (line == null) {
								return false;
							}
							parsedLine = parser.parse(line); 
						} while (parsedLine == null);
						setColumnNames(parsedLine);
						first = false;
					}
					do { 
						line = reader.readLine();
						if (line == null) {
							return false;
						}
						parsedLine = parser.parse(line); 
					} while (parsedLine == null);
					return true;
				} catch (IOException e) {
					return false;
				}
			}
			
			@Override
			public int getNumberOfColumnsInCurrentRow() {
				return parsedLine.length;
			}

			@Override
			public boolean isMissing(int columnIndex) {
				return parsedLine[columnIndex] == null || parsedLine[columnIndex].isEmpty();
			}

			@Override
			public Number getNumber(int columnIndex) {
				try {
					return numberFormat.parse(parsedLine[columnIndex]);
				} catch (ParseException e) {
				}
				return null; 
			}

			@Override
			public String getString(int columnIndex) {
				return parsedLine[columnIndex];
			}

			@Override
			public Date getDate(int columnIndex) {
				try {
					return dateFormat.parse(parsedLine[columnIndex]);					
				} catch (ParseException e) {
				}
				return null;
			}
			
			@Override
			public void close() throws OperatorException {
				try {
					reader.close();
				} catch (IOException e) {
					
				}
			}
		};
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_FILE_NAME, "Name of the file to read the data from.", "csv", false));
		types.addAll(Encoding.getParameterTypes(this));
		types.add(new ParameterTypeBoolean(PARAMETER_TRIM_LINES, "Indicates if lines should be trimmed (empty spaces are removed at the beginning and the end) before the column split is performed.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_SKIP_COMMENTS, "Indicates if qa comment character should be used.", true));
		ParameterType type = new ParameterTypeString(PARAMETER_COMMENT_CHARS, "Lines beginning with these characters are ignored.", "#", true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SKIP_COMMENTS, false, true));
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES, "Read attribute names from file (assumes the attribute names are in the first line of the file).", true);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_USE_QUOTES, "Indicates if quotes should be regarded (slower!).", true));
		type = new ParameterTypeChar(PARAMETER_QUOTES_CHARACTER, "The quotes character.", '"', true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_QUOTES, false, true));
		types.add(type);
		types.add(new ParameterTypeString(PARAMETER_COLUMN_SEPARATORS, "Column separators for data files (regular expression)", ",\\s*|;\\s*|\\s+"));
		types.addAll(StrictDecimalFormat.getParameterTypes(this));
		types.addAll(DateParser.getParameterTypes(this));
		return types;
	}
}
