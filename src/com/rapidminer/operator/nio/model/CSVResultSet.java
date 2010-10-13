package com.rapidminer.operator.nio.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;

import com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv.LineReader;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;

/**
 * 
 * @author Simon Fischer
 *
 */
public class CSVResultSet implements DataResultSet {

	private CSVResultSetConfiguration configuration;
	private LineReader reader;
	private LineParser parser;

	private String[] next;
	private String[] current;
	private int currentRow;
	private String[] columnNames;
	private int[] valueTypes;
	private int numColumns = 0;
	private Operator operator;

	public CSVResultSet(CSVResultSetConfiguration configuration, Operator operator) throws OperatorException {
		this.configuration = configuration;
		this.operator = operator;
		open();
	}

	private void open() throws OperatorException {
		close();
		InputStream in;
		try {
			in = new FileInputStream(configuration.getCsvFile());
		} catch (FileNotFoundException e) {
			throw new UserError(operator, 301, e, configuration.getCsvFile());
		}
		reader = new LineReader(in, configuration.getEncoding());
		parser = new LineParser(configuration);
		try {
			readNext();
		} catch (IOException e) {
			throw new UserError(operator, e, 321, configuration.getCsvFile(), e.toString());
		}
		columnNames = new String[next.length];
		for (int i = 0; i < next.length; i++) {
			columnNames[i] = "att"+(i+1);
		}
		valueTypes = new int[next.length];
		Arrays.fill(valueTypes, Ontology.NOMINAL); 
		currentRow = -1;		
	}

	private void readNext() throws IOException {
		String line = reader.readLine();
		if (line == null) {
			next = null;
			return;
		}
		try {			
			next = parser.parse(line);			
		} catch (IllegalArgumentException e){
			next = new String [] { line };
		}
		numColumns = Math.max(numColumns, next.length);
	}

	@Override
	public boolean hasNext() {		
		return next != null;
	}

	@Override
	public void next(ProgressListener listener) throws OperatorException {
		current = next;
		currentRow++;
		try {
			readNext();
		} catch (IOException e) {
			throw new UserError(operator, e, 321, configuration.getCsvFile(), e.toString());
		}
	}

	@Override
	public int getNumberOfColumns() {
		return numColumns;
	}

	@Override
	public String[] getColumnNames() {
		return columnNames;
	}

	@Override
	public boolean isMissing(int columnIndex) {
		return (current[columnIndex] == null) || current[columnIndex].isEmpty();
	}

	@Override
	public Number getNumber(int columnIndex) throws ParseException {
		throw new ParseException(new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL, current[columnIndex]));
	}

	@Override
	public String getString(int columnIndex) throws ParseException {
		if (columnIndex  < current.length) {
			return current[columnIndex];
		} else {
			return null;
		}
	}

	@Override
	public Date getDate(int columnIndex) throws ParseException {
		throw new ParseException(new ParsingError(currentRow, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_DATE, current[columnIndex]));
	}

	@Override
	public ValueType getNativeValueType(int columnIndex) throws ParseException {
		return ValueType.STRING;
	}

	@Override
	public void close() throws OperatorException {
		if (reader == null) {
			return;
		}
		try {
			reader.close();
		} catch (IOException e) {
			throw new UserError(operator, 321, e, configuration.getCsvFile(), e.toString());
		} finally {
			reader = null;
		}
	}

	@Override
	public void reset(ProgressListener listener) throws OperatorException {
		open();
	}

	@Override
	public int[] getValueTypes() {
		return valueTypes;
	}

	@Override
	public int getCurrentRow() {
		return currentRow;
	}
}
