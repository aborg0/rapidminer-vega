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
package com.rapidminer.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.documentation.ExampleProcess;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

/** Displays HTML help text for an operator
 * 
 * @author Simon Fischer
 *
 */
public class OperatorDocViewer extends JPanel implements Dockable, ProcessEditor {

	private static final long serialVersionUID = 1L;

	private final ExtendedHTMLJEditorPane editor = new ExtendedHTMLJEditorPane("text/html", "<html></html>");

	private Operator displayedOperator;

	public OperatorDocViewer() {
		super();
		setLayout(new BorderLayout());
		JScrollPane scrollPane = new ExtendedJScrollPane(editor);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);
		setSelection(Collections.<Operator>emptyList());
		editor.installDefaultStylesheet();

		getEditor().addHyperlinkListener(new HyperlinkListener() {			
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (!RMUrlHandler.handleUrl(e.getDescription())) {
						if (e.getDescription().startsWith("show_example_")) {
							int index = Integer.parseInt(e.getDescription().substring("show_example_".length()));
							ExampleProcess example = getDisplayedOperator().getOperatorDescription().getOperatorDocumentation().getExamples().get(index);
							RapidMinerGUI.getMainFrame().setProcess(example.getProcess(), true);
						}
					}
				}
			}
		});
	}

	protected JEditorPane getEditor() {
		return editor;
	}

	protected Operator getDisplayedOperator() {
		return displayedOperator;
	}

	public void setDisplayedOperator(Operator operator) {
		this.displayedOperator = operator;
		showHelptext();
	}

	protected void showHelptext() {
		editor.setEditable(false);
		if (displayedOperator == null) {
			editor.setText("<html></html>");
		} else {			
			OperatorDescription descr = displayedOperator.getOperatorDescription();			
			StringBuilder buf = new StringBuilder("<html>");
			buf.append("<table cellpadding=0 cellspacing=0><tr><td>");

			String iconName = "icons/24/" + displayedOperator.getOperatorDescription().getIconName();
			URL resource = Tools.getResource(iconName);
			if (resource != null) {
				buf.append("<img src=\"" + resource + "\"/>");
			}

			buf.append("</td><td style=\"padding-left:4px;\">"); 
			buf.append("<h2>" + descr.getName());
			String wikiName;
			try {
				wikiName = URLEncoder.encode(descr.getName(), "UTF-8");
				buf.append(" <small><a href=\"http://rapid-i.com/wiki/index.php?title=").append(wikiName).append("\">(Wiki)</a></small>");
			} catch (UnsupportedEncodingException e) {
				LogService.getRoot().log(Level.WARNING, "Failed to URL-encode operator name: "+descr.getName()+": "+e, e);
			}
			buf.append("</h2>");
			buf.append("</td></tr></table>");
			//#"+Integer.toHexString(SwingTools.RAPID_I_ORANGE.getRGB()).substring(0,6)+"
			buf.append("<hr noshade=\"true\"/><br/>");
			//System.out.println("<hr color=\"#"+Integer.toHexString(SwingTools.RAPID_I_ORANGE.getRGB()).substring(0,6)+"\"/>");
			buf.append(makeSynopsisHeader());
			buf.append("<p>");
			buf.append(descr.getShortDescription());
			buf.append("</p>");
			buf.append("</p><br/>");
			buf.append(makeDescriptionHeader());
			String descriptionText = descr.getLongDescriptionHTML();
			if (descriptionText != null) {
				if (!descriptionText.trim().startsWith("<p>")) {
					buf.append("<p>");
				}
				buf.append(descriptionText);
				if (!descriptionText.trim().endsWith("</p>")) {
					buf.append("</p>");
				}
				buf.append("<br/>");
			}		
			appendPorts(displayedOperator.getInputPorts(), "Input", null, buf);
			appendPorts(displayedOperator.getOutputPorts(), "Output", "outPorts", buf);
			Parameters parameters = displayedOperator.getParameters();
			if (parameters.getKeys().size() > 0) {
				buf.append("<h4>Parameters</h4><dl>");
				for (String key : parameters.getKeys()) {
					ParameterType type = parameters.getParameterType(key);
					if (type == null) {
						LogService.getRoot().warning("Unknown parameter key: " + displayedOperator.getName() + "# " +key);
						continue;
					}
					buf.append("<dt>");
					if (type.isExpert()) {
						buf.append("<i>");
					}
					//if (type.isOptional()) {
					buf.append(makeParameterHeader(type));					
					//} else {
					//buf.append("<strong>");
					//buf.append(makeParameterHeader(type));
					//buf.append("</strong>");
					//}
					if (type.isExpert()) {
						buf.append("</i>");
					}
					buf.append("</dt><dd style=\"padding-bottom:10px\">");
					// description
					buf.append(" ");
					buf.append(type.getDescription() + "<br/><font color=\"#777777\" size=\"-2\">");
					if (type.getDefaultValue() != null) {
						if (!type.toString(type.getDefaultValue()).equals("")) {
							buf.append(" Default value: ");
							buf.append(type.toString(type.getDefaultValue()));
							buf.append("<br/>");
						}
					}
					if (type.isExpert()) {
						buf.append("Expert parameter<br/>");
					}
					// conditions
					if (type.getDependencyConditions().size() > 0) {
						buf.append("Depends on:<ul class=\"param_dep\">");
						for (ParameterCondition condition: type.getDependencyConditions()) {
							buf.append("<li>");
							buf.append(condition.toString());
							buf.append("</li>");
						}
						buf.append("</ul>");
					}
					buf.append("</small></dd>");
				}
				buf.append("</dl>");
			}

			if (!descr.getOperatorDocumentation().getExamples().isEmpty()) {
				buf.append("<h4>Examples</h4><ul>");
				int i = 0;
				for (ExampleProcess exampleProcess : descr.getOperatorDocumentation().getExamples()) {					
					buf.append("<li>");					
					buf.append(exampleProcess.getComment());
					buf.append(makeExampleFooter(i));
					buf.append("</li>");
					i++;
				}
				buf.append("</ul>");
			}

			buf.append("</html>");
			editor.setText(buf.toString());
			editor.setCaretPosition(0);
		}
	}

	protected Object makeExampleFooter(int exampleIndex) {
		return "<br/><a href=\"show_example_"+exampleIndex+"\">Show example process</a>.";
	}

	protected String makeSynopsisHeader() {
		return "<h4>Synopsis</h4>";
	}

	protected String makeDescriptionHeader() {
		return "<h4>Description</h4>";
	}

	protected String makeParameterHeader(ParameterType type) {
		return  type.getKey().replace('_',' ');
	}

	private void appendPorts(Ports<? extends Port> ports, String title, String ulClass, StringBuilder buf) {
		//buf.append("<dl><dt>Input:<dt></dt><dd>");
		if (ports.getNumberOfPorts() > 0) {
			buf.append("<h4>" + title + "</h4><ul class=\"ports\">");
			for (Port port : ports.getAllPorts()) {
				if (ulClass != null)
					buf.append("<li class=\"" + ulClass + "\"><strong>");
				else
					buf.append("<li><strong>");				
				buf.append(port.getName());
				buf.append("</strong>");
				if (port.getDescription() != null && port.getDescription().length() > 0) {
					buf.append(": ");
					buf.append(port.getDescription());
				}
				buf.append("</li>");
			}
			buf.append("</ul><br/>");
		}
	}

	public void setSelection(List<Operator> selection) {		
		if (selection.isEmpty()) {
			setDisplayedOperator(null);			
		} else {
			setDisplayedOperator(selection.get(0));
		}
	}

	public static final String OPERATOR_HELP_DOCK_KEY = "operator_help";
	private final DockKey DOCK_KEY = new ResourceDockKey(OPERATOR_HELP_DOCK_KEY);
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}
	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	@Override
	public void processChanged(Process process) {
	}

	@Override
	public void processUpdated(Process process) {
	}

	public static OperatorDocViewer instantiate() {
		if ("true".equals(System.getProperty(RapidMiner.PROPERTY_DEVELOPER_MODE))) {
			return new OperatorDocEditor();
		} else {
			return new OperatorDocViewer();
		}
	}
}
