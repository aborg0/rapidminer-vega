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

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.ParameterService;

/** A dialog that shows the repository tree. The static method
 *  {@link #selectLocation()} shows a dialog and returns the location
 *  selected by the user.
 *  
 *  @author Simon Fischer, Tobias Malbrecht
 *
 */
public class RepositoryLocationChooser extends JPanel {

	private static final long serialVersionUID = 1L;

	private final RepositoryTree tree;

	private final JTextField locationField = new JTextField(30);

	private final RepositoryLocation resolveRelativeTo;

	private JCheckBox resolveBox;

	private final LinkedList<ChangeListener> listeners = new LinkedList<ChangeListener>();

	private final JLabel resultLabel = new JLabel();

	public RepositoryLocationChooser(RepositoryLocation resolveRelativeTo, String initialValue) {
		this(null, resolveRelativeTo, initialValue);
	}

	public RepositoryLocationChooser(Dialog owner, RepositoryLocation resolveRelativeTo, String initialValue) {
		if (initialValue != null) {			
			try {
				RepositoryLocation repositoryLocation = new RepositoryLocation(resolveRelativeTo, initialValue);
				locationField.setText(repositoryLocation.getName());
				resultLabel.setText(repositoryLocation.toString());
			} catch (Exception e) {
			}
		}
		this.resolveRelativeTo = resolveRelativeTo;
		tree = new RepositoryTree(owner);
		
		if (initialValue != null) {
			if (tree.expandIfExists(resolveRelativeTo, initialValue)) {
				locationField.setText("");
			}
		}
		tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getPath() != null) {
					Entry entry = (Entry) e.getPath().getLastPathComponent();
					if (!(entry instanceof Folder)) {												
						locationField.setText(entry.getLocation().getName());
					}
					updateResult();
				}
				for (ChangeListener l : listeners) {
					l.stateChanged(new ChangeEvent(this));
				}
			}			
		});
		locationField.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				for (ChangeListener l : listeners) {
					l.stateChanged(new ChangeEvent(this));
				}
				updateResult();
			}
			@Override
			public void keyTyped(KeyEvent e) {
				TreePath selectionPath = tree.getSelectionPath();
				if (selectionPath != null) {
					Entry selectedEntry = (Entry) selectionPath.getLastPathComponent();
					if (!(selectedEntry instanceof Folder)) {
						tree.setSelectionPath(selectionPath.getParentPath());	
					}					
				}
			}			
		});

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 0, 0);		
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridwidth = GridBagConstraints.REMAINDER;

		JScrollPane treePane = new ExtendedJScrollPane(tree);
		treePane.setBorder(ButtonDialog.createBorder());
		add(treePane, c);

		c.insets = new Insets(ButtonDialog.GAP, 0, 0, ButtonDialog.GAP);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 0;
		JLabel label = new ResourceLabel("repository_chooser.entry_name");
		label.setLabelFor(locationField);
		add(label, c);		

		c.weightx = 1;
		c.insets = new Insets(ButtonDialog.GAP, 0, 0, 0);
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(locationField, c);

		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 0;
		c.insets = new Insets(ButtonDialog.GAP, 0, 0, ButtonDialog.GAP);
		add(new ResourceLabel("repository_chooser.location"), c);		
		c.weightx = 1;
		c.insets = new Insets(ButtonDialog.GAP, 0, 0, 0);
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(resultLabel, c);

		if (resolveRelativeTo != null) {
			resolveBox = new JCheckBox(new ResourceActionAdapter("repository_chooser.resolve", resolveRelativeTo.getAbsoluteLocation()));
			resolveBox.setSelected(System.getProperty(RapidMinerGUI.PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS, "true").equals("true"));
			add(resolveBox, c);
			resolveBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					updateResult();
				}
			});
		}
	}

	public String getRepositoryLocation() {
		if (tree.getSelectionPath() != null) {
			Entry selectedEntry = (Entry) tree.getSelectionPath().getLastPathComponent();
			RepositoryLocation selectedLocation = selectedEntry.getLocation();
			if (selectedEntry instanceof Folder) {
				selectedLocation = new RepositoryLocation(selectedLocation, locationField.getText());
			}
			if ((RepositoryLocationChooser.this.resolveRelativeTo != null) && resolveBox.isSelected()) {
				return selectedLocation.makeRelative(RepositoryLocationChooser.this.resolveRelativeTo);						  
			} else {
				return selectedLocation.getAbsoluteLocation();
			}
		} else {
			return locationField.getText();
		}
	}
	
	public boolean hasSelection() {
		return !locationField.getText().equals("");
	}

	public boolean resolveRelative() {
		return resolveBox.isSelected();
	}

	public void addChangeListener(ChangeListener l) {
		listeners.add(l);
	}

	public void removeChangeListener(ChangeListener l) {
		listeners.remove(l);
	}

	public static String selectLocation(RepositoryLocation resolveRelativeTo, Component c) {
		return selectLocation(resolveRelativeTo, null, c);
	}

	public static String selectLocation(RepositoryLocation resolveRelativeTo, String initialValue, Component c) {
		final String result[] = new String[1];		
		class RepositoryLocationChooserDialog extends ButtonDialog {
			private static final long serialVersionUID = -726540444296013310L;

			private RepositoryLocationChooser chooser = null;

			public RepositoryLocationChooserDialog(RepositoryLocation resolveRelativeTo, String initialValue) {
				super("repository_chooser", true);
				chooser = new RepositoryLocationChooser(this, resolveRelativeTo, initialValue);
				chooser.tree.addRepositorySelectionListener(new RepositorySelectionListener() {			
					@Override
					public void repositoryLocationSelected(RepositorySelectionEvent e) {
						Entry entry = e.getEntry();
						result[0] = entry.getLocation().toString();
						dispose();
					}
				});
				layoutDefault(chooser, NORMAL, makeOkButton(), makeCancelButton());
			}
		}
		final RepositoryLocationChooserDialog dialog = new RepositoryLocationChooserDialog(resolveRelativeTo, initialValue);
		dialog.setVisible(true);
		
		if (result[0] != null) {
			return result[0];
		}
		if (dialog.wasConfirmed()) {
			if (resolveRelativeTo != null) {
				System.setProperty(RapidMinerGUI.PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS, dialog.chooser.resolveRelative() ? "true" : "false");
				ParameterService.writePropertyIntoMainUserConfigFile(RapidMinerGUI.PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS, dialog.chooser.resolveRelative() ? "true" : "false");
			}
			String text = dialog.chooser.getRepositoryLocation();
			if (text.length() > 0) {
				return text;
			} else {
				return null;
			}			
		} else {
			return null;
		}
	}

	private void updateResult() {
		String repositoryLocation = getRepositoryLocation();
		resultLabel.setText(repositoryLocation);
	}
}
