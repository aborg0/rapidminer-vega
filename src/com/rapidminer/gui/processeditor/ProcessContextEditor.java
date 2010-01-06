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
package com.rapidminer.gui.processeditor;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.rapidminer.Process;
import com.rapidminer.ProcessContext;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.repository.gui.RunRemoteDialog;

/** An editor to editor {@link ProcessContext}s. This is used in the {@link RunRemoteDialog} and
 *  in the {@link ProcessContextProcessEditor}.
 *  
 * @author Simon Fischer, Tobias Malbrecht
 *
 */
public class ProcessContextEditor extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private final RepositoryLocationsEditor<OutputPorts> inputEditor;
	private final RepositoryLocationsEditor<InputPorts> outputEditor;
	private final MacroEditor macroEditor;

	public ProcessContextEditor(Process process) {
		inputEditor = new RepositoryLocationsEditor<OutputPorts>(true, "context.input", "input"); 
		outputEditor = new RepositoryLocationsEditor<InputPorts>(false, "context.output", "result");
		macroEditor = new MacroEditor(true);

		setLayout(new GridLayout(3, 1));
		((GridLayout) getLayout()).setHgap(0);
		((GridLayout) getLayout()).setVgap(10);
		
		inputEditor.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		outputEditor.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

		add(inputEditor);
		add(outputEditor);
		add(macroEditor);
		
		setProcess(process);
	}
	
	protected void setProcess(Process process) {
		ProcessContext context = process != null ? process.getContext() : null;
		macroEditor.setContext(context);
		if (context != null) {
			inputEditor.setData(process.getContext(), process.getRootOperator().getSubprocess(0).getInnerSources());
			outputEditor.setData(process.getContext(), process.getRootOperator().getSubprocess(0).getInnerSinks());
		} else {
//			inputEditor.clear();
//			outputEditor.clear();
		}
	}

//	public List<String> getInputRepositoryLocations() {
//		return inputEditor.getRepositoryLocations();
//	}
//
//	public List<String> getOutputRepositoryLocations() {
//		return outputEditor.getRepositoryLocations();
//	}

}
