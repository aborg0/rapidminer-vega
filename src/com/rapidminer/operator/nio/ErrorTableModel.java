package com.rapidminer.operator.nio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.rapidminer.operator.nio.model.ParsingError;

/** A table model to display {@link ParsingError}s.
 * 
 * @author Simon Fischer
 *
 */
public class ErrorTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	
	private List<ParsingError> errors = new ArrayList<ParsingError>();

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0:
			return "Row/Column";
		case 1:
			return "Error";
		case 2:
			return "Original value";
		case 3:
			return "Message";
		default:
			return super.getColumnName(column);
		}	
	}
	
	@Override
	public int getRowCount() {
		return errors.size();
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		ParsingError error = errors.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return (error.getRow()+1) + "/" + (error.getColumn()+1);
		case 1:
			return error.getErrorCode();
		case 2:
			return error.getOriginalValue();
		case 3:
			return error.getCause() != null ? error.getCause().getMessage() : null;
		default:
			return null;
		}		
	}
	
	public void setErrors(Collection<ParsingError> errors) {
		this.errors.clear();
		this.errors.addAll(errors);
		fireTableStructureChanged();
	}
}
