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

import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
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
public class ExcelImportWizard extends AbstractDataImportWizard {

	private static final long serialVersionUID = 1L;
	
	public ExcelImportWizard(ExcelExampleSource source, ConfigurationListener listener, RepositoryLocation preselectedLocation) throws OperatorException {
		super(source, preselectedLocation, "data_import_wizard");
		
		// adding steps		
		addStep(new ExcelFileSelectionWizardStep(this, (ExcelResultSetConfiguration) getState().getDataResultSetFactory()));
		addStep(new ExcelSheetSelectionWizardStep((ExcelResultSetConfiguration) getState().getDataResultSetFactory()));
		addCommonSteps();
//		addStep(new AnnotationDeclarationWizardStep(state));
//		addStep(new MetaDataDeclarationWizardStep(state));
//		if (source == null) {
//			addStep(new StoreDataWizardStep(this, state, (preselectedLocation != null) ? preselectedLocation.getAbsoluteLocation() : null));
//		}
		layoutDefault(HUGE);
	}
	
	@Override
	public void cancel() {
		super.cancel();
	}

	@Override
	protected DataResultSetFactory makeFactory(AbstractDataResultSetReader reader) throws OperatorException {
		if (reader != null) {
			return new ExcelResultSetConfiguration((ExcelExampleSource) reader);
		} else {
			return new ExcelResultSetConfiguration();
		}
	}
}
