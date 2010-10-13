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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.CellColorProviderAlternating;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.table.EditableTableHeader;
import com.rapidminer.gui.tools.table.EditableTableHeaderColumn;
import com.rapidminer.gui.viewer.DataTableViewerTableModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.ParsingError;
import com.rapidminer.operator.nio.model.WizardState;
import com.rapidminer.parameter.ParameterTypeDateFormat;

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

	private JCheckBox errorsAsMissingBox = new JCheckBox(new ResourceAction("wizard.error_tolerant") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			state.getTranslationConfiguration().setFaultTolerant(errorsAsMissingBox.isSelected());
		}		
	});
	private JCheckBox filterErrorsBox = new JCheckBox(new ResourceActionAdapter("wizard.show_error_rows"));

	private JComboBox dateFormatField = new JComboBox(ParameterTypeDateFormat.PREDEFINED_DATE_FORMATS);
	
	private WizardState state;

	private JPanel panel = new JPanel(new BorderLayout());
	private JScrollPane tableScrollPane;

	private ErrorTableModel errorTableModel = new ErrorTableModel();
	private JLabel errorLabel = new JLabel();

	public MetaDataDeclarationWizardStep(WizardState state) {
		super("importwizard.metadata");		
		this.state = state;
		dateFormatField.setEditable(true);
		dateFormatField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MetaDataDeclarationWizardStep.this.state.getTranslationConfiguration().setDatePattern((String)dateFormatField.getSelectedItem());
			}
		});
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonPanel.add(reloadButton);
		buttonPanel.add(guessButton);
		
		JLabel label = new ResourceLabel("date_format");
		label.setLabelFor(dateFormatField);
		buttonPanel.add(label);
		buttonPanel.add(dateFormatField);
		panel.add(buttonPanel, BorderLayout.NORTH);

		buttonPanel.add(errorsAsMissingBox);
		buttonPanel.add(filterErrorsBox);
		
		JPanel errorPanel = new JPanel(new BorderLayout());
		errorPanel.add(errorLabel, BorderLayout.NORTH);
		final JTable errorTable = new JTable(errorTableModel);		
		final JScrollPane errorScrollPane = new JScrollPane(errorTable);
		errorScrollPane.setPreferredSize(new Dimension(500, 80));
		errorPanel.add(errorScrollPane, BorderLayout.CENTER);		
		panel.add(errorPanel, BorderLayout.SOUTH);

		final JLabel dummy = new JLabel("-");
		dummy.setPreferredSize(new Dimension(500, 500));
		dummy.setMinimumSize(new Dimension(500, 500));
		tableScrollPane = new JScrollPane(dummy, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		panel.add(tableScrollPane, BorderLayout.CENTER);
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		dateFormatField.setSelectedItem(state.getTranslationConfiguration().getDatePattern());
		errorsAsMissingBox.setSelected(state.getTranslationConfiguration().isFaultTolerant());
		
		new ProgressThread("loading_data") {
			@Override
			public void run() {
				try {
					final DataResultSet previewResultSet = state.getDataResultSetFactory().makeDataResultSet(state.getOperator());
					state.getTranslationConfiguration().reconfigure(previewResultSet);
				} catch (OperatorException e1) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e1.toString(), e1);
					return;
				}
				
				try {
					TableModel dataPreview = state.getDataResultSetFactory().makePreviewTableModel(getProgressListener());
					// Copy name annotations to name		
					int nameIndex = state.getTranslationConfiguration().getNameRow();		
					if (nameIndex != -1) {
						for (int i = 0; i < dataPreview.getColumnCount(); i++) {
							state.getTranslationConfiguration().getColumnMetaData(i).setUserDefinedAttributeName((String) dataPreview.getValueAt(nameIndex, i));
						}
					}
				} catch (Exception e) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
					return;
				}
				guessValueTypes();
			}
			
		}.start();
		return true;
	}

	private void updateErrors() {
		final int size = state.getTranslator().getErrors().size();
		errorLabel.setText(size+" errors.");
		if (size == 0) {
			errorLabel.setIcon(SwingTools.createIcon("16/ok.png"));
		} else {
			errorLabel.setIcon(SwingTools.createIcon("16/error.png"));
		}
		errorTableModel.setErrors(state.getTranslator().getErrors());
	}

	private void updateTableModel(ExampleSet exampleSet) {
		ExtendedJTable table = new ExtendedJTable(false, false, false);

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

		table.setCellColorProvider(new CellColorProviderAlternating() {
			@Override
			public Color getCellColor(int row, int column) {
				ParsingError error = state.getTranslator().getErrorByExampleIndexAndColumn(row, column);
				if (error != null) {
					return SwingTools.DARK_YELLOW;
				} else {
					return super.getCellColor(row, column);
				}
			}
		});
		tableScrollPane.setViewportView(table);
	}	

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.FINISH) {
			try {
				if (state.getTranslator() != null) {
					state.getTranslator().close();
				}
			} catch (OperatorException e) {
				ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
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
		new ProgressThread("loading_data") {
			@Override
			public void run() {
				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(10);

				try {
					if (state.getTranslator() != null) {
						state.getTranslator().close();
					}
					DataResultSet resultSet = state.getDataResultSetFactory().makeDataResultSet(null);
					state.getTranslator().clearErrors();
					final ExampleSet exampleSet = state.readNow(resultSet, true, getProgressListener());
					getProgressListener().setCompleted(80);

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							updateTableModel(exampleSet);
							updateErrors();
						}
					});
				} catch (OperatorException e) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
				} finally {
					getProgressListener().complete();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							cancelReload();							
						}						
					});
				}
			}			
		}.start();
	}

	private void cancelReload() {
		reloadButton.configurePropertiesFromAction(reloadAction);
		isReloading = false;
	}

	private void guessValueTypes() {
		guessButton.configurePropertiesFromAction(cancelGuessValueTypes);
		isGuessing = true;
		new ProgressThread("guessing_value_types") {
			@Override
			public void run() {
				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(10);
				Thread.yield();
				try {
					if (state.getTranslator() != null) {
						state.getTranslator().close();
					}				
					DataResultSet resultSet = state.getDataResultSetFactory().makeDataResultSet(null);
					state.getTranslator().clearErrors();
					getProgressListener().setCompleted(30);
					state.getTranslationConfiguration().resetValueTypes();
					state.getTranslator().guessValueTypes(state.getTranslationConfiguration(), resultSet, state.getNumberOfPreviewRows(), getProgressListener());
					getProgressListener().setCompleted(60);
					final ExampleSet exampleSet = state.readNow(resultSet, true, getProgressListener());
					getProgressListener().setCompleted(80);

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							updateTableModel(exampleSet);		
							updateErrors();
						}
					});
				} catch (OperatorException e) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
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

	private boolean isGuessing = false;
	private boolean isReloading = false;

	private void toggleGuessValueTypes() {
		isGuessing = !isGuessing;
		if (isGuessing) {
			guessValueTypes();
		} else {
			cancelGuessing();				
		}
	}

	private void toggleReload() {
		isReloading = !isReloading;
		if (isReloading) {
			reload();
		} else {
			cancelReload();				
		}
	}

}
