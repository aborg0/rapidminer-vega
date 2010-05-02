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
package com.rapidminer.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.BugReport;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Tools;


/**
 * This dialog is shown in cases where a non-user error occured and the user
 * decided to send a bugreport. Collects all necessary data for bug fixing and
 * creates a zip file from the data.
 * 
 * @author Simon Fischer, Ingo Mierswa, Tobias Malbrecht
 */
public class BugAssistant extends ButtonDialog {

	private static final long serialVersionUID = 8379605320787188372L;

	private static final String TEXT_INSTRUCTIONS = "Enter a brief description of what happened into this text field. Please also describe the purpose of your process definition because this cannot always be concluded trivially from the process setup.";

	private final JTextArea message = new JTextArea(TEXT_INSTRUCTIONS, 5, 20);

	private final JList attachments = new JList(new DefaultListModel());
	
	public BugAssistant(final Throwable exception) {
		super("send_bugreport", true);
		Collection<AbstractButton> buttons = new LinkedList<AbstractButton>();
		
		JPanel panel = new JPanel(new BorderLayout());

		final JPanel addressPanel = new JPanel(new BorderLayout(GAP, GAP));
		JLabel nameLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".e_mail.label"));
		addressPanel.add(nameLabel, BorderLayout.WEST);

		final JTextField name = new JTextField(15);
		name.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".e_mail.tip"));
		addressPanel.add(name, BorderLayout.CENTER);
		panel.add(addressPanel, BorderLayout.NORTH);

		final JPanel mailPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();

		message.setLineWrap(true);
		message.setWrapStyleWord(true);
		JScrollPane messagePane = new ExtendedJScrollPane(message);
		messagePane.setBorder(createBorder());
		messagePane.setPreferredSize(new Dimension(400, 300));
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(GAP, 0, 0, GAP);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.9;
		c.weighty = 1;
		//messagePane.setPreferredSize(new Dimension((int) (getPreferredSize().getWidth() * 0.7), 50));
		mailPanel.add(messagePane, c);
		
		c.insets = new Insets(GAP, 0, 0, 0);
		c.gridx = 1;
		c.weightx = 0.1;
		c.weighty = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		attachments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane attachmentPane = new JScrollPane(attachments);
		attachmentPane.setBorder(createBorder());
		attachmentPane.setPreferredSize(new Dimension(150, 300));
		mailPanel.add(attachmentPane, c);
		//attachmentPane.setPreferredSize(new Dimension((int) (getPreferredSize().getWidth() * 0.3), 50));
		panel.add(mailPanel, BorderLayout.CENTER);
		
		buttons.add(new JButton(new ResourceAction("send_bugreport.add_file") {
			private static final long serialVersionUID = 5152169309271935854L;

			@Override
			public void actionPerformed(ActionEvent e) {
				File file = SwingTools.chooseFile(null, null, true, null, null);
				if (file != null) {
					((DefaultListModel) attachments.getModel()).addElement(file);					
				}
			}
			
		}));
		buttons.add(new JButton(new ResourceAction("send_bugreport.remove_file") {
			private static final long serialVersionUID = 5353693430346577972L;

			public void actionPerformed(ActionEvent e) {
				if (attachments.getSelectedIndex() >= 0) {
					((DefaultListModel) attachments.getModel()).remove(attachments.getSelectedIndex());
				}				
			}
		}));
		buttons.add(new JButton(new ResourceAction("send_bugreport.submit") {
			private static final long serialVersionUID = -4559762951458936715L;

			public void actionPerformed(ActionEvent e) {
				String email = name.getText().trim();
				if (email.length() == 0) {
					SwingTools.showVerySimpleErrorMessage("enter_email");
					return;
				}
				File file = SwingTools.chooseFile(null, new File("bugreport.zip"), false, false, ".zip", "zip archives");
				if (file != null) {
					try {
						ListModel model = attachments.getModel();
						File[] attachments = new File[model.getSize()];
						for (int i = 0; i < attachments.length; i++) {
							attachments[i] = (File) model.getElementAt(i);
						}
						BugReport.createBugReport(file, exception, "From: " + name.getText() + Tools.getLineSeparator() + "Date: " + new java.util.Date() + Tools.getLineSeparator() + Tools.getLineSeparator() + message.getText(), RapidMinerGUI.getMainFrame().getProcess(), RapidMinerGUI.getMainFrame().getMessageViewer().getLogMessage(), attachments);
						dispose();
					} catch (Throwable t) {
//						SwingTools.showSimpleErrorMessage("Cannot create report file!", t);
						SwingTools.showVerySimpleErrorMessage("bugreport_creation_failed");
					}
				}
			}
		}));
		buttons.add(makeCancelButton());

		message.setSelectionStart(0);
		message.setSelectionEnd(TEXT_INSTRUCTIONS.length() - 1);

		layoutDefault(panel, LARGE, buttons);
	}
}
