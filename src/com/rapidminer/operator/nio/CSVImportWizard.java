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
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;
import com.rapidminer.operator.nio.model.WizardState;
import com.rapidminer.repository.RepositoryLocation;

/**
 * This is the Wizard for Excel Import. It consists of several steps:
 * - Selecting Excel file
 * - Selecting Sheet and possibly selection of sheet
 * - Defining Annotations Step
 * - Defining Meta Data
 * 
 * @author Tobias Malbrecht, Sebastian Loh, Sebastian Land, Simon Fischer
 */
public class CSVImportWizard extends DataImportWizard {

	private static final long serialVersionUID = -4308448171060612833L;
	private WizardState state;
	private CSVExampleSource source;
	private CSVResultSetConfiguration csvConfiguration;
	
	public CSVImportWizard(CSVExampleSource source, ConfigurationListener listener, final boolean showStoreInRepositoryStep, RepositoryLocation preselectedLocation,String i18nKey, Object... i18nArgs) throws OperatorException {
		super(i18nKey, i18nArgs);
		this.source = source;
		
		if (source != null) {
			csvConfiguration = new CSVResultSetConfiguration(source);
		} else {
			csvConfiguration = new CSVResultSetConfiguration();
		}
		
		state = new WizardState(source, csvConfiguration);
		
		// adding steps		
		addStep(new CSVFileSelectionWizardStep(this, csvConfiguration));
		addStep(new CSVSyntaxConfigurationWizardStep(this, csvConfiguration));
		addStep(new AnnotationDeclarationWizardStep(state));
		addStep(new MetaDataDeclarationWizardStep(state));
		layoutDefault(HUGE);
	}
	
	@Override
	public void cancel() {
		super.cancel();
	}

	@Override
	public void finish() {
		super.finish();
		if (source != null) {
			state.getTranslationConfiguration().setParameters(source);
			csvConfiguration.setParameters(source);
		}
	}
}
