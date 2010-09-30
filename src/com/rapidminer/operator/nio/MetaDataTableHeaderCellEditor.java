package com.rapidminer.operator.nio;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.nio.model.ColumnMetaData;
import com.rapidminer.tools.Ontology;

/**
 * 
 * @author Simon Fischer
 *
 */
public class MetaDataTableHeaderCellEditor extends JPanel implements TableCellEditor, TableCellRenderer {

	private static final long serialVersionUID = 1L;

	private ColumnMetaData value;

	private EventListenerList cellEditorListeners = new EventListenerList(); 

	private JComboBox valueTypeBox     = new JComboBox(Ontology.ATTRIBUTE_VALUE_TYPE.getNames());
	private JCheckBox selectCheckBox = new JCheckBox();
	private JTextField nameField = new JTextField();
	private JComboBox roleBox = new JComboBox(Attributes.KNOWN_ATTRIBUTE_TYPES);

	public MetaDataTableHeaderCellEditor() {
		super(new GridLayout(4, 1));
		roleBox.setEditable(true);
		
		add(selectCheckBox);
		add(nameField);
		add(valueTypeBox);				
		add(roleBox);

		valueTypeBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (value != null) {
					value.setAttributeValueType(valueTypeBox.getSelectedIndex());
				}
			}
		});
		nameField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (value != null) {
					value.setUserDefinedAttributeName(nameField.getText());
				}
			}
		});
		selectCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (value != null) {
					value.setSelected(selectCheckBox.isSelected());
				}
			}
		});
		roleBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (value != null) {
					value.setRole(roleBox.getSelectedItem().toString());
				}
			}
		});
	}

	@Override
	public Object getCellEditorValue() {
		return value;
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean stopCellEditing() {
		if (value != null) {
			value.setUserDefinedAttributeName(nameField.getText());
		}
		fireEditingStopped();
		return true;
	}

	private void fireEditingStopped() {
		CellEditorListener[] listeners = listenerList.getListeners(CellEditorListener.class);
		ChangeEvent changeEvent = null;
		for (CellEditorListener l : listeners) {
			if (changeEvent == null) {
				changeEvent = new ChangeEvent(this);
			}
			l.editingStopped(changeEvent);
		}	       
	}

	private void fireEditingCancelled() {
		CellEditorListener[] listeners = listenerList.getListeners(CellEditorListener.class);
		ChangeEvent changeEvent = null;
		for (CellEditorListener l : listeners) {
			if (changeEvent == null) {
				changeEvent = new ChangeEvent(this);
			}
			l.editingCanceled(changeEvent);
		}	       
	}

	@Override
	public void cancelCellEditing() {
		fireEditingCancelled();
	}

	@Override
	public void addCellEditorListener(CellEditorListener l) {
		cellEditorListeners.add(CellEditorListener.class, l);
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		cellEditorListeners.remove(CellEditorListener.class, l);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		setMetaData((ColumnMetaData) value);
		return this;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		setMetaData((ColumnMetaData) value);
		return this;
	}

	private void setMetaData(ColumnMetaData value) {
		this.value = value;
		valueTypeBox.setSelectedIndex(value.getAttributeValueType());
		selectCheckBox.setSelected(value.isSelected());
		nameField.setText(value.getUserDefinedAttributeName());
		roleBox.setSelectedItem(value.getRole());
	}

}
