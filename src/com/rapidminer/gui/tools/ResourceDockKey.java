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
package com.rapidminer.gui.tools;

import javax.swing.Icon;

import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.vlsolutions.swing.docking.DockKey;

/**
 * 
 * @author Simon Fischer
 *
 */
public class ResourceDockKey extends DockKey {
	
	public ResourceDockKey(String resourceKey) {
		super(resourceKey);		
		setName(getMessage(resourceKey + ".name"));
		setTooltip(getMessage(resourceKey + ".tip"));
		String iconName = getMessageOrNull(resourceKey + ".icon");
		if (iconName != null) {
			Icon icon = SwingTools.createIcon("16/"+iconName);
			if (icon != null) {
				setIcon(icon);
			} else {
				LogService.getRoot().warning("Missing icon: "+iconName);
			}
		}
		setFloatEnabled(true);
		setCloseEnabled(true);
		setAutoHideEnabled(true);		
	}
		
	private static String getMessage(String key) {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.dockkey."+key);
	}
	
	private static String getMessageOrNull(String key) {
		return I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.dockkey."+key);
	}

}
