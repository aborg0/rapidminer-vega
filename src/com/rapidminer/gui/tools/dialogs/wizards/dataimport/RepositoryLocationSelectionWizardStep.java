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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport;

import javax.swing.JComponent;

import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;

/**
 * 
 * @author Tobias Malbrecht
 */
public class RepositoryLocationSelectionWizardStep extends WizardStep {

	private final RepositoryLocationChooser locationChooser;
	
	public RepositoryLocationSelectionWizardStep(String key, AbstractWizard parent, RepositoryLocation location, String initialValue) {
		super(key);
		this.locationChooser = new RepositoryLocationChooser(parent, location, initialValue);
		this.locationChooser.addChangeListener(parent);
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}
	
	@Override
	protected boolean canProceed() {
		return locationChooser.hasSelection();
	}

	@Override
	protected JComponent getComponent() {
		return locationChooser;
	}
	
	public String getRepositoryLocation() {
		return locationChooser.getRepositoryLocation();
	}
}
