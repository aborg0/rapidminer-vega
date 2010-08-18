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
package com.rapidminer.gui.properties;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ScrollPaneConstants;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.ParameterService;


/**
 * The tabs for the different groups of RapidMiner settings. Each tab contains a
 * {@link SettingsPropertyPanel} for the settings in this group.
 * 
 * @author Ingo Mierswa
 */
public class SettingsTabs extends ExtendedJTabbedPane {

	private static final long serialVersionUID = -229446448782516589L;

	private final List<SettingsPropertyPanel> tables = new LinkedList<SettingsPropertyPanel>();

	public SettingsTabs() {
		this(null);
	}
	
	public SettingsTabs(String initialSelectedTab) {
		Set<ParameterType> allProperties = RapidMiner.getRapidMinerProperties();
		SortedMap<String, List<ParameterType>> groups = new TreeMap<String, List<ParameterType>>();
		Iterator<ParameterType> i = allProperties.iterator();
		while (i.hasNext()) {
			ParameterType type = i.next();
			String key = type.getKey();
			String[] parts = key.split("\\.");
			String group;
			if ("rapidminer".equals(parts[0])) {
				group = parts[1];
			} else {
				group = "system";
			}
			List<ParameterType> list = groups.get(group);
			if (list == null) {
				list = new LinkedList<ParameterType>();
				groups.put(group, list);
			}
			list.add(type);
		}

		Iterator<Map.Entry<String,List<ParameterType>>> it =
						groups.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String,List<ParameterType>> e = it.next();
			String group = e.getKey();
			if ((initialSelectedTab != null) && !initialSelectedTab.equals(group)) {
				continue;
			}
			List<ParameterType> groupList = e.getValue();
			SettingsPropertyPanel table = new SettingsPropertyPanel(groupList);
			tables.add(table);
			String name = new String(new char[] { group.charAt(0) }).toUpperCase() + group.substring(1, group.length());
			ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(table);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			scrollPane.setPreferredSize(new Dimension(600, 300));
			addTab(name, scrollPane);
		}
	}

	public void applyProperties() {
		Iterator i = tables.iterator();
		while (i.hasNext()) {
			((SettingsPropertyPanel) i.next()).applyProperties();
		}
	}

	public void save() throws IOException {
		applyProperties();
		Properties props = new Properties();	
		Iterator<SettingsPropertyPanel> i = tables.iterator();
		while (i.hasNext()) {
			i.next().applyProperties(props);
		}
		ParameterService.writeProperties(props, ParameterService.getMainUserConfigFile());		
	}
}
