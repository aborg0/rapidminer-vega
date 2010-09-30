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

import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.operator.nio.model.WizardState;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.RepositoryLocation;

/**
 * This is the Wizard for Excel Import. It consists of several steps:
 * - Selecting Excel file
 * - Selecting Sheet and possibly selection of sheet
 * - Defining Annotations Step
 * - Defining Meta Data
 * 
 * @author Tobias Malbrecht, Sebastian Loh, Sebastian Land
 */
public class ExcelImportWizard extends DataImportWizard {

	private static final long serialVersionUID = -4308448171060612833L;
	
	public ExcelImportWizard(ExcelExampleSource source, ConfigurationListener listener, final boolean showStoreInRepositoryStep, RepositoryLocation preselectedLocation,String i18nKey, Object... i18nArgs) throws UndefinedParameterError {
		super(i18nKey, i18nArgs);

		ExcelResultSetConfiguration configuration;
		if (source != null) {
			configuration = new ExcelResultSetConfiguration(source);
		} else {
			configuration = new ExcelResultSetConfiguration();
		}
		
		WizardState state = new WizardState(configuration);
		
		// adding steps		
		addStep(new ExcelFileSelectionWizardStep(this, configuration));
		addStep(new ExcelSheetSelectionWizardStep(configuration));
		addStep(new AnnotationDeclarationWizardStep(state));
		addStep(new MetaDataDeclarationWizardStep(state));
//
//		addStep(new MetaDataDeclerationWizardStep("select_attributes", null) {
//
//			@Override
//			protected JComponent getComponent() {
//				JPanel typeDetection = new JPanel(ButtonDialog.createGridLayout(1, 2));
//				typeDetection.setBorder(ButtonDialog.createTitledBorder("Value Type Detection"));
//				typeDetection.add(new JLabel("Guess the value types of all attributes"));
//				typeDetection.add(guessingButtonsPanel);
//
//				Component[] superComponents = super.getComponent().getComponents();
//
//				JPanel upperPanel = new JPanel(new BorderLayout());// new
//				// JPanel(ButtonDialog.createGridLayout(2,
//				// 1));
//				upperPanel.add(typeDetection, BorderLayout.NORTH);
//				upperPanel.add(superComponents[0], BorderLayout.CENTER);
//
//				JPanel panel = new JPanel(new BorderLayout(0, ButtonDialog.GAP));
//				panel.add(upperPanel, BorderLayout.NORTH);
//				panel.add(superComponents[1], BorderLayout.CENTER);
//
//				return panel;
//			}
//
//			@Override
//			protected void doAfterEnteringAction() {
//				reader.setAttributeNamesDefinedByUser(true);
//				((ExcelExampleSource) reader).skipNameAnnotationRow(true);
//			}
//
//			@Override
//			protected boolean performLeavingAction() {
//				reader.stopReading();
//				reader.writeMetaDataInParameter();
////TODO				if (ExcelImportWizard.this.isComplete()) {
////					((ExcelExampleSource) reader).resetWorkbook();
////				}
//				return true;
//			}
//
//		});

//		if (showStoreInRepositoryStep) {
//			addStep(new RepositoryLocationSelectionWizardStep("select_repository_location", this, null, preselectedLocation != null ? preselectedLocation.getAbsoluteLocation() : null) {
//				@Override
//				protected boolean performLeavingAction() {
////					synchronized (reader) {
////						boolean flag = transferData(reader, getRepositoryLocation());
////						//TODO (reader).resetWorkbook();
////						return flag;
////					}
//				}
//			});
//		}

		layoutDefault(HUGE);
	}
	
	@Override
	public void cancel() {
		super.cancel();
	}

	@Override
	public void finish() {
		super.finish();
	}
}
