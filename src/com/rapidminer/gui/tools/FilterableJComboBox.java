/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
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
package com.rapidminer.gui.tools;

import java.util.Arrays;
import java.util.Vector;

import javax.swing.ComboBoxModel;

import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
/**
 * 
 * @author Sebastian Land
 */
public class FilterableJComboBox extends ExtendedJComboBox{

	private static final long serialVersionUID = -4910048606664095221L;
	private transient AutoCompleteSupport<String> autoCompl;

	public FilterableJComboBox(String[] values) {
		super(values);
		Vector<String> valueVector = new Vector<String>();
		valueVector.addAll(Arrays.asList(values));
		refreshAutoCompletionSupport(valueVector);
	}

	public FilterableJComboBox(ComboBoxModel model) {
		super(model);
		Vector<String> values = new Vector<String>();
		int size = model.getSize();
		for (int i = 0; i < size; i++) {
			values.add((String) model.getElementAt(i));
		}
		refreshAutoCompletionSupport(values);
	}
	
	public FilterableJComboBox(Vector<String> values) {
		super(values.toArray(new String[values.size()]));
		refreshAutoCompletionSupport(values);
		//initAutoCompletition(values);
	}

	private void initAutoCompletition(Vector<String> valueVector) {
		autoCompl = AutoCompleteSupport.install(this, GlazedLists.eventList(valueVector));		
		autoCompl.setStrict(false);
		autoCompl.setCorrectsCase(false);
		autoCompl.setFilterMode(TextMatcherEditor.STARTS_WITH);
		autoCompl.setSelectsTextOnFocusGain(true);
		autoCompl.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
	}
	
	public void refreshAutoCompletionSupport(Vector<String> values) {
		if (autoCompl != null) {
			autoCompl.uninstall();
			autoCompl = null;
		}
		initAutoCompletition(values);
	}

	public AutoCompleteSupport<String> getAutoCompleteSupport() {
		return autoCompl;
	}
}
