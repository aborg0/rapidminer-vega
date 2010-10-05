package com.rapidminer.operator.nio.model;

import javax.swing.table.TableModel;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;

/**
 * 
 * @author Simon Fischer
 *
 */
public interface DataResultSetFactory {

	public DataResultSet makeDataResultSet(Operator operator) throws OperatorException;

	public TableModel makePreviewTableModel() throws OperatorException;

}
