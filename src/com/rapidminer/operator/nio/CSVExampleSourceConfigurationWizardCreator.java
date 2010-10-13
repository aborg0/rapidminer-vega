package com.rapidminer.operator.nio;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.wizards.AbstractConfigurationWizardCreator;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;

/**
 * Creates an {@link ExcelImportWizard}.
 * 
 * @author Sebastian Loh (06.05.2010)
 * 
 */
public class CSVExampleSourceConfigurationWizardCreator extends AbstractConfigurationWizardCreator {

	private static final long serialVersionUID = 1L;

	@Override
	public void createConfigurationWizard(ParameterType type, ConfigurationListener listener) {
		CSVExampleSource sourceOperator = (CSVExampleSource) listener;
		try {
			new CSVImportWizard(sourceOperator, listener, false, null, getI18NKey()).setVisible(true);
		} catch (OperatorException e) {
			SwingTools.showSimpleErrorMessage("importwizard.error_creating_wizard", e);
		}
	}

	@Override
	public String getI18NKey() {
		return "data_import_wizard";
	}
}