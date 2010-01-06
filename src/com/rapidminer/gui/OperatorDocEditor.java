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
package com.rapidminer.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.rapidminer.Process;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.documentation.OperatorDocumentation;


/** Extends the standard operator documentation viewer with edit functionality.
 * 
 * @author Simon Fischer, Tobias Malbrecht
 *
 */
public class OperatorDocEditor extends OperatorDocViewer {

	private static final long serialVersionUID = 3341472230093161784L;
	
	private static final String[] EDIT_ACTION_NAMES = { "font-bold", "font-italic",
														"InsertOrderedList", "InsertOrderedListItem",
														"InsertUnorderedList", "InsertUnorderedListItem" };

	private final List<Action> editActions = new LinkedList<Action>();
	
	private final Action applyChanges = new ResourceAction(true, "operatorhelp.apply_changes") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			if (editWhat != null) {
				if (editWhat.equals("edit_description")) {
					OperatorDocumentation doc = getDisplayedOperator().getOperatorDescription().getOperatorDocumentation();
					if (doc != null) {
						doc.setDocumentation(stripHtmlFrame(getEditor().getText()));
					}
				} else if (editWhat.equals("edit_synopsis")) {
					OperatorDocumentation doc = getDisplayedOperator().getOperatorDescription().getOperatorDocumentation();
					if (doc != null) {
						doc.setSynopsis(stripHtmlFrame(getEditor().getText()));
					}
				} else if (editWhat.startsWith("edit_parameter_")) {
				}
				setEditEnabled(false);
				showHelptext();
			}
			
		}
	};
	
	private final Action discardChanges = new ResourceAction(true, "operatorhelp.discard_changes") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			if (editWhat != null) {
				setEditEnabled(false);
				showHelptext();
			}
			
		}
	};
	
	private final Action addExample = new ResourceAction(true, "operatorhelp.add_example") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			Process current = RapidMinerGUI.getMainFrame().getProcess();
			getDisplayedOperator().getOperatorDescription().getOperatorDocumentation().addExample(current, current.getRootOperator().getUserDescription());
			showHelptext();
		}
	};
	
	private final Action saveDocumentation = new ResourceAction(true, "operatorhelp.save_docs") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			getDisplayedOperator().getOperatorDescription().getOperatorDocumentation().getBundle().save();
		}
	};
	
	private String editWhat;
		
	public OperatorDocEditor() {
		final JToolBar toolBar = new ExtendedJToolBar();
		toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		toolBar.add(saveDocumentation);
		toolBar.add(addExample);
		toolBar.addSeparator();
		toolBar.add(applyChanges);
		toolBar.add(discardChanges);
		toolBar.addSeparator();

		Map<String,Action> actionMap = new HashMap<String,Action>();
		for (final Action action : getEditor().getActions()) {
			actionMap.put((String) action.getValue(Action.NAME), action);
		}
		for (final String editActionName : EDIT_ACTION_NAMES) {
			final Action editAction = actionMap.get(editActionName);
			editActions.add(new ResourceAction(true, "operatorhelp." + (String) editAction.getValue(Action.NAME)) {
				private static final long serialVersionUID = -8259341536856732400L;
				
				public void actionPerformed(ActionEvent e) {
					editAction.actionPerformed(e);
				}
				
			});
		}
		for (final Action action : editActions) {
			toolBar.add(action);
		}
		add(toolBar, BorderLayout.NORTH);
		setEditEnabled(false);
		
		getEditor().addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (e.getDescription().startsWith("edit_")) {
						edit(e.getDescription());
					} else if (e.getDescription().startsWith("delete_example_")) {
						int index = Integer.parseInt(e.getDescription().substring("delete_example_".length()));
						getDisplayedOperator().getOperatorDescription().getOperatorDocumentation().removeExample(index);
						showHelptext();
					}
				}
			}			
		});
	}
	
	@Override
	protected Object makeExampleFooter(int exampleIndex) {
		return super.makeExampleFooter(exampleIndex) + " <small><a href=\"delete_example_"+exampleIndex+"\">(delete)</a></small>";
	}
	
	@Override
	protected String makeSynopsisHeader() {
		return "<h4>Synopsis <small><a href=\"edit_synopsis\">(edit)</a></small></h4>";
	}
	
	@Override
	protected String makeDescriptionHeader() {
		return  "<h4>Description <small><a href=\"edit_description\">(edit)</a></h4>";
	}

	@Override
	protected String makeParameterHeader(ParameterType type) {
		return  super.makeParameterHeader(type) + "<small><a href=\"edit_parameter_"+type.getKey()+"\">(edit)</a></small>";
	}

	/** Enables/disables all actions for editing HTML text.*/
	private void setEditEnabled(boolean enabled) {
		getEditor().getCaret().setVisible(enabled);
		applyChanges.setEnabled(enabled);
		discardChanges.setEnabled(enabled);
		for (Action action : editActions) {
			action.setEnabled(enabled);
		}
	}
	
	/**
	 * 
	 * @param editWhat Either "synopsis", "description", or "parameter_KEY" where key is a parameter key.
	 */
	private void edit(String editWhat) {		
		this.editWhat = editWhat;
		String initialText = null;
		if (editWhat.equals("edit_description")) {
			initialText = getDisplayedOperator().getOperatorDescription().getLongDescriptionHTML();
		} else if (editWhat.equals("edit_synopsis")) {
			initialText = getDisplayedOperator().getOperatorDescription().getShortDescription();
		} else if (editWhat.startsWith("edit_parameter_")) {
			String key = editWhat.substring("edit_parameter_".length());
			ParameterType type = getDisplayedOperator().getParameters().getParameterType(key);
			if (type != null) {
				initialText = type.getDescription();
			}
		}

		if (initialText != null) {
			getEditor().setText("<html>"+initialText+"</html>");
			getEditor().setEditable(true);
			setEditEnabled(true);
			getEditor().requestFocus(true);
			getEditor().setCaretPosition(0);
		}
	}
	
	private static String stripHtmlFrame(String html) {
		int bodyStart = html.indexOf("<body>");
		if (bodyStart != -1) {
			html = html.substring(bodyStart + "<body>".length());
		}
		int bodyEnd = html.lastIndexOf("</body>");
		if (bodyEnd != -1) {
			html = html.substring(0, bodyEnd);
		}		
		return html;
	}

}
