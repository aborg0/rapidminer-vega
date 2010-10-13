package com.rapidminer.operator.nio;

import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * Wraps around another table model and adds another column at position 0 for editing
 * annotations. The table should use an {@link AnnotationCellEditor} as an editor in
 * column 0.
 * 
 * @author Simon Fischer
 * 
 */
public class AnnotationTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private TableModel wrappedModel;
	private Map<Integer, String> annotationsMap;

	public AnnotationTableModel(TableModel wrappedModel, Map<Integer,String> annotationsMap) {
		this.annotationsMap = annotationsMap;
		this.wrappedModel = wrappedModel;
		wrappedModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				TableModelEvent translated;
				translated = new TableModelEvent(AnnotationTableModel.this, e.getFirstRow(), e.getLastRow(), e.getColumn()+1, e.getType());
				fireTableChanged(translated);				
			}
		});
	}
	
	@Override
	public String getColumnName(int column) {
		if (column == 0) {
			return "Annotation";
		} else {
			return wrappedModel.getColumnName(column - 1);
		}
	}
	
	@Override
	public int getRowCount() {
		return wrappedModel.getRowCount();
	}

	@Override
	public int getColumnCount() {
		return wrappedModel.getColumnCount() + 1;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			annotationsMap.put(rowIndex, (String) aValue);
		} else {
			wrappedModel.setValueAt(aValue, rowIndex, columnIndex-1);
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			String annotation = annotationsMap.get(rowIndex);
			if (annotation == null) {
				annotation = AnnotationCellEditor.NONE;
			}
			return annotation;
		} else {
			return wrappedModel.getValueAt(rowIndex, columnIndex - 1);
		}
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 0;
	}
}
