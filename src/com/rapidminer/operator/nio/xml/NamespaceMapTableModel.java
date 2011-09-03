package com.rapidminer.operator.nio.xml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import com.rapidminer.tools.I18N;

/**
 * A model for the mapping of ids to namespaces.
 * 
 * Works on a copy of the initial data, not on a reference.
 * 
 * @author Marius Helf
 */
public class NamespaceMapTableModel extends AbstractTableModel {
	public static final int ID_COLUMN = 0;
	public static final int NAMESPACE_COLUMN = 1;
	
	private static final long serialVersionUID = 1L;

	private Vector<Vector<String>> tableData;

	/**
	 * Creates a new NamespaceMapTableModel and initializes its data:
	 * namespaceMap is a map of already set mappings, namespaces is a list
	 * of all namespaces to which an id could be assigned.
	 * 
	 * This function initializes the data first from idNamespaceMap and then adds
	 * rows with empty id column for all namespaces in namespaces for which no entry
	 * exists in idNamespaceMap.
	 */
	public NamespaceMapTableModel(Map<String,String> idNamespaceMap, String[] namespaces) {
		if (idNamespaceMap != null && namespaces != null) {
			initializeData(idNamespaceMap, namespaces);
		} else {
			tableData = new Vector<Vector<String>>();
		}
	}


	public void initializeData(Map<String, String> idNamespaceMap, String[] namespaces) {
		// init table
		this.tableData = new Vector<Vector<String>>(namespaces.length);
		
		// invert namespaceMap
		Map<String,String> namespaceIdMap = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : idNamespaceMap.entrySet()) {
			namespaceIdMap.put(entry.getValue(), entry.getKey());
		}
		
		// add all namespaces with their id to table model, in alphabetical order.
		Arrays.sort(namespaces);
		for (String namespace : namespaces) {
			addRow(namespaceIdMap.get(namespace), namespace);
		}
		fireTableDataChanged();
	}
	
	public Map<String, String> getIdNamespaceMap() {
		Map<String, String> idNamespaceMap = new LinkedHashMap<String, String>();
		for (Vector<String> row : tableData) {
			if (row.get(ID_COLUMN) != null && !row.get(ID_COLUMN).isEmpty()) {
			idNamespaceMap.put(row.get(ID_COLUMN), row.get(NAMESPACE_COLUMN));
			}
		}
		return idNamespaceMap;
	}

	
	/**
	 * appends a row to the table
	 */
	private void addRow(String id, String namespace) {
		Vector<String> rowVector = new Vector<String>(2);
		rowVector.add(id);
		rowVector.add(namespace);
		tableData.add(rowVector);
		fireTableRowsInserted(getRowCount()-1, getRowCount()-1);
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public int getRowCount() {
		return tableData.size();
	}

	@Override
	public String getColumnName(int column) {
		switch(column) {
		case ID_COLUMN:
			return I18N.getGUILabel("importwizard.xml.namespace_mapping.namespace_table.id_column");
		case NAMESPACE_COLUMN:
			return I18N.getGUILabel("importwizard.xml.namespace_mapping.namespace_table.namespace_column");
		default:
			return null;
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return tableData.get(rowIndex).get(columnIndex);
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == ID_COLUMN) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Updates the table model.
	 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
	 */
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		String value = (String)aValue;
		tableData.get(rowIndex).set(columnIndex, value);
		fireTableCellUpdated(rowIndex, columnIndex);
	}
}
