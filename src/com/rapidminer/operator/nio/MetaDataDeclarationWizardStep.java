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
import javax.swing.JPanel;
import javax.swing.JTable;

import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.table.EditableHeaderJTable;
import com.rapidminer.gui.viewer.DataTableViewerTable;
import com.rapidminer.operator.OperatorException;

/**
 * This Wizard Step might be used to defined
 * the meta data of each attribute.
 * 
 * @author Sebastian Land, Simon Fischer
 */
public class MetaDataDeclarationWizardStep extends WizardStep {

	private JPanel panel = new JPanel(new BorderLayout());
	
	private AnnotationDeclarationWizardStep previousStep;
	
	private DataResultSetTranslator translator = null;
	private DataResultSetTranslationConfiguration config;
	//private DataResultSet resultSet;

	private ExampleSet exampleSet;

	private JComponent tableComponent;

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
		
	public MetaDataDeclarationWizardStep(AnnotationDeclarationWizardStep previousStep) {
		super("importwizard.metadata");		
		this.previousStep = previousStep;
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonPanel.add(reloadButton);
		buttonPanel.add(guessButton);
		buttonPanel.add(errorsAsMissingBox);
		buttonPanel.add(filterErrorsBox);
		panel.add(buttonPanel, BorderLayout.NORTH);
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		if (tableComponent != null) {
			panel.remove(tableComponent);
		}
		this.config = previousStep.getConfiguration();
		this.exampleSet = previousStep.getParsedExampleSet();
		
		// Copy name annotations to name
		int nameIndex = config.getNameRow();
		if (nameIndex != -1) {
			Example nameRow = exampleSet.getExample(nameIndex);
			int i = 0;
			Iterator<AttributeRole> r = exampleSet.getAttributes().allAttributeRoles();
			while (r.hasNext()) {
				config.getColumnMetaData(i).setUserDefinedAttributeName(nameRow.getValueAsString(r.next().getAttribute()));
				i++;
			}
		}
		
		JTable table = new DataTableViewerTable(new DataTableExampleSetAdapter(exampleSet, null), false, false, false);
		MetaDataTableHeaderCellEditor headerEditor = new MetaDataTableHeaderCellEditor();
		MetaDataTableHeaderCellEditor headerRenderer = new MetaDataTableHeaderCellEditor();
		EditableHeaderJTable.installEditableHeader(table, headerRenderer, headerEditor, config.getColumnMetaData());
		tableComponent = new ExtendedJScrollPane(table);
		panel.add(tableComponent, BorderLayout.CENTER);
		return true;
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.FINISH) {
			try {
				if (translator != null) {
					translator.close();
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
	}
	
	private void cancelGuessing() {
		guessButton.configurePropertiesFromAction(guessValueTypes);		
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
