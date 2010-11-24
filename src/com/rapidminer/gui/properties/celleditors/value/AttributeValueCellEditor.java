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

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeAttribute;

/**
 * Value cell editor for attribute type.
 * 
 * @author Sebastian Land
 */
public class AttributeValueCellEditor extends DefaultCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -1889899793777695100L;
	
	public AttributeValueCellEditor(ParameterTypeAttribute type) {
		super(new AttributeComboBox(type));	
		((JComboBox) editorComponent).removeItemListener(this.delegate);
		((JComboBox) editorComponent).setEditable(true);
		final JTextComponent textField = (JTextComponent) ((JComboBox) editorComponent).getEditor().getEditorComponent();
		
		this.delegate = new EditorDelegate() {
			private static final long serialVersionUID = -5592150438626222295L;

			@Override
			public void setValue(Object x) {
				if (x == null) {
					super.setValue(null);
					((JComboBox) editorComponent).setSelectedItem(null);
				} else {
					String value = x.toString();
					super.setValue(x);
					((JComboBox) editorComponent).setSelectedItem(value);
					if (value != null) {
						textField.setText(value.toString());
					} else {
						textField.setText("");
					}
				}
				
//				if (!((JComboBox) editorComponent).getEditor().getEditorComponent().hasFocus()) {
//					((JComboBox) editorComponent).getEditor().setItem(value);
//				}				
			}

			@Override
			public Object getCellEditorValue() {
				String selected = textField.getText();
//				String selected = (String) ((JComboBox) editorComponent).getSelectedItem();
				if ((selected != null) && (selected.trim().length() == 0)) {
					selected = null;
				}
				return selected;
			}
		};
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void setOperator(Operator operator) {

	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		return getTableCellEditorComponent(table, value, isSelected, row, column);
	}
	
	
}
