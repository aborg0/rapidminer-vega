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
package com.rapidminer.operator.nio;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.EventObject;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.table.EditableHeaderJTable;
import com.rapidminer.gui.viewer.DataTableViewerTable;
import com.rapidminer.operator.OperatorException;

/**
 * This Wizard Step might be used to defined
 * the meta data of each attribute.
 * 
 * @author Sebastian Land
 */
public class MetaDataDeclarationWizardStep extends WizardStep {

	private JPanel panel = new JPanel(new BorderLayout());
	
	private AnnotationDeclarationWizardStep previousStep;
	
	private DataResultSetTranslator translator = null;
	private DataResultSetTranslationConfiguration config;
	private DataResultSet resultSet;

	private ExampleSet exampleSet;
	
	
	public MetaDataDeclarationWizardStep(AnnotationDeclarationWizardStep previousStep) {
		super("key"); // TODO
		
		this.previousStep = previousStep;
	}

	/**
	 * This method is the main passing point from system depended wizard steps to the general meta data definition
	 * steps. Here the resultSet can be set. If
	 */
	public void setDataResultSet(DataResultSet resultSet) {
		if (translator != null) {
			try {
				translator.close();
			} catch (OperatorException e) {
				// TODO: show error dialog
			}
		}
		this.resultSet = resultSet;
		translator = new DataResultSetTranslator(resultSet);
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		panel.removeAll();
		this.config = previousStep.getConfiguration();
		this.exampleSet = previousStep.getParsedExampleSet();
		
		JTable table = new DataTableViewerTable(new DataTableExampleSetAdapter(exampleSet, null), false, false, false);

		TableCellRenderer headerRenderer = new TableCellRenderer() {
			
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				return new JComboBox(new String[] {"Hurz", "Hurz"});
			}
		};
		TableCellEditor headerEditor = new TableCellEditor() {
			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
				return new JComboBox(new String[] {"Hurz", "Hurz"});
			}

			@Override
			public void addCellEditorListener(CellEditorListener l) {
			}

			@Override
			public void cancelCellEditing() {
			}

			@Override
			public Object getCellEditorValue() {
				return "Hurz2";
			}

			@Override
			public boolean isCellEditable(EventObject anEvent) {
				return true;
			}

			@Override
			public void removeCellEditorListener(CellEditorListener l) {
			}

			@Override
			public boolean shouldSelectCell(EventObject anEvent) {
				return false;
			}

			@Override
			public boolean stopCellEditing() {
				return true;
			}
		};
		EditableHeaderJTable.installEditableHeader(table, headerRenderer, headerEditor, new String[table.getColumnCount()]);
		panel.add(new ExtendedJScrollPane(table), BorderLayout.CENTER);
		return true;
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.FINISH) {
			try {
				translator.close();
			} catch (OperatorException e) {
				// TODO: Show error dialog
			}
		} 
		return true;
	}
	
	@Override
	protected boolean canGoBack() {
		return true;
	}

	@Override
	protected boolean canProceed() {
		return true;
	}

	@Override
	protected JComponent getComponent() {
		return panel;
	}

}
