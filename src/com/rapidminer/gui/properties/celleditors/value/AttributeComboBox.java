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
package com.rapidminer.gui.properties.celleditors.value;

import java.util.Vector;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.tools.FilterableJComboBox;
import com.rapidminer.parameter.ParameterTypeAttribute;

/** Autocompletion combo box that observes an input port so it can update
 *  itself whenever the meta data changes.
 * 
 * @author Simon Fischer
 *
 */
public class AttributeComboBox extends FilterableJComboBox {

	private static final long serialVersionUID = 1L;

	private ParameterTypeAttribute type;
	private Vector<String> displayedAttributeNames; 
	
	public AttributeComboBox(ParameterTypeAttribute type) {
		super(type.getAttributeNames());
		this.type = type;
		displayedAttributeNames = type.getAttributeNames();
		addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				if (updateComboBoxModel()) {
					hidePopup();
					showPopup();
				}
			}			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) { }
		});

	}
	
	private boolean updateComboBoxModel() {
		Vector<String> newNames = type.getAttributeNames();
		if (!newNames.equals(displayedAttributeNames)) {
			refreshAutoCompletionSupport(newNames);
			displayedAttributeNames = newNames;
			return true;
		} else {
			return false;
		}
	}	
}
