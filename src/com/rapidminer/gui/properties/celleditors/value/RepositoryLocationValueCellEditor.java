/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;

/** Cell editor that allows to select a repository entry by pressing a button.
 * 
 * @author Simon Fischer
 *
 */
public class RepositoryLocationValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = 1L;

	private final JPanel panel = new JPanel();

	private final JTextField textField = new JTextField(12);

	private Operator operator;

	public RepositoryLocationValueCellEditor(ParameterTypeRepositoryLocation type) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		panel.setLayout(gridBagLayout);
		panel.setToolTipText(type.getDescription());
		textField.setToolTipText(type.getDescription());
		textField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireEditingStopped();
			}			
		});
		textField.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				fireEditingStopped();
			}			
			@Override public void focusGained(FocusEvent e) { }
		});

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.RELATIVE;
		gridBagLayout.setConstraints(textField, c);
		panel.add(textField);
		
		JButton button = new JButton(new ResourceAction(true, "repository_select_location") {
			private static final long serialVersionUID = 1L;
			{
				putValue(NAME, null);
			}
			public void actionPerformed(ActionEvent e) {
			    com.rapidminer.Process process = (RepositoryLocationValueCellEditor.this.operator != null) ? RepositoryLocationValueCellEditor.this.operator.getProcess() : null;
			    RepositoryLocation processLocation = null;
			    if (process != null) {
			    	processLocation = process.getRepositoryLocation();
			    	if (processLocation != null) {
			    		processLocation = processLocation.parent();
			    	}
			    }
				String locationName = RepositoryLocationChooser.selectLocation(processLocation, textField.getText(), panel);
//				if (locationName != null) {
//					if ((operator != null) && (operator.getProcess() != null)) {
//						try {
//							RepositoryLocation loc = new RepositoryLocation(processLocation, locationName);
//							locationName = operator.getProcess().makeRelativeRepositoryLocation(loc);
//						} catch (Exception ex) {
//							LogService.getRoot().log(Level.WARNING, "Cannot make relative process location for '"+locationName+"': "+ex, ex);
//						}
//					}
//				}
				if (locationName != null) {
					textField.setText(locationName);
				}
				fireEditingStopped();

			}
		});
		button.setMargin(new Insets(0, 0, 0, 0));
		c.gridwidth = GridBagConstraints.REMAINDER;
		panel.add(button);
	}
	
	@Override
	public Object getCellEditorValue() {
		return (textField.getText().trim().length() == 0) ? null : textField.getText().trim();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		textField.setText((value == null) ? "" : value.toString());
		return panel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		return getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void setOperator(Operator operator) { 
		this.operator = operator;
	}
}
