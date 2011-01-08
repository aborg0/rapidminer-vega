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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.BugReport;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XmlRpcHandler;


/**
 * This dialog is shown in cases where a non-user error occured and the user
 * decided to send a bugreport. Collects all necessary data for bug fixing and
 * sends it to the RapidMiner BugZilla tracker.
 * 
 * @author Marco Boeck
 */
public class BugZillaAssistant extends ButtonDialog {
	
	private XmlRpcClient rpcClient = null;
	
	private final String descriptionText = I18N.getMessage(I18N.getGUIBundle(), getKey() + ".description.text");

	private final JTextArea descriptionField = new JTextArea(descriptionText, 5, 20);

	private final JList attachments = new JList(new DefaultListModel());
	
	private JButton submitButton;
	
	private int returnVal;
	
	private static final long serialVersionUID = 8379605320787188372L;
	
	
	public BugZillaAssistant(ProgressThread thread, final Throwable exception, final XmlRpcClient client) throws XmlRpcException {
		super("send_bugreport", true);
		rpcClient = client;
		thread.getProgressListener().setCompleted(35);
		if (thread.isCancelled()) {
			return;
		}
		// gather information to fill out combo boxes
		Object[] compVals, severityVals, platformVals, osVals;
		// components
		Map<String, String> valQueryMap = new HashMap<String, String>();
		valQueryMap.put("field", "component");
		//TODO: change to 2
		valQueryMap.put("product_id", "4");
		Map resultMap = (Map)rpcClient.execute("Bug.legal_values", new Object[]{ valQueryMap });
		compVals = (Object[])resultMap.get("values");
		thread.getProgressListener().setCompleted(50);
		if (thread.isCancelled()) {
			return;
		}
		// severity
		valQueryMap = new HashMap<String, String>();
		valQueryMap.put("field", "severity");
		valQueryMap.put("product_id", "2");
		resultMap = (Map)rpcClient.execute("Bug.legal_values", new Object[]{ valQueryMap });
		severityVals = (Object[])resultMap.get("values");
		thread.getProgressListener().setCompleted(65);
		if (thread.isCancelled()) {
			return;
		}
		// platform
		valQueryMap = new HashMap<String, String>();
		valQueryMap.put("field", "platform");
		valQueryMap.put("product_id", "2");
		resultMap = (Map)rpcClient.execute("Bug.legal_values", new Object[]{ valQueryMap });
		platformVals = (Object[])resultMap.get("values");
		thread.getProgressListener().setCompleted(80);
		if (thread.isCancelled()) {
			return;
		}
		// operating system
		valQueryMap = new HashMap<String, String>();
		valQueryMap.put("field", "op_sys");
		valQueryMap.put("product_id", "2");
		resultMap = (Map)rpcClient.execute("Bug.legal_values", new Object[]{ valQueryMap });
		osVals = (Object[])resultMap.get("values");
		thread.getProgressListener().setCompleted(95);
		if (thread.isCancelled()) {
			return;
		}
		
		Collection<AbstractButton> buttons = new LinkedList<AbstractButton>();
		
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		final JPanel loginPanel = new JPanel();
		loginPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(GAP, 0, 0, GAP);
		
		JLabel loginLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".login_e_mail.label"));
		loginPanel.add(loginLabel, gbc);

		final JTextField loginName = new JTextField(15);
		loginName.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".login_e_mail.tip"));
		gbc.gridx = 1;
		gbc.weightx = 1;
		loginPanel.add(loginName, gbc);
		
		JLabel passwordLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".login_password.label"));
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0;
		loginPanel.add(passwordLabel, gbc);
		
		final JPasswordField loginPassword = new JPasswordField(15);
		loginPassword.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".login_password.tip"));
		gbc.gridx = 1;
		gbc.weightx = 1;
		loginPanel.add(loginPassword, gbc);
		
		final JCheckBox useAnonymousLogin = new JCheckBox();
		useAnonymousLogin.setText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".login_as_anonymous.label"));
		useAnonymousLogin.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".login_as_anonymous.tip"));
		useAnonymousLogin.setSelected(false);
		useAnonymousLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
		useAnonymousLogin.setMinimumSize(useAnonymousLogin.getPreferredSize());
		useAnonymousLogin.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					loginName.setEditable(false);
					loginPassword.setEditable(false);
				} else {
					loginName.setEditable(true);
					loginPassword.setEditable(true);
				}
			}
		});
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0;
		loginPanel.add(useAnonymousLogin, gbc);
		
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.gridy = 3;
		gbc.insets = new Insets(GAP*2, 0, GAP, 0);
		loginPanel.add(new JSeparator(), gbc);
		panel.add(loginPanel);
		
		final JPanel detailPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(GAP, 0, 0, GAP);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weighty = 0;
		detailPanel.add(new JLabel(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".component.label") + ":"), c);
		
		c.gridx = 1;
		c.gridy = 0;
		final JComboBox compBox = new JComboBox(compVals);
		compBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".component.tip"));
		compBox.setSelectedItem("Vega: Processes, data flow  and meta data");
		detailPanel.add(compBox, c);
		
		c.gridx = 2;
		c.gridy = 0;
		detailPanel.add(new JLabel(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".severity.label") + ":"), c);
		
		c.gridx = 3;
		c.gridy = 0;
		final JComboBox severityBox = new JComboBox(severityVals);
		severityBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".severity.tip"));
		severityBox.setSelectedItem("normal");
		detailPanel.add(severityBox, c);
		
		c.gridx = 0;
		c.gridy = 1;
		detailPanel.add(new JLabel(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".platform.label") + ":"), c);
		
		c.gridx = 1;
		c.gridy = 1;
		final JComboBox platformBox = new JComboBox(platformVals);
		platformBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".platform.tip"));
		detailPanel.add(platformBox, c);
		
		c.gridx = 2;
		c.gridy = 1;
		detailPanel.add(new JLabel(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".os.label") + ":"), c);
		
		c.gridx = 3;
		c.gridy = 1;
		final JComboBox osBox = new JComboBox(osVals);
		osBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".os.tip"));
		String os = System.getProperty("os.name");
		if (os.toLowerCase(Locale.ENGLISH).contains("windows")) {
			osBox.setSelectedItem("Windows");
			platformBox.setSelectedItem("PC");
		} else if (os.toLowerCase(Locale.ENGLISH).contains("linux")) {
			osBox.setSelectedItem("Linux");
			platformBox.setSelectedItem("PC");
		} else if (os.toLowerCase(Locale.ENGLISH).contains("mac")) {
			osBox.setSelectedItem("Mac OS");
			platformBox.setSelectedItem("Macintosh");
		} else {
			osBox.setSelectedItem("Other");
			platformBox.setSelectedItem("Other");
		}
		detailPanel.add(osBox, c);
		
		c.gridx = 4;
		c.gridy = 0;
		c.weightx = 1;
		detailPanel.add(new JLabel(), c);
		
		c.gridy = 1;
		detailPanel.add(new JLabel(), c);
		
		c.gridwidth = 5;
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(GAP*2, 0, GAP, 0);
		detailPanel.add(new JSeparator(), c);
		panel.add(detailPanel);

		final JPanel mailPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();

		
		c.gridx = 0;
		c.insets = new Insets(GAP, 0, 0, GAP);
		c.weighty = 0;
		final JCheckBox addProcessCheckBox = new JCheckBox();
		addProcessCheckBox.setText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".add_process_xml.label"));
		addProcessCheckBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".add_process_xml.tip"));
		addProcessCheckBox.setSelected(false);
		addProcessCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		mailPanel.add(addProcessCheckBox, c);
		
		c.gridx = 1;
		c.insets = new Insets(GAP, 0, 0, GAP);
		c.weighty = 0;
		final JCheckBox addSysPropsCheckBox = new JCheckBox();
		addSysPropsCheckBox.setText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".add_system_props.label"));
		addSysPropsCheckBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".add_system_props.tip"));
		addSysPropsCheckBox.setSelected(false);
		addSysPropsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		mailPanel.add(addSysPropsCheckBox, c);
		
		c.weightx = 0.9;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.fill = GridBagConstraints.BOTH;
		c.gridy = 1;
		mailPanel.add(new JLabel(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".summary.label") + ":"), c);
		
		final JTextField summaryField = new JTextField(15);
		summaryField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".summary.tip"));
		c.gridy = 2;
		mailPanel.add(summaryField, c);
		
		c.gridy = 3;
		mailPanel.add(new JLabel(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".description.label") + ":"), c);
		
		descriptionField.setLineWrap(true);
		descriptionField.setWrapStyleWord(true);
		descriptionField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				if (descriptionField.getText().equals(descriptionText)) {
					descriptionField.setText("");
					descriptionField.removeFocusListener(this);
				}
			}
			
		});
		descriptionField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".description.tip"));
		JScrollPane descriptionPane = new ExtendedJScrollPane(descriptionField);
		descriptionPane.setBorder(createBorder());
		descriptionPane.setPreferredSize(new Dimension(400, 400));
		c.gridy = 4;
		c.weighty = 1;
		mailPanel.add(descriptionPane, c);
		//TODO: Add again when BugZilla version 4.x is used which supports attachements
//		c.insets = new Insets(GAP, 0, 0, 0);
//		c.gridx = 1;
//		c.weightx = 0.1;
//		c.weighty = 1;
//		c.gridwidth = GridBagConstraints.REMAINDER;
//		attachments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		JScrollPane attachmentPane = new JScrollPane(attachments);
//		attachmentPane.setBorder(createBorder());
//		attachmentPane.setPreferredSize(new Dimension(150, 400));
//		mailPanel.add(attachmentPane, c);
		panel.add(mailPanel);
		
//		buttons.add(new JButton(new ResourceAction("send_bugreport.add_file") {
//			private static final long serialVersionUID = 5152169309271935854L;
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				File file = SwingTools.chooseFile(null, null, true, null, null);
//				if (file != null) {
//					((DefaultListModel) attachments.getModel()).addElement(file);					
//				}
//			}
//			
//		}));
//		buttons.add(new JButton(new ResourceAction("send_bugreport.remove_file") {
//			private static final long serialVersionUID = 5353693430346577972L;
//
//			public void actionPerformed(ActionEvent e) {
//				if (attachments.getSelectedIndex() >= 0) {
//					((DefaultListModel) attachments.getModel()).remove(attachments.getSelectedIndex());
//				}				
//			}
//		}));
		submitButton = new JButton(new ResourceAction("send_bugreport.submit") {
			private static final long serialVersionUID = -4559762951458936715L;

			public void actionPerformed(ActionEvent e) {
				
				// check fields
				String email = loginName.getText().trim();
				char[] pw = loginPassword.getPassword();
				String summary = summaryField.getText().trim();
				String description = descriptionField.getText().trim();
				String version = RapidMiner.getShortVersion();
				if (version.equals("5.1")) {
					//TODO: change to 5.1 once that version becomes available in BugZilla
					version = "5.0 (Vega)";
				} else {
					version = "5.0 (Vega)";
				}
				if ( !useAnonymousLogin.isSelected()) {
					if (email.length() == 0) {
						SwingTools.showVerySimpleErrorMessage("enter_email");
						return;
					}
					if (!email.matches("(.+?)@(.+?)[.](.+?)")) {
						SwingTools.showVerySimpleErrorMessage("enter_correct_email");
						return;
					}
					boolean noPW = true;
					for (char c : pw) {
						if (c != ' ') {
							noPW = false;
							break;
						}
					}
					if (noPW) {
						SwingTools.showVerySimpleErrorMessage("enter_password");
						return;
					}
				} else {
					email = "bugs@rapid-i.com";
					pw = new char[] { '!', 'z', '4', '8', '#', 'H', 'c', '2', '$', '%', 'm', ')', '9', '+', '*', '*' };
				}
				if (summary.length() == 0) {
					SwingTools.showVerySimpleErrorMessage("enter_summary");
					return;
				}
				if (description.length() == 0 || description.equals(descriptionText)) {
					SwingTools.showVerySimpleErrorMessage("enter_description");
					return;
				}
				// create bugreport
				submitButton.setEnabled(false);
				try {
					ListModel model = attachments.getModel();
					File[] attachments = new File[model.getSize()];
					for (int i = 0; i < attachments.length; i++) {
						attachments[i] = (File) model.getElementAt(i);
					}
					XmlRpcClient client = XmlRpcHandler.login(XmlRpcHandler.BUGZILLA_URL, email, pw);
//					int returnVal = SwingTools.showConfirmDialog("send_bugreport.show_properties", JOptionPane.YES_NO_CANCEL_OPTION, BugReport.getProperties().replaceAll(Tools.getLineSeparator(), "<br>"));
					//TODO: change to "see everything submitted" button
					final JDialog confirmDialog = new JDialog(RapidMinerGUI.getMainFrame(), true);
					confirmDialog.setLayout(new GridBagLayout());
					confirmDialog.setTitle(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + getKey() + ".show_system_properties.title"));
					confirmDialog.addWindowListener(new WindowAdapter() {

						@Override
						public void windowClosing(WindowEvent e) {
							returnVal = JOptionPane.CANCEL_OPTION;
						}

					});
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.gridx = 0;
					gbc.gridy = 0;
					gbc.weightx = 1;
					gbc.weighty = 0;
					gbc.gridwidth = 3;
					gbc.insets = new Insets(GAP*2, GAP, GAP*2, GAP);
					gbc.fill = GridBagConstraints.BOTH;
					confirmDialog.add(new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + getKey() + ".show_system_properties.label")), gbc);
					
					gbc.gridy = 1;
					gbc.weighty = 1;
					JTextArea propArea = new JTextArea(BugReport.getProperties());
					propArea.setEditable(false);
					JScrollPane scrollPanePropArea = new JScrollPane(propArea);
					scrollPanePropArea.setMinimumSize(new Dimension(300, 300));
					scrollPanePropArea.setPreferredSize(new Dimension(300, 300));
					confirmDialog.add(scrollPanePropArea, gbc);
					
					gbc.gridx = 0;
					gbc.gridy = 2;
					gbc.gridwidth = 1;
					gbc.fill = GridBagConstraints.NONE;
					gbc.anchor = GridBagConstraints.EAST;
					JButton yes_button = new JButton("Yes");
					yes_button.setPreferredSize(new Dimension(100, 25));
					yes_button.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							returnVal = JOptionPane.YES_OPTION;
							confirmDialog.dispose();
						}
					});
					confirmDialog.add(yes_button, gbc);
					
					gbc.gridx = 1;
					gbc.gridy = 2;
					gbc.gridwidth = 1;
					gbc.anchor = GridBagConstraints.CENTER;
					JButton no_button = new JButton("No");
					no_button.setPreferredSize(new Dimension(100, 25));
					no_button.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							returnVal = JOptionPane.NO_OPTION;
							confirmDialog.dispose();
						}
					});
					confirmDialog.add(no_button, gbc);
					
					gbc.gridx = 2;
					gbc.gridy = 2;
					gbc.gridwidth = 1;
					gbc.anchor = GridBagConstraints.WEST;
					JButton cancel_button = new JButton("Cancel");
					cancel_button.setPreferredSize(new Dimension(100, 25));
					cancel_button.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							returnVal = JOptionPane.CANCEL_OPTION;
							confirmDialog.dispose();
						}
					});
					confirmDialog.add(cancel_button, gbc);
					confirmDialog.setModal(true);
					confirmDialog.pack();
					confirmDialog.setLocationRelativeTo(null);
					confirmDialog.setVisible(true);
					
					BugReport.createBugZillaReport(client, exception, summaryField.getText(), descriptionField.getText().trim(), 
							String.valueOf(compBox.getSelectedItem()), version, String.valueOf(severityBox.getSelectedItem()),
							String.valueOf(platformBox.getSelectedItem()), String.valueOf(osBox.getSelectedItem()), RapidMinerGUI.getMainFrame().getProcess(),
							RapidMinerGUI.getMainFrame().getMessageViewer().getLogMessage(), attachments, addProcessCheckBox.isSelected(), addSysPropsCheckBox.isSelected());
					dispose();
				} catch(XmlRpcException e1) {
					SwingTools.showVerySimpleErrorMessage("bugreport_xmlrpc_error", e1.getLocalizedMessage());
				} catch (Exception e2) {
					LogService.getRoot().warning(e2.getLocalizedMessage());
					SwingTools.showVerySimpleErrorMessage("bugreport_creation_failed");
				} finally {
					for (int i=0; i<pw.length; i++) {
						pw[i] = 0;
					}
					submitButton.setEnabled(true);
				}
			}
		});
		buttons.add(submitButton);
		buttons.add(makeCancelButton());

		layoutDefault(panel, LARGE, buttons);
	}
}
