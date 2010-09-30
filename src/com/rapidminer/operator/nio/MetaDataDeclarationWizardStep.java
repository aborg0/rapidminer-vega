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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.table.EditableTableHeader;
import com.rapidminer.gui.tools.table.EditableTableHeaderColumn;
import com.rapidminer.gui.viewer.DataTableViewerTableModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.ColumnMetaData;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.DataResultSetTranslationConfiguration;
import com.rapidminer.operator.nio.model.DataResultSetTranslator;
import com.rapidminer.operator.nio.model.WizardState;

/**
 * This Wizard Step might be used to defined
 * the meta data of each attribute.
 * 
 * @author Sebastian Land, Simon Fischer
 */
public class MetaDataDeclarationWizardStep extends WizardStep {

	/** Publicly exposes the method {@link #configurePropertiesFromAction(Action)} public. */
	private class ReconfigurableButton extends JButton {
		private static final long serialVersionUID = 1L;
		private ReconfigurableButton(Action action) {
			super(action);
		}
		@Override
		public void configurePropertiesFromAction(Action a) {
			super.configurePropertiesFromAction(a);
		}
	}
	private Action reloadAction = new ResourceAction("wizard.validate_value_types") {
		private static final long serialVersionUID = 1L;
		@Override public void actionPerformed(ActionEvent e) { toggleReload(); }		
	};
	private Action cancelReloadAction = new ResourceAction("wizard.abort_validate_value_types") {
		private static final long serialVersionUID = 1L;
		@Override public void actionPerformed(ActionEvent e) { toggleReload(); }		
	};
	private ReconfigurableButton reloadButton = new ReconfigurableButton(reloadAction);

	private Action guessValueTypes = new ResourceAction("wizard.guess_value_types") {
		private static final long serialVersionUID = 1L;
		@Override public void actionPerformed(ActionEvent e) { toggleGuessValueTypes(); }		
	};
	private Action cancelGuessValueTypes = new ResourceAction("wizard.abort_guess_value_types") {
		private static final long serialVersionUID = 1L;
		@Override public void actionPerformed(ActionEvent e) { toggleGuessValueTypes(); }		
	};
	private ReconfigurableButton guessButton = new ReconfigurableButton(guessValueTypes);

	private JCheckBox errorsAsMissingBox = new JCheckBox(new ResourceActionAdapter("wizard.error_tolerant"));
	private JCheckBox filterErrorsBox = new JCheckBox(new ResourceActionAdapter("wizard.show_error_rows"));

	private WizardState state;

	private JPanel panel = new JPanel(new BorderLayout());
	private JScrollPane tableScrollPane;

	public MetaDataDeclarationWizardStep(WizardState state) {
		super("importwizard.metadata");		
		this.state = state;
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonPanel.add(reloadButton);
		buttonPanel.add(guessButton);
		buttonPanel.add(errorsAsMissingBox);
		buttonPanel.add(filterErrorsBox);
		panel.add(buttonPanel, BorderLayout.NORTH);
		
		tableScrollPane = new JScrollPane(new JLabel("-"));
		//table = new JTable(); //false, false, false);
//		table.setCellColorProvider(new CellColorProvider() {
//            public Color getCellColor(int row, int col) {
//            	if (row % 2 == 0) {
//            		return Color.WHITE;
//            	} else {
//            		return SwingTools.LIGHTEST_BLUE;
//            	}
//            }
//		});		
		//panel.add(new ExtendedJScrollPane(table), BorderLayout.CENTER);
		//panel.add(new JScrollPane(table), BorderLayout.CENTER);
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
//		if (tableComponent != null) {
//			panel.remove(tableComponent);
//		}
		
		ExampleSet exampleSet = state.getCachedExampleSet();
		// Copy name annotations to name		
		int nameIndex = state.getTranslationConfiguration().getNameRow();		
		if (nameIndex != -1) {
			Example nameRow = exampleSet.getExample(nameIndex);
			int i = 0;
			Iterator<AttributeRole> r = exampleSet.getAttributes().allAttributeRoles();
			while (r.hasNext()) {
				state.getTranslationConfiguration().getColumnMetaData(i).setUserDefinedAttributeName(nameRow.getValueAsString(r.next().getAttribute()));
				i++;
			}
		}
		
		//table = new DataTableViewerTable(new DataTableExampleSetAdapter(exampleSet, null), false, false, false);		
				
		updateTableModel(exampleSet);
		panel.add(tableScrollPane, BorderLayout.CENTER);
		
//		table.setModel(new DataTableViewerTableModel(new DataTableExampleSetAdapter(exampleSet, null)));
//
//		MetaDataTableHeaderCellEditor headerEditor = new MetaDataTableHeaderCellEditor();
//		MetaDataTableHeaderCellEditor headerRenderer = new MetaDataTableHeaderCellEditor();
//		
//		TableColumnModel columnModel = table.getColumnModel();
//		table.setTableHeader(new EditableTableHeader(columnModel));
//
//		for (int i = 0; i < table.getColumnCount(); i++) {
//			EditableTableHeaderColumn col = (EditableTableHeaderColumn) table.getColumnModel().getColumn(i);
//			col.setHeaderValue(state.getTranslationConfiguration().getColumnMetaData()[i]);
//			col.setHeaderRenderer(headerRenderer);
//			col.setHeaderEditor(headerEditor);
//		}

		
		//updateTableModel(exampleSet);

		return true;
	}
	
	private void updateTableModel(ExampleSet exampleSet) {
		JTable table = new ExtendedJTable(false, false, false);
		
		// data model
		DataTableViewerTableModel model = new DataTableViewerTableModel(new DataTableExampleSetAdapter(exampleSet, null));
		table.setModel(model);
		
		// Header model
		
		TableColumnModel columnModel = table.getColumnModel();
		table.setTableHeader(new EditableTableHeader(columnModel));
		
		// header editors and renderers and values
		MetaDataTableHeaderCellEditor headerEditor = new MetaDataTableHeaderCellEditor();
		MetaDataTableHeaderCellEditor headerRenderer = new MetaDataTableHeaderCellEditor();
		for (int i = 0; i < table.getColumnCount(); i++) {
			EditableTableHeaderColumn col = (EditableTableHeaderColumn) table.getColumnModel().getColumn(i);
			col.setHeaderValue(state.getTranslationConfiguration().getColumnMetaData()[i]);
			col.setHeaderRenderer(headerRenderer);
			col.setHeaderEditor(headerEditor);
		}
		table.getTableHeader().setReorderingAllowed(false);
		
		tableScrollPane.setViewportView(table);
	}
	
	private void updateTableHeader(ColumnMetaData[] cmd) {
		
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.FINISH) {
			try {
				if (state.getTranslator() != null) {
					state.getTranslator().close();
				}
			} catch (OperatorException e) {
				// TODO: Show error dialog
				e.printStackTrace();
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
	
	private void reload() {
		reloadButton.configurePropertiesFromAction(cancelReloadAction);
	}
	
	private void cancelReload() {
		reloadButton.configurePropertiesFromAction(reloadAction);
	}
	
	private void guessValueTypes() {
		guessButton.configurePropertiesFromAction(cancelGuessValueTypes);
		new ProgressThread("guessing_value_types") {
			@Override
			public void run() {
				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(10);

				try {
					if (state.getTranslator() != null) {
						state.getTranslator().close();
					}
					DataResultSet resultSet = state.getDataResultSetFactory().makeDataResultSet(null);
					state.setTranslator(new DataResultSetTranslator(resultSet));
					getProgressListener().setCompleted(30);
					
//					state.setTranslationConfiguration(new DataResultSetTranslationConfiguration(resultSet));
//					getProgressListener().setCompleted(40);					
					state.getTranslator().guessValueTypes(state.getTranslationConfiguration(), resultSet, state.getNumberOfPreviewRows(), getProgressListener());
					getProgressListener().setCompleted(60);
					final ExampleSet exampleSet = state.readNow(true, getProgressListener());
					getProgressListener().setCompleted(80);

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							updateTableModel(exampleSet);							
						}
					});
				} catch (OperatorException e) {					
					// TODO: Show error dialog
					e.printStackTrace();
				} finally {
					getProgressListener().complete();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							cancelGuessing();							
						}						
					});
				}
			}			
		}.start();
	}
	
	private void cancelGuessing() {
		guessButton.configurePropertiesFromAction(guessValueTypes);
		isGuessing = false;
	}
	
	private Object GUESS_LOCK = new Object();
	private Object RELOAD_LOCK = new Object();
	private boolean isGuessing = false;
	private boolean isReloading = false;
	
	private void toggleGuessValueTypes() {
		synchronized (GUESS_LOCK) {
			isGuessing = !isGuessing;
			if (isGuessing) {
				guessValueTypes();
			} else {
				cancelGuessing();				
			}
		}
	}

	private void toggleReload() {
		synchronized (RELOAD_LOCK) {
			isReloading = !isReloading;
			if (isReloading) {
				reload();
			} else {
				cancelReload();				
			}
		}
	}

}
