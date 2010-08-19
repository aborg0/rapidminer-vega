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
package com.rapidminer.gui.tools.dialogs;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.FilterableListModel;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.FixedWidthLabel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.jdbc.DatabaseService;
import com.rapidminer.tools.jdbc.JDBCProperties;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;
import com.rapidminer.tools.jdbc.connection.FieldConnectionEntry;

/**
 * 
 * @author Tobias Malbrecht
 */
public class DatabaseConnectionDialog extends ButtonDialog {
	private static final long serialVersionUID = -2046390670591412166L;
	
	private static final String TEXT_CONNECTION_STATUS_UNKNOWN = I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.status.unknown.label");
	
	private static final String TEXT_CONNECTION_STATUS_OK = I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.status.ok.label");
	
	private static final Icon ICON_CONNECTION_STATUS_UNKNOWN = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.status.unknown.icon"));
	
	private static final Icon ICON_CONNECTION_STATUS_OK = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.status.ok.icon"));
	
	private static final Icon ICON_CONNECTION_STATUS_ERROR = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.status.error.icon"));
	
	private static final Color TEXT_SELECTED_COLOR = UIManager.getColor("Tree.selectionForeground");

	private static final Color TEXT_NON_SELECTED_COLOR = UIManager.getColor("Tree.textForeground");
	
	private final FilterableListModel model = new FilterableListModel();
	{
		for (ConnectionEntry entry : DatabaseConnectionService.getConnectionEntries()) {
			model.addElement(entry);
		}
	}
	
	private final JList connectionList = new JList(model);
	{
		connectionList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 4616183160018529751L;
			
			private final Icon entryIcon = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.connection_entry.icon"));

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (isSelected) {
					label.setForeground(TEXT_SELECTED_COLOR);
				} else {
					label.setForeground(TEXT_NON_SELECTED_COLOR);					
				}
				if (value instanceof FieldConnectionEntry) {
					FieldConnectionEntry entry = (FieldConnectionEntry) value;
					String readOnly = (entry.isReadOnly()) ? "*":"";
					label.setText("<html>" + entry.getName() + readOnly + " <small>(" + entry.getProperties().getName() + "; " + entry.getHost() + ":" + entry.getPort() + ")</small></html>");
					label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
					label.setIcon(entryIcon);
				}
				return label;
			}
		});
		connectionList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					OPEN_CONNECTION_ACTION.actionPerformed(null);
				}
			}
		});
		connectionList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				boolean selected = connectionList.getSelectedValue() != null;
				OPEN_CONNECTION_ACTION.setEnabled(selected);
				
				// open delete only if not read only
				if (selected) {
					selected = !((FieldConnectionEntry)connectionList.getSelectedValue()).isReadOnly();
				}
				DELETE_CONNECTION_ACTION.setEnabled(selected);
			}
		});
	}
	
	private final JTextField aliasTextField = new JTextField(12);
	
	private final JComboBox databaseTypeComboBox = new JComboBox(DatabaseService.getDBSystemNames());
	
	private final JTextField hostTextField = new JTextField(12);
	
	private final JTextField portTextField = new JTextField(4);
	
	private final JTextField databaseTextField = new JTextField(12);
	
	private final JTextField userTextField = new JTextField(12);
	
	private final JPasswordField passwordField = new JPasswordField(12);
	
	private final JTextField urlField = new JTextField(12);
	
	private final JLabel testLabel = new FixedWidthLabel(280, TEXT_CONNECTION_STATUS_UNKNOWN, ICON_CONNECTION_STATUS_UNKNOWN);
	
	{
		urlField.setEditable(false);
		databaseTypeComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				updateDefaults();
				updateURL(null);
			}
		});
		KeyListener keyListener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				updateURL(null);
			}

			@Override
			public void keyTyped(KeyEvent e) {}
		};
		portTextField.addKeyListener(keyListener);
		hostTextField.addKeyListener(keyListener);
		databaseTextField.addKeyListener(keyListener);
		userTextField.addKeyListener(keyListener);
		passwordField.addKeyListener(keyListener);
	}
	
	private final Action OPEN_CONNECTION_ACTION = new ResourceAction("manage_db_connections.open") {
		private static final long serialVersionUID = 2451337494765496601L;

		@Override
		public void actionPerformed(ActionEvent e) {
			Object value = connectionList.getSelectedValue();
			if (value instanceof FieldConnectionEntry) {
				FieldConnectionEntry entry = (FieldConnectionEntry) value;
				// setting values of connection into fields
				aliasTextField.setText(entry.getName());
				databaseTypeComboBox.setSelectedItem(entry.getProperties().getName());
				hostTextField.setText(entry.getHost());
				portTextField.setText(entry.getPort());
				databaseTextField.setText(entry.getDatabase());
				userTextField.setText(entry.getUser());
				if (entry.getPassword() == null)
					passwordField.setText("");
				else
					passwordField.setText(new String(entry.getPassword()));

				// setting fields editable depending on entry's readonly flag
				aliasTextField.setEditable(!entry.isReadOnly());
				databaseTypeComboBox.setEnabled(!entry.isReadOnly());
				hostTextField.setEditable(!entry.isReadOnly());
				portTextField.setEditable(!entry.isReadOnly());
				databaseTextField.setEditable(!entry.isReadOnly());
				userTextField.setEditable(!entry.isReadOnly());
				passwordField.setEditable(!entry.isReadOnly());
				
				// disabling save action if needed
				SAVE_CONNECTION_ACTION.setEnabled(!entry.isReadOnly());
				
				// updating URL
				updateURL(entry);
			}
		}
	};
	
	private final Action SAVE_CONNECTION_ACTION = new ResourceAction("manage_db_connections.save") {
		private static final long serialVersionUID = -8477647509533859436L;

		@Override
		public void actionPerformed(ActionEvent e) {
			FieldConnectionEntry entry = checkFields(true);
			if (entry != null) {
				ConnectionEntry sameNameEntry = null;
				for (int i = 0; i < model.getSize(); i++) {
					ConnectionEntry compareEntry = (ConnectionEntry) model.getElementAt(i);
					if (compareEntry.getName().equals(entry.getName())) {
						sameNameEntry = compareEntry; 
						break;
					}
				}
				if (sameNameEntry == null) {
					model.addElement(entry);
					DatabaseConnectionService.addConnectionEntry(entry);
				} else {
					if (SwingTools.showConfirmDialog("manage_db_connections.overwrite", ConfirmDialog.YES_NO_OPTION, entry.getName()) == ConfirmDialog.YES_OPTION) {
						DatabaseConnectionService.deleteConnectionEntry(sameNameEntry);
						model.removeElement(sameNameEntry);
						model.addElement(entry);
						DatabaseConnectionService.addConnectionEntry(entry);
					}
				}
			}
		}
	};
	
	private final Action NEW_CONNECTION_ACTION = new ResourceAction("manage_db_connections.new") {
		private static final long serialVersionUID = -6286464201049577441L;

		@Override
		public void actionPerformed(ActionEvent e) {
			// resetting fields
			databaseTypeComboBox.setSelectedIndex(0);
			aliasTextField.setText("");
			hostTextField.setText("");
			portTextField.setText("");
			databaseTextField.setText("");
			userTextField.setText("");
			passwordField.setText("");
			
			// enabling fields
			aliasTextField.setEditable(true);
			databaseTypeComboBox.setEnabled(true);
			hostTextField.setEditable(true);
			portTextField.setEditable(true);
			databaseTextField.setEditable(true);
			userTextField.setEditable(true);
			passwordField.setEditable(true);
			
			
			SAVE_CONNECTION_ACTION.setEnabled(true);
			
			// setting defaults
			updateDefaults();
			updateURL(null);
		}
	};
	
	private final Action DELETE_CONNECTION_ACTION = new ResourceAction("manage_db_connections.delete") {
		private static final long serialVersionUID = 1155260480975020776L;

		@Override
		public void actionPerformed(ActionEvent e) {
			Object[] selectedValues = connectionList.getSelectedValues();
			boolean applyToAll = false;
			int returnOption = ConfirmDialog.CANCEL_OPTION;
			for (int i = 0; i < selectedValues.length; i++) {
				ConnectionEntry entry = (ConnectionEntry) selectedValues[i];
				if (!applyToAll) {
					MultiConfirmDialog dialog = new MultiConfirmDialog("manage_db_connections.delete", ConfirmDialog.YES_NO_CANCEL_OPTION, entry.getName());
					dialog.setVisible(true);
					applyToAll = dialog.applyToAll();
					returnOption = dialog.getReturnOption();
				}
				if (returnOption == ConfirmDialog.CANCEL_OPTION) {
					break;
				}
				if (returnOption == ConfirmDialog.YES_OPTION) {
					DatabaseConnectionService.deleteConnectionEntry(entry);
					model.removeElement(entry);
					connectionList.clearSelection();
					for (int j = 0; j < selectedValues.length; j++) {
						int index = model.indexOf(selectedValues[j]);
						connectionList.getSelectionModel().addSelectionInterval(index, index);
					}
				}
			}
		}
	};
	
	private final Action TEST_CONNECTION_ACTION = new ResourceAction("manage_db_connections.test") {
		private static final long serialVersionUID = -25485375154547037L;

		@Override
		public void actionPerformed(ActionEvent e) {
			ProgressThread t = new ProgressThread("test_database_connection") {
				@Override
				public void run() {
					getProgressListener().setTotal(100);
					getProgressListener().setCompleted(10);
					try {
						ConnectionEntry entry = checkFields(false);
						if (entry == null) {
							return;
						}
			            if (!DatabaseConnectionService.testConnection(entry)) {
			            	throw new SQLException();
			            }
			            testLabel.setText(TEXT_CONNECTION_STATUS_OK);
			            testLabel.setIcon(ICON_CONNECTION_STATUS_OK);
			        } catch (SQLException exception) {
			        	String errorMessage = exception.getLocalizedMessage();
			        	if (errorMessage.length() > 100) {
			        		errorMessage = exception.getLocalizedMessage().substring(0, 100) + "...";
			        	}
			        	testLabel.setText(errorMessage);
			            testLabel.setIcon(ICON_CONNECTION_STATUS_ERROR);
			        } finally {
			        	getProgressListener().complete();
			        }
				}
			};
			t.start();
		}
	};
	
	{
		OPEN_CONNECTION_ACTION.setEnabled(false);
		DELETE_CONNECTION_ACTION.setEnabled(false);
	}
		
	public DatabaseConnectionDialog(String i18nKey, Object ... i18nArgs) {
		super(i18nKey, true, i18nArgs);
	}
	
	public Collection<AbstractButton> makeButtons() {
		Collection<AbstractButton> list = new LinkedList<AbstractButton>();
		list.add(new JButton(OPEN_CONNECTION_ACTION));
		list.add(new JButton(SAVE_CONNECTION_ACTION));
		list.add(new JButton(NEW_CONNECTION_ACTION));
		list.add(new JButton(DELETE_CONNECTION_ACTION));
		list.add(new JButton(TEST_CONNECTION_ACTION));
		return list;
	}
	
	private JPanel makeConnectionPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.border.manage_db_connections.details")));
		GridBagConstraints c = new GridBagConstraints();

		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		
		c.insets = new Insets(0, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.name"), c);
		c.insets = new Insets(0, GAP, GAP, GAP);
		panel.add(aliasTextField, c);
		
		c.weightx = 1;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.system"), c);
		c.insets = new Insets(0, GAP, 0, GAP);
		panel.add(databaseTypeComboBox, c);

		c.weightx = 0.8;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.host"), c);
		
		c.weightx = 0.2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		panel.add(new ResourceLabel("manage_db_connections.port"), c);

		c.weightx = 0.8;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.insets = new Insets(0, GAP, 0, GAP);
		panel.add(hostTextField, c);
		
		c.weightx = 0.2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		panel.add(portTextField, c);

		c.weightx = 1;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.database"), c);
		c.insets = new Insets(0, GAP, 0, GAP);
		panel.add(databaseTextField, c);

		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.user"), c);
		c.insets = new Insets(0, GAP, 0, GAP);
		panel.add(userTextField, c);

		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.password"), c);
		c.insets = new Insets(0, GAP, GAP, GAP);
		panel.add(passwordField, c);
		
		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.url"), c);
		c.insets = new Insets(0, GAP, GAP, GAP);
		panel.add(urlField, c);

		c.weighty = 1;
		c.insets = new Insets(0, 2 * GAP, GAP, GAP);
		panel.add(testLabel, c);
		
		updateDefaults();
		updateURL(null);
		return panel;
	}
	
	public JPanel makeConnectionManagementPanel() {
		JPanel panel = new JPanel(createGridLayout(1, 2));
		JScrollPane connectionListPane = new ExtendedJScrollPane(connectionList);
		connectionListPane.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.border.manage_db_connections.connections")));
		panel.add(connectionListPane);
		panel.add(makeConnectionPanel());
		return panel;
	}
		
	private JDBCProperties getProperties() {
		return DatabaseService.getJDBCProperties((String) databaseTypeComboBox.getSelectedItem());
	}

	private void updateDefaults() {
		portTextField.setText(getProperties().getDefaultPort());
	}
	
	private void updateURL(FieldConnectionEntry entry) {
		if (entry != null && entry.isReadOnly()) {
			urlField.setText(entry.getURL());
		} else {
			urlField.setText(FieldConnectionEntry.createURL(getProperties(), hostTextField.getText(), portTextField.getText(), databaseTextField.getText()));
		}
		testLabel.setText(TEXT_CONNECTION_STATUS_UNKNOWN);
		testLabel.setIcon(ICON_CONNECTION_STATUS_UNKNOWN);
		fireStateChanged();
	}
	
	protected FieldConnectionEntry checkFields(boolean save) {
		String alias = aliasTextField.getText();
		if (save && (alias == null || "".equals(alias.trim()))) {
			SwingTools.showVerySimpleErrorMessage("manage_db_connections.missing", I18N.getMessage(I18N.getGUIBundle(), "gui.label.manage_db_connections.name.label"));
			aliasTextField.requestFocusInWindow();
			return null;
		}
		String host = hostTextField.getText();
		if (host == null || "".equals(host)) {
			SwingTools.showVerySimpleErrorMessage("manage_db_connections.missing", I18N.getMessage(I18N.getGUIBundle(), "gui.label.manage_db_connections.host.label"));
			hostTextField.requestFocusInWindow();
			return null;
		}
		String port = portTextField.getText();
//		if (port == null || "".equals(port)) {
//			SwingTools.showVerySimpleErrorMessage("manage_db_connections.missing", I18N.getMessage(I18N.getGUIBundle(), "gui.label.manage_db_connections.port.label"));
//			portTextField.requestFocusInWindow();
//			return null;
//		}
		String database = databaseTextField.getText();
		if (database == null) {
			database = "";
		}
//		if (database == null || "".equals(database)) {
//			SwingTools.showVerySimpleErrorMessage("manage_db_connections.missing", I18N.getMessage(I18N.getGUIBundle(), "gui.label.manage_db_connections.database.label"));
//			databaseTextField.requestFocusInWindow();
//			return null;
//		}
		String user = userTextField.getText();
		char[] password = passwordField.getPassword();
		return new FieldConnectionEntry(alias, getProperties(), host, port, database, user, password);
	}
	
	public FieldConnectionEntry getConnectionEntry(boolean save) {
		String alias = aliasTextField.getText();
		if (save && (alias == null || "".equals(alias.trim()))) {
			return null;
		}
		String host = hostTextField.getText();
		if (host == null || "".equals(host)) {
			return null;
		}
		String port = portTextField.getText();
		if (host == null) { // || "".equals(port)) {
			port = "";
		}
		String database = databaseTextField.getText();
		if (database == null) {
			database = "";
		}
//		if (database == null || "".equals(database)) {
//			return null;
//		}
		String user = userTextField.getText();
		char[] password = passwordField.getPassword();
		return new FieldConnectionEntry(alias, getProperties(), host, port, database, user, password);
	}

//	private boolean isEntryModified() {
//		Object value = connectionList.getSelectedValue();
//		if (value instanceof FieldConnectionEntry) {
//			FieldConnectionEntry selectedEntry = (FieldConnectionEntry) value;
//			FieldConnectionEntry modifiedEntry = getConnectionEntry(false);
//			return modifiedEntry.equals(selectedEntry);
//		}
//		return true;
//	}
}
