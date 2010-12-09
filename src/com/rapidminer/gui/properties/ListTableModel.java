package com.rapidminer.gui.properties;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.rapidminer.parameter.ParameterType;

/** Table model returning values from the list of string pairs.
 *  Column types are determined by {@link ParameterType}s.
 *  
 * @author Simon Fischer*/
public class ListTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private List<String[]> parameterList;
	private ParameterType[] types;
	
	public ListTableModel(ParameterType[] types, List<String[]> parameterList) {
		super();
		this.types = types;
		this.parameterList = new ArrayList<String[]>(parameterList);
	}

	@Override
	public String getColumnName(int column) {
		return types[column].getKey().replace('_', ' ');
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}

	@Override
	public int getRowCount() {
		return parameterList.size();
	}

	@Override
	public int getColumnCount() {
		return types.length;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		ParameterType columnType = types[columnIndex];
		if (aValue == null) {
			parameterList.get(rowIndex)[columnIndex] = columnType.getDefaultValueAsString();
		} else {
			parameterList.get(rowIndex)[columnIndex] = aValue.toString();
		}					
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return parameterList.get(rowIndex)[columnIndex];
	}

	void addRow() {
		String[] initialValues = new String[types.length];
		for (int i = 0; i < initialValues.length; i++) {
			initialValues[i] = types[i].getDefaultValueAsString();
		}
		parameterList.add(initialValues);
		fireTableRowsInserted(parameterList.size()-1, parameterList.size()-1);
	}

	public void removeRow(int selectedRow) {
		parameterList.remove(selectedRow);
		//fireTableStructureChanged();
		fireTableRowsDeleted(selectedRow, selectedRow);
	}

	public List<String[]> getParameterList() {
		return parameterList;
	}
	
}