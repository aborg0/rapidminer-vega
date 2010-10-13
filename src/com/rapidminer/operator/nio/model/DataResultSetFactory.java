package com.rapidminer.operator.nio.model;

import javax.swing.table.TableModel;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.ProgressListener;

/**
 * 
 * @author Simon Fischer
 *
 */
public interface DataResultSetFactory {

	// TODO: Add progress listener
	public DataResultSet makeDataResultSet(Operator operator) throws OperatorException;

	public TableModel makePreviewTableModel(ProgressListener listener) throws OperatorException, ParseException;

	/** Returns the human readable name of the resource read (most often, this will be a file or URL). */
	public String getResourceName();

}
