package com.rapidminer.operator.nio;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.FileSelectionWizardStep;
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;

/**
 * This step allows to select an file. With this file the {@link CSVResultSetConfiguration} will be created.
 * 
 * @author Simon Fischer
 * 
 */
public class CSVFileSelectionWizardStep extends FileSelectionWizardStep {

	private CSVResultSetConfiguration configuration;
	
	/**
	 * There must be a configuration given, but might be empty.
	 */
	public CSVFileSelectionWizardStep(AbstractWizard parent, CSVResultSetConfiguration configuration) {
		super(parent, configuration.getCsvFileAsFile(), new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith("csv");
			}
			@Override
			public String getDescription() {
				return "Delimiter separated files";
			}		
		});
		this.configuration = configuration;
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		if (configuration.getCsvFile() != null) {
			this.fileChooser.setSelectedFile(configuration.getCsvFileAsFile());
		}
		return true;
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		configuration.setCsvFile(getSelectedFile().getAbsolutePath());
		return true;
	}
}