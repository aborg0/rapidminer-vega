package com.rapidminer.gui.properties;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JTable;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeList;

/** Parameter table for list and enumeration types.
 * 
 * @author Simon Fischer
 *
 */
public class ListPropertyTable2 extends JTable {

	private static final long serialVersionUID = 1L;

	public ListPropertyTable2(ParameterTypeList type, List<String[]> parameterList, Operator operator) {
		this(new ParameterType[] { type.getKeyType(), type.getValueType() }, parameterList, operator);		 
	}
	
	public ListPropertyTable2(ParameterTypeEnumeration type, List<String> parameterList, Operator operator) {
		this(new ParameterType[] { type.getValueType() }, to2DimList(parameterList), operator);		
	}
	
	private ListPropertyTable2(ParameterType[] types, List<String[]> parameterList, Operator operator) {
		setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		getTableHeader().setReorderingAllowed(false);
		setRowSelectionAllowed(true);
		setColumnSelectionAllowed(false);
		setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
		setModel(new ListTableModel(types, parameterList));

		for (int i = 0; i < types.length; i++) {
			getColumnModel().getColumn(i).setCellEditor(PropertyPanel.instantiateValueCellEditor(types[i], operator));
			getColumnModel().getColumn(i).setCellRenderer(PropertyPanel.instantiateValueCellEditor(types[i], operator));
		}
	}

	private static List<String[]> to2DimList(List<String> parameterList) {
		List<String[]> result = new LinkedList<String[]>();
		for (String v : parameterList) {
			result.add(new String[] { v });
		}
		return result;
	}

	public void addRow() {
		((ListTableModel) getModel()).addRow();
	}

	public void removeSelected() {
		if (getSelectedRow() != -1) {
			((ListTableModel) getModel()).removeRow(getSelectedRow());
		}
	}

	public void storeParameterList(List<String[]> parameterList2) {
		parameterList2.clear();		
		parameterList2.addAll(((ListTableModel) getModel()).getParameterList());
	}
	
	public void storeParameterEnumeration(List<String> parameterList2) {
		parameterList2.clear();		
		for (String[] values : ((ListTableModel) getModel()).getParameterList()) {
			parameterList2.add(values[0]);
		}		
	}

}
