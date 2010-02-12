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
package com.rapidminer.repository.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import com.michaelbaranov.microba.calendar.DatePicker;
import com.rapid_i.repository.wsimport.ExecutionResponse;
import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.processeditor.ProcessContextEditor;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.ResourceTabbedPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryConstants;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;

/**
 * A dialog that lets the user run a process on a remote server, either now, at
 * a fixed later point of time or scheduled by a cron expression.
 * 
 * @author Simon Fischer
 * 
 */
public class RunRemoteDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;

	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(); //new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

	private final DatePicker dateField = new DatePicker(new Date(), DATE_FORMAT); // new
																// JTextField(DATE_FORMAT.format(new
																// Date()), 30);
	private final JTextField cronField = new JTextField(30);
	private final JComboBox repositoryBox = new JComboBox();
	private final JLabel dateLabel = new ResourceLabel("runremotedialog.date");
	private final JLabel cronLabel = new ResourceLabel("runremotedialog.cronexpression");
	private final JCheckBox startBox = new JCheckBox(new ResourceAction("runremotedialog.cronstart") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			enableComponents();
		}
	});
	private final DatePicker startField = new DatePicker(new Date(), DATE_FORMAT);
	private final DatePicker endField   = new DatePicker(new Date(), DATE_FORMAT);
	private final JCheckBox endBox = new JCheckBox(new ResourceAction("runremotedialog.cronend") {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			enableComponents();
		}
	});
	private final JTextField processField = new JTextField(30);

	private JRadioButton nowButton;

	private JRadioButton onceButton;

	private JRadioButton cronButton;;

	private final ResourceTabbedPane tabs = new ResourceTabbedPane("runremotedialog");
	
	public RunRemoteDialog(Process process) {
		super("runremotedialog", true);
		setModal(true);

		dateField.setStripTime(false);
		dateField.setKeepTime(true);
		startField.setStripTime(false);
		startField.setKeepTime(true);
		endField.setStripTime(false);
		endField.setKeepTime(true);
		
		startBox.setSelected(false);
		endBox.setSelected(false);

		ProcessLocation processLocation = process.getProcessLocation();
		if ((processLocation != null) && (processLocation instanceof RepositoryProcessLocation)) {			
			processField.setText(((RepositoryProcessLocation) processLocation).getRepositoryLocation().getPath());
		} else {
			processField.setText("");
		}
		processField.selectAll();

		final JButton okButton = makeOkButton();
		final JButton cancelButton = makeCancelButton();
		
		repositoryBox.setModel(new DefaultComboBoxModel(RepositoryManager.getInstance(null).getRemoteRepositories().toArray()));
		repositoryBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				okButton.setEnabled(repositoryBox.getSelectedItem() != null);
			}
		});
		RepositoryManager.getInstance(null).addObserver(new Observer<Repository>() {
			@Override
			public void update(Observable<Repository> observable, Repository arg) {
				repositoryBox.setModel(new DefaultComboBoxModel(RepositoryManager.getInstance(null).getRemoteRepositories().toArray()));
				if ((arg != null) && (arg instanceof RemoteRepository)) {
					repositoryBox.setSelectedItem(arg);
				}
				pack();
			}
		}, true);
		
		JPanel schedulePanel = makeSchedulePanel();
		JComponent contextPanel = new ProcessContextEditor(null);
		tabs.addTabI18N("schedule", schedulePanel);
		tabs.addTabI18N("context", contextPanel);
		
		// Buttons		
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(nowButton);
		buttonGroup.add(onceButton);
		buttonGroup.add(cronButton);

		nowButton.setSelected(true);
		
		layoutDefault(tabs, NORMAL, okButton, cancelButton);
		enableComponents();
		okButton.setEnabled(repositoryBox.getSelectedItem() != null);
	}
	
	private JPanel makeSchedulePanel() {		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.weighty = 0;
		
		// Repository
		JPanel repositoryPanel = new JPanel(new GridBagLayout());
		c.insets = new Insets(0, GAP, 0, GAP);
		JLabel label = new ResourceLabel("runremotedialog.repository");
		label.setLabelFor(repositoryBox);
		c.insets = new Insets(GAP, GAP, GAP, GAP);
		repositoryPanel.add(label, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.insets = new Insets(0, GAP, GAP, 0);
		repositoryPanel.add(repositoryBox, c);
		JButton addRepositoryButton = new JButton(RepositoryBrowser.ADD_REPOSITORY_ACTION);
		addRepositoryButton.setText("");
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(0, 0, GAP, GAP);
		repositoryPanel.add(addRepositoryButton, c);

		// Process
		label = new ResourceLabel("runremotedialog.process_location");
		label.setLabelFor(processField);
		c.weightx = 1;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		repositoryPanel.add(label, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.insets = new Insets(0, GAP, GAP, 0);
		repositoryPanel.add(processField, c);
				
		JButton selectButton = new JButton(new ResourceAction(true, "repository_select_location") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				String selected = RepositoryLocationChooser.selectLocation(null, RunRemoteDialog.this);
				if (selected != null) {
					try {
						RepositoryLocation location = new RepositoryLocation(selected);
						Repository repository = location.getRepository();
						String relative = location.getPath();
						repositoryBox.setSelectedItem(repository);
						processField.setText(relative);
					} catch (Exception ex) {
						processField.setText(selected);
					}
					processField.selectAll();
				}
			}
		});
		selectButton.setText("");
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(0, 0, GAP, GAP);
		repositoryPanel.add(selectButton, c);
		
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		JPanel dummy = new JPanel();
		repositoryPanel.add(dummy, c);
		
		
		
		// RIGHT SIDE
		// Now
		JPanel schedPanel = new JPanel(new GridBagLayout());
		nowButton = new JRadioButton(new ResourceAction("runremotedialog.now") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				enableComponents();
			}
		});
		c.weightx = 1;
		c.insets = new Insets(GAP, GAP, GAP, GAP);
		c.gridwidth = GridBagConstraints.REMAINDER;
		schedPanel.add(nowButton, c);

		// Once
		onceButton = new JRadioButton(new ResourceAction("runremotedialog.once") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				enableComponents();
			}
		});
		c.insets = new Insets(3 * GAP, GAP, GAP, GAP);
		c.gridwidth = GridBagConstraints.REMAINDER;
		schedPanel.add(onceButton, c);

		dateLabel.setLabelFor(dateField);
		c.insets = new Insets(0, 8 * GAP, 0, GAP);
		c.gridwidth = GridBagConstraints.REMAINDER;
		schedPanel.add(dateLabel, c);
		c.insets = new Insets(0, 8 * GAP, GAP, GAP);
		schedPanel.add(dateField, c);

		// Cron
		cronButton = new JRadioButton(new ResourceAction("runremotedialog.cron") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				enableComponents();
			}
		});
		c.insets = new Insets(3 * GAP, GAP, GAP, GAP);
		c.gridwidth = GridBagConstraints.REMAINDER;
		schedPanel.add(cronButton, c);

		c.insets = new Insets(0, 8 * GAP, 0, GAP);
		cronLabel.setLabelFor(cronField);
		schedPanel.add(cronLabel, c);
		c.insets = new Insets(0, 8 * GAP, GAP, 0);
		c.gridwidth = GridBagConstraints.RELATIVE;
		schedPanel.add(cronField, c);
		
		c.insets = new Insets(0, 0, GAP, GAP);
//		ResourceLabel cronHelp = new ResourceLabel("cron_help");
		JButton cronHelpButton = new JButton(new ResourceAction(true, "cron_help") {
			private static final long serialVersionUID = 1L;
			{
				putValue(Action.NAME, "");
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				SwingTools.showMessageDialog("cron_long_help");				
			}
		});
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		schedPanel.add(cronHelpButton, c);
		c.weightx = 1;
		
		c.insets = new Insets(GAP, 8 * GAP, 0, GAP);
		schedPanel.add(startBox, c);
		c.insets = new Insets(0, 8 * GAP, GAP, GAP);
		schedPanel.add(startField, c);

		c.insets = new Insets(GAP, 8 * GAP, 0, GAP);
		schedPanel.add(endBox, c);
		c.insets = new Insets(0, 8 * GAP, GAP, GAP);
		schedPanel.add(endField, c);

		JPanel panel = new JPanel(createGridLayout(1, 2));
		panel.add(repositoryPanel);
		panel.add(schedPanel);
		return panel;
	}

	private void enableComponents() {
		dateLabel.setEnabled(onceButton.isSelected());
		dateField.setEnabled(onceButton.isSelected());
		cronLabel.setEnabled(cronButton.isSelected());
		cronField.setEnabled(cronButton.isSelected());
		startBox.setEnabled(cronButton.isSelected());
		endBox.setEnabled(cronButton.isSelected());
		startField.setEnabled(cronButton.isSelected() && startBox.isSelected());
		endField.setEnabled(cronButton.isSelected() && endBox.isSelected());
	}

	public static void showDialog(Process process) {
		RunRemoteDialog d = new RunRemoteDialog(process);
		d.setVisible(true);
	}

	@Override
	public void ok() {
		RemoteRepository repos = (RemoteRepository) repositoryBox.getSelectedItem();
		if (repos != null) {
			String location = processField.getText();
			ExecutionResponse response;
			if (nowButton.isSelected()) {
				try {
					response = repos.getProcessService().executeProcessSimple(location, null);
				} catch (RepositoryException e) {
					SwingTools.showSimpleErrorMessage("error_connecting_to_server", e);
					return;
				}
			} else if (onceButton.isSelected()) {
				try {
					Date date = dateField.getDate();
					response = repos.getProcessService().executeProcessSimple(location, XMLTools.getXMLGregorianCalendar(date));
				} catch (DatatypeConfigurationException e) {
					SwingTools.showSimpleErrorMessage("cannot_parse_date", e);
					return;
				} catch (RepositoryException e) {
					SwingTools.showSimpleErrorMessage("error_connecting_to_server", e);
					return;
				}
			} else if (cronButton.isSelected()) {
				try {
					XMLGregorianCalendar start = startBox.isSelected() ? XMLTools.getXMLGregorianCalendar(startField.getDate()) : null;
					XMLGregorianCalendar end = endBox.isSelected() ? XMLTools.getXMLGregorianCalendar(endField.getDate()) : null;
					response = repos.getProcessService().executeProcessCron(location, cronField.getText(), start, end);				
				} catch (DatatypeConfigurationException e) {
					SwingTools.showSimpleErrorMessage("cannot_parse_date", e);
					return;
				} catch (RepositoryException e) {
					SwingTools.showSimpleErrorMessage("error_connecting_to_server ", e);
					return;
				}
			} else {
				throw new RuntimeException("No radio button selected. (This cannot happen)");
			}
			if (response.getStatus() != RepositoryConstants.OK) {
				SwingTools.showSimpleErrorMessage("run_proc_remote", response.getErrorMessage());
			} else {
				dispose();
				JOptionPane.showMessageDialog(this, "Process will first run on: " + response.getFirstExecution(), "Process scheduled", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
}
