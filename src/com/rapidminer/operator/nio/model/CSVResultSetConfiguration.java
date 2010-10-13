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

import java.io.File;
import java.nio.charset.Charset;

import javax.swing.table.TableModel;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.CSVExampleSource;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.io.Encoding;

/**
 * A class holding information about configuration of the Excel Result Set
 * 
 * @author Sebastian Land
 */
public class CSVResultSetConfiguration implements DataResultSetFactory {

	private File csvFile;

	private boolean skipComments;
	private boolean useQuotes;
	private boolean firstRowAsAttributeNames;
	private boolean trimLines;
	private String columnSeparators;

	private char quoteCharacter;
	private char escapeCharacter;
	private String commentCharacters;

	private Charset encoding;
	
	/**
	 * This will create a completely empty result set configuration
	 */
	public CSVResultSetConfiguration() {
	}

	/**
	 * This constructor reads all settings from the parameters of the given operator.
	 */
	public CSVResultSetConfiguration(CSVExampleSource csvExampleSource) throws OperatorException {
		if (csvExampleSource.isParameterSet(CSVExampleSource.PARAMETER_CSV_FILE)) {
			setCsvFile(csvExampleSource.getParameterAsFile(CSVExampleSource.PARAMETER_CSV_FILE));
		}
		setSkipComments(csvExampleSource.getParameterAsBoolean(CSVExampleSource.PARAMETER_SKIP_COMMENTS));
		setUseQuotes(csvExampleSource.getParameterAsBoolean(CSVExampleSource.PARAMETER_USE_QUOTES));
		setFirstRowAsAttributeNames(csvExampleSource.getParameterAsBoolean(CSVExampleSource.PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES));
		setTrimLines(csvExampleSource.getParameterAsBoolean(CSVExampleSource.PARAMETER_TRIM_LINES));
		if (csvExampleSource.isParameterSet(CSVExampleSource.PARAMETER_COLUMN_SEPARATORS)) {
			setColumnSeparators(csvExampleSource.getParameterAsString(CSVExampleSource.PARAMETER_COLUMN_SEPARATORS));
		}
		if (csvExampleSource.isParameterSet(CSVExampleSource.PARAMETER_ESCAPE_CHARACTER)) {
			setEscapeCharacter(csvExampleSource.getParameterAsChar(CSVExampleSource.PARAMETER_ESCAPE_CHARACTER));
		}
		if (csvExampleSource.isParameterSet(CSVExampleSource.PARAMETER_COMMENT_CHARS)) {
			setCommentCharacters(csvExampleSource.getParameterAsString(CSVExampleSource.PARAMETER_COMMENT_CHARS));
		}
		if (csvExampleSource.isParameterSet(CSVExampleSource.PARAMETER_QUOTES_CHARACTER)) {
			setQuoteCharacter(csvExampleSource.getParameterAsChar(CSVExampleSource.PARAMETER_QUOTES_CHARACTER));
		}
		encoding = Encoding.getEncoding(csvExampleSource);		
	}

	public void setParameters(CSVExampleSource source) {
		source.setParameter(CSVExampleSource.PARAMETER_CSV_FILE, getCsvFile().getAbsolutePath());
		source.setParameter(CSVExampleSource.PARAMETER_SKIP_COMMENTS, String.valueOf(isSkipComments()));
		source.setParameter(CSVExampleSource.PARAMETER_USE_QUOTES, String.valueOf(isUseQuotes()));
		source.setParameter(CSVExampleSource.PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES, String.valueOf(isFirstRowAsAttributeNames()));
		source.setParameter(CSVExampleSource.PARAMETER_COLUMN_SEPARATORS, getColumnSeparators());
		source.setParameter(CSVExampleSource.PARAMETER_TRIM_LINES, String.valueOf(isTrimLines()));
		source.setParameter(CSVExampleSource.PARAMETER_QUOTES_CHARACTER, String.valueOf(getQuoteCharacter()));
		source.setParameter(CSVExampleSource.PARAMETER_ESCAPE_CHARACTER, String.valueOf(getEscapeCharacter()));
		source.setParameter(CSVExampleSource.PARAMETER_COMMENT_CHARS, getCommentCharacters());
		// TODO: Set encoding
	}

	@Override
	public DataResultSet makeDataResultSet(Operator operator) throws OperatorException  {
		return new CSVResultSet(this, operator);
	}

	@Override
	public TableModel makePreviewTableModel(ProgressListener listener) throws OperatorException, ParseException {
		return new DefaultPreview(makeDataResultSet(null), listener);
	}

	public void setCsvFile(File csvFile) {
		this.csvFile = csvFile;
	}

	public File getCsvFile() {
		return csvFile;
	}

	public void setFirstRowAsAttributeNames(boolean firstRowAsAttributeNames) {
		this.firstRowAsAttributeNames = firstRowAsAttributeNames;
	}

	public boolean isFirstRowAsAttributeNames() {
		return firstRowAsAttributeNames;
	}

	public void setUseQuotes(boolean useQuotes) {
		this.useQuotes = useQuotes;
	}

	public boolean isUseQuotes() {
		return useQuotes;
	}

	public void setSkipComments(boolean skipComments) {
		this.skipComments = skipComments;
	}

	public boolean isSkipComments() {
		return skipComments;
	}

	public void setColumnSeparators(String columnSeparators) {
		this.columnSeparators = columnSeparators;
	}

	public String getColumnSeparators() {
		return columnSeparators;
	}

	public void setCommentCharacters(String commentCharacters) {
		this.commentCharacters = commentCharacters;
	}

	public String getCommentCharacters() {
		return commentCharacters;
	}

	public void setEscapeCharacter(char escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

	public char getEscapeCharacter() {
		return escapeCharacter;
	}

	public void setQuoteCharacter(char quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
	}

	public char getQuoteCharacter() {
		return quoteCharacter;
	}

	public void setTrimLines(boolean trimLines) {
		this.trimLines = trimLines;
	}

	public boolean isTrimLines() {
		return trimLines;
	}

	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	public Charset getEncoding() {
		return encoding;
	}

	@Override
	public String getResourceName() {
		return getCsvFile().getAbsolutePath();
	}	
}
