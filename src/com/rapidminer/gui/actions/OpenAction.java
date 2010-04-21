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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;
import com.rapidminer.tools.XMLException;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class OpenAction extends ResourceAction {

	private static final long serialVersionUID = -323403851840397447L;
	
	public OpenAction() {
		super("open");	
		
		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	public void actionPerformed(ActionEvent e) {
		open();
	}
		
	public static void open() {
		if (RapidMinerGUI.getMainFrame().close()) {
			String location = RepositoryLocationChooser.selectLocation(null, RapidMinerGUI.getMainFrame());
			if (location != null) {
				try {
					open(new RepositoryProcessLocation(new RepositoryLocation(location)), true);
				} catch (MalformedRepositoryLocationException e) {
					SwingTools.showSimpleErrorMessage("while_loading", e, location, e.getMessage());
				}
			}			
		}
	}

	
	public static void open(final ProcessLocation processLocation, final boolean showInfo) {
		RapidMinerGUI.getMainFrame().stopProcess();
		ProgressThread openProgressThread = new ProgressThread("open_file") {
			public void run() {		
				try {				
					Process process = processLocation.load(getProgressListener());
					process.setProcessLocation(processLocation);
					RapidMinerGUI.getMainFrame().setOpenedProcess(process, showInfo, processLocation.toString());					
				} catch (XMLException ex) {
					try {
						RapidMinerGUI.getMainFrame().handleBrokenProxessXML(processLocation, processLocation.getRawXML(), ex);
					} catch (IOException e) {
						SwingTools.showSimpleErrorMessage("while_loading", e, processLocation, e.getMessage());
						return;
					}					
				} catch (Exception e) {
					SwingTools.showSimpleErrorMessage("while_loading", e, processLocation, e.getMessage());
					return;
				}			
			}
		};
		openProgressThread.start();
	}
}
