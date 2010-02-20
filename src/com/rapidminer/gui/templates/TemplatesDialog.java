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
package com.rapidminer.gui.templates;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;


/**
 * The manage templates dialog assists the user in managing his created process templates.
 * Template processes are saved in the local &quot;.rapidminer&quot;
 * directory of the user. The name, description and additional parameters to set
 * can be specified by the user. In this dialog he can also delete one of the templates.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class TemplatesDialog extends ButtonDialog {

	private static final long serialVersionUID = 1428487062393160289L;

	private final JList templateList = new JList();
	{
		templateList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1496872314541527746L;

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); 
				if (value instanceof Template) {
					Template template = (Template) value;
					label.setText("<html><strong>" + template.getName() + "</strong><div width=\"600\">" + template.getDescription() + "</div></html>");
					label.setIcon(SwingTools.createIcon("16/package.png"));
					label.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
					label.setVerticalAlignment(SwingConstants.TOP);
					label.setVerticalTextPosition(SwingConstants.TOP);
				}
				return label;
			}
			
		});
		templateList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				fireStateChanged();
			}
		});
		templateList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					actOnDoubleClick();
				}
			}
		});
	}

	private final Map<String, Template> templateMap = new TreeMap<String, Template>();
	
	protected final ExtendedJScrollPane listPane = new ExtendedJScrollPane(templateList);
	{
		listPane.setBorder(createBorder());
	}
	
	protected transient final Action DELETE_ACTION = new ResourceAction("delete_template") {
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent e) {
			delete();
		}
	};
	
	private void readUserTemplates() {
		File[] templateFiles = ParameterService.getUserRapidMinerDir().listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.getName().endsWith(".template");
			}
		});
		for (int i = 0; i < templateFiles.length; i++) {
			try {
				Template template = new Template(templateFiles[i]);
				templateMap.put(template.getName(), template);
			} catch (Exception e) {
				SwingTools.showSimpleErrorMessage("cannot_load_template_file", e, templateFiles[i]);
			}
		}
	}
	
	private void readSystemTemplates() {
		InputStream in = TemplatesDialog.class.getResourceAsStream("/com/rapidminer/resources/templates/Templates");
		if (in != null) {
			BufferedReader reader;
			try {
				reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				LogService.getRoot().warning("Resource /com/rapidminer/resources/templates/Templates cannot be read. UTF-8 not supported.");
				return;
			}
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					try {
						String templateName = "/com/rapidminer/resources/templates/"+line.trim()+".template";
						InputStream tin = TemplatesDialog.class.getResourceAsStream(templateName);
						if (tin != null) {
							Template template = new Template(tin);
							templateMap.put(template.getName(), template);
						} else {
							LogService.getRoot().warning("Cannot find template "+templateName);
						}
					} catch (IOException e) {
						SwingTools.showSimpleErrorMessage("cannot_load_template_file", e, line);
					}
				}
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "Error reading template list: "+e, e);
			}		
		} else {
			LogService.getRoot().warning("Resource com/rapidminer/resources/templates/Templates not found.");
		}
	}
	


	public TemplatesDialog(int templateSource) {
		super("manage_templates", true);
		switch (templateSource) {
		case Template.PREDEFINED:
			readSystemTemplates();
			break;
		case Template.USER_DEFINED:
			readUserTemplates();
			break;
		case Template.ALL:
		default:
			readSystemTemplates();
			readUserTemplates();
			break;
		}
		update();
	}
	
	public JPanel createTemplateManagementPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(listPane, BorderLayout.CENTER);
		return panel;
	}
	
	protected void actOnDoubleClick() {}

	private void update() {
		Vector<Template> data = new Vector<Template>();
		Iterator<Template> i = templateMap.values().iterator();
		while (i.hasNext()) {
			Template template = i.next();
			data.add(template);
		}
		templateList.setListData(data);
		repaint();
	}

	private void delete() {
		Object[] selection = templateList.getSelectedValues();
		for (int i = 0; i < selection.length; i++) {
			String name = ((Template) selection[i]).getName();
			Template template = templateMap.remove(name);
			File templateFile = template.getFile();
			File expFile = template.getProcessFile();
			boolean deleteResult = templateFile.delete();
			if (!deleteResult)
				LogService.getGlobal().logWarning("Unable to delete template file: " + templateFile);
			deleteResult = expFile.delete();
			if (!deleteResult)
				LogService.getGlobal().logWarning("Unable to delete template experiment file: " + expFile);
		}
		update();
	}
	
	public Template getSelectedTemplate() {
		return (Template) templateList.getSelectedValue();
	}
}
