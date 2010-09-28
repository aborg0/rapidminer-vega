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
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;

import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
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
	
	public MetaDataDeclarationWizardStep(AnnotationDeclarationWizardStep previousStep) {
		super("importwizard.metadata");		
		this.previousStep = previousStep;
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		panel.removeAll();
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
		panel.add(new ExtendedJScrollPane(table), BorderLayout.CENTER);
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
}
