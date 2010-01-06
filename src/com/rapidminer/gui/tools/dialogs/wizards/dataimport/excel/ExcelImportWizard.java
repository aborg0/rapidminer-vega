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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.rapidminer.gui.tools.SimpleFileFilter;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.AttributeSelectionWizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.FileSelectionWizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.RepositoryLocationSelectionWizardStep;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.ExcelExampleSource;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.OperatorService;


/**
 * @author Tobias Malbrecht
 */
public class ExcelImportWizard extends DataImportWizard {
	private static final long serialVersionUID = -4308448171060612833L;
	
	private File file = null;
	
	private ExcelExampleSource reader = null;
	
	private ExampleSetMetaData metaData = null;
	
	private final WizardStep STEP_FILE_SELECTION = new FileSelectionWizardStep(this, new SimpleFileFilter("Excel File (.xls)", ".xls")) {
		@Override
		protected boolean performLeavingAction() {
			file = getSelectedFile();
			reader.setParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE, file.getAbsolutePath());
			return true;
		}
	};
	
	private final WizardStep STEP_EXCEL_DATA_SELECTION = new WizardStep("excel_data_selection") {
		 
		private final ExcelWorkbookPane workbookSelectionPanel = new ExcelWorkbookPane();
		
		private final JCheckBox useFirstRowAsColumnNamesCheckBox = new JCheckBox("Use First Row As Column Names", true);

		@Override
		protected boolean canGoBack() {
			return true;
		}

		@Override
		protected boolean canProceed() {
			return true;
		}
		
		@Override
		protected boolean performEnteringAction() {
			workbookSelectionPanel.loadWorkbook(file);
			reader.setParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE, file.getAbsolutePath());
			return true;
		}
		
		@Override
		protected boolean performLeavingAction() {
			reader.setParameter(ExcelExampleSource.PARAMETER_SHEET_NUMBER, Integer.toString(workbookSelectionPanel.getSelection().getSheetIndex() + 1));
			reader.setParameter(ExcelExampleSource.PARAMETER_FIRST_ROW_AS_NAMES, Boolean.toString(useFirstRowAsColumnNamesCheckBox.isSelected()));
			try {
				metaData = (ExampleSetMetaData) reader.getGeneratedMetaData();
			} catch (OperatorException e) {
				return false;
			}
			return true;
		}

		@Override
		protected JComponent getComponent() {
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(workbookSelectionPanel, BorderLayout.CENTER);
			panel.add(useFirstRowAsColumnNamesCheckBox, BorderLayout.SOUTH);
			return panel;
		}
	};
	
	private final WizardStep STEP_ATTRIBUTES_SELECTION = new AttributeSelectionWizardStep("select_attributes") {
		@Override
		protected boolean canGoBack() {
			return true;
		}

		@Override
		protected boolean canProceed() {
			return true;
		}

		@Override
		protected boolean performEnteringAction() {
			setMetaData(metaData);
			return true;
		}
	};
	
	public ExcelImportWizard(String i18nKey, Object ... i18nArgs) {
		this(i18nKey, null, null, i18nArgs);
	}
	
	public ExcelImportWizard(String i18nKey, File preselectedFile, RepositoryLocation preselectedLocation, Object ... i18nArgs) {
		super(i18nKey, i18nArgs);
		file = preselectedFile;
		try {
			reader = OperatorService.createOperator(com.rapidminer.operator.io.ExcelExampleSource.class);
		} catch (OperatorCreationException e) {
		}
		if (preselectedFile == null) {
			addStep(STEP_FILE_SELECTION);
		}
		addStep(STEP_EXCEL_DATA_SELECTION);
		addStep(STEP_ATTRIBUTES_SELECTION);
		addStep(new RepositoryLocationSelectionWizardStep("select_repository_location", this, null, preselectedLocation != null ? preselectedLocation.getAbsoluteLocation() : null) {
			@Override
			protected boolean performLeavingAction() {
				return transferData(reader, metaData, getRepositoryLocation());
			}			
		});
		layoutDefault();
	}
	
	public ExcelImportWizard(String i18nKey, ExcelExampleSource reader, Object ... i18nArgs) {
		super(i18nKey, i18nArgs);
		this.reader = reader;
		addStep(STEP_FILE_SELECTION);
		addStep(STEP_EXCEL_DATA_SELECTION);
		layoutDefault();
	}
}
