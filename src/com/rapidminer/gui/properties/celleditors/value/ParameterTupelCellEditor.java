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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.AbstractCellEditor;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeTupel;
/**
 * 
 * @author Simon Fischer
 */
public class ParameterTupelCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {
	private static final long serialVersionUID = -2387465714767785072L;

	private JPanel panel;

	private ParameterType[] types;
	private PropertyValueCellEditor[] editors;

	private Operator operator;

	public ParameterTupelCellEditor(ParameterTypeTupel type) {
		types = type.getParameterTypes();
	}

	@Override
	public Object getCellEditorValue() {
		String[] values = new String[editors.length];
		for (int i = 0; i < editors.length; i++) {
			if (editors[i].getCellEditorValue() != null)
				values[i] = editors[i].getCellEditorValue().toString();
		}
		return ParameterTypeTupel.transformTupel2String(values);
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		String[] tupel;
		if (value instanceof String) {
			tupel = ParameterTypeTupel.transformString2Tupel((String)value);
		} else {
			tupel = (String[]) value;
		}
		if (panel == null) {
			constructPanel(tupel);
		}
		for (int i = 0; i < editors.length; i++) {
			editors[i].getTableCellEditorComponent(null, tupel[i], false, 0, 0);
		}		
		return panel;
	}

	private void constructPanel(String[] values) {
		// constructing editors
		editors = new PropertyValueCellEditor[types.length];
		for (int i = 0; i < types.length; i++) {
			editors[i] = PropertyPanel.instantiateValueCellEditor(types[i], operator);
			editors[i].addCellEditorListener(new CellEditorListener() {
				@Override
				public void editingCanceled(ChangeEvent e) {
					fireEditingCanceled();
				}

				@Override
				public void editingStopped(ChangeEvent e) {
					fireEditingStopped();//					
				}
			});
		}

		// building panel
		panel = new JPanel(); 
		panel.setLayout(new GridLayout(1,editors.length));
		for (int i = 0; i < types.length; i++) {
			Component editorComponent = editors[i].getTableCellEditorComponent(null, values[i], false, 0, 0);
			panel.add(editorComponent);
		}
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		String[] tupel;
		if (value instanceof String) {
			tupel = ParameterTypeTupel.transformString2Tupel((String)value);
		} else {
			tupel = (String[]) value;
		}
		if (panel == null) {
			constructPanel(tupel);
		}
		for (int i = 0; i < editors.length; i++) {
			editors[i].getTableCellEditorComponent(null, tupel[i], false, 0, 0);
		}		
		return panel;
	}
}
