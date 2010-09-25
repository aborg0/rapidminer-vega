package com.rapidminer.operator.nio;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.FileSelectionWizardStep;

/**
 * This step allows to select an file. With this file the {@link ExcelResultSetConfiguration} will be created.
 * 
 * @author Sebastian Land
 * 
 */
public class ExcelFileSelectionWizardStep extends FileSelectionWizardStep {

	private ExcelResultSetConfiguration configuration;
	
	/**
	 * There must be a configuration given, but might be empty.
	 */
	public ExcelFileSelectionWizardStep(AbstractWizard parent, ExcelResultSetConfiguration configuration) {
		super(parent, new FileFilter() {

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith("xls");
			}

			@Override
			public String getDescription() {
				return "Excel Files";
			}
			
		});
		this.configuration = configuration;
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		if (configuration.getFile() != null) {
			this.fileChooser.setSelectedFile(configuration.getFile());
		}
		return true;
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		configuration.setWorkbookFile(getSelectedFile());
		return true;
	}
}