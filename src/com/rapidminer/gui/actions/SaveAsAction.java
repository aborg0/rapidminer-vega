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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.Process;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class SaveAsAction extends ResourceAction {

	private static final long serialVersionUID = -6107588898380953147L;
		
	public SaveAsAction() {
		super("save_as");		
		
		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	public void actionPerformed(ActionEvent e) {
		saveAs(RapidMinerGUI.getMainFrame().getProcess());
	}
	
	public static void saveAs(Process process) {
		String initial = null;
		if (process.getRepositoryLocation() != null) {
			initial = process.getRepositoryLocation().toString();
		}
		String loc = RepositoryLocationChooser.selectLocation(null, initial, RapidMinerGUI.getMainFrame());
		if (loc!= null) {
			process.setProcessLocation(new RepositoryProcessLocation(new RepositoryLocation(loc)));
			SaveAction.save(process);
		}
	}
}
