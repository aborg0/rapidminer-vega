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
package com.rapidminer.gui.tools.dialogs.wizards;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.rapidminer.gui.tools.dialogs.MultiPageDialog;

/**
 * 
 * @author Tobias Malbrecht
 */
public class AbstractWizard extends MultiPageDialog {

	private static final long serialVersionUID = 7091671433172940496L;
	
	private ArrayList<WizardStep> wizardSteps = new ArrayList<WizardStep>();
	
	private boolean showStepNumbersInTitle = true;
	
	private boolean showStepInfo = true;
	
	public AbstractWizard(Dialog owner, String key, Object ... arguments) {
		super(owner, key, true, arguments);
	}
	
	public AbstractWizard(Frame owner, String key, Object ... arguments) {
		super(owner, key, true, arguments);
	}
	
	protected void layoutDefault(int size) {
		Map<String, Component> cards = new LinkedHashMap<String, Component>();
		for (WizardStep wizardStep : wizardSteps) {
			cards.put(wizardStep.getTitle(), wizardStep.getComponent());
		}
		layoutDefault(cards, size);
		updateTitle();
		updateInfoHeader();
	}
	
	protected void layoutDefault() {
		layoutDefault(NORMAL);
	}

	protected void addStep(WizardStep wizardStep) {
		wizardStep.addChangeListener(this);
		wizardSteps.add(wizardStep);
	}
	
	@Override
	protected boolean canProceed(int step) {
		return wizardSteps.get(step).canProceed();
	}
	
	@Override
	protected boolean canGoBack(int step) {
		return wizardSteps.get(step).canGoBack();
	}

	@Override
	protected String getNameForStep(int step) {
		return wizardSteps.get(step).getTitle();
	}

	@Override
	protected boolean isComplete() {
		return wizardSteps.get(wizardSteps.size() - 1).canProceed();
	}

	@Override
	protected boolean isLastStep(int step) {
		return (step == wizardSteps.size() - 1);
	}
	
	@Override
	protected void previous() {
		super.previous();
		updateTitle();
		updateInfoHeader();
	}
	
	@Override
	protected void next() {
		if (!getCurrentWizardStep().performLeavingAction()) {
			return;
		}
		super.next();
		updateTitle();
		updateInfoHeader();
		getCurrentWizardStep().performEnteringAction();
	}
	
	@Override
	protected void finish() {
		if (!getCurrentWizardStep().performLeavingAction()) {
			return;
		}
		super.finish();
	}
	
	private WizardStep getCurrentWizardStep() {
		return wizardSteps.get(getCurrentStep());
	}
	
	protected void updateTitle() {
		if (showStepNumbersInTitle) {
			setTitle(getDialogTitle() + " - Step " + (getCurrentStep() + 1 + " of " + wizardSteps.size()));
		}
	}
	
	protected void updateInfoHeader() {
		if (showStepInfo) {
			infoTextLabel.setText(getInfoText() + "<br/><b>Step " + (getCurrentStep() + 1) + ":</b> " + wizardSteps.get(getCurrentStep()).getInfoText());
		}
	}
	
	@Override
	public void setVisible(boolean visible) {
		getCurrentWizardStep().performEnteringAction();
		super.setVisible(visible);
	}
}
