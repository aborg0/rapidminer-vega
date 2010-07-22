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
package com.rapidminer.gui.tools.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.processeditor.profiler.ProfilingViewer;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * Start the corresponding action.
 * 
 * @author Marco Boeck
 */
public class SaveProfilerAction extends ResourceAction {

	/** the ProfilingViewer instance */
	private ProfilingViewer profilingViewer;

	private static final long serialVersionUID = 7235883223940060389L;
	
	
	public SaveProfilerAction(ProfilingViewer profilingViewer) {
		super(true, "save_profiling_viewer");
		this.profilingViewer = profilingViewer;
	}

	public void actionPerformed(ActionEvent e) {
		this.profilingViewer.saveData();
	}
}
