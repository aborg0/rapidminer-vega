package com.rapidminer.operator.nio;

import com.rapidminer.gui.wizards.AbstractConfigurationWizardCreator;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;

/**
 * Creates a {@link ExcelImportWizard}.
 * 
 * @author Sebastian Loh (06.05.2010)
 * 
 */
public class ExcelExampleSourceConfigurationWizardCreator extends AbstractConfigurationWizardCreator {

	private static final long serialVersionUID = 1L;

	@Override
	public void createConfigurationWizard(ParameterType type, ConfigurationListener listener) {
		ExcelExampleSource sourceOperator = (ExcelExampleSource) listener;
		try {
			new ExcelImportWizard(sourceOperator, listener, false, null, getI18NKey()).setVisible(true);
		} catch (OperatorException e) {
			// TODO : Handle correctly
			e.printStackTrace();
		}
	}

	@Override
	public String getI18NKey() {
		return "data_import_wizard";
	}
}