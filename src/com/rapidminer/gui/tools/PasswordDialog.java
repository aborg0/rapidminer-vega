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
package com.rapidminer.gui.tools;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.rapidminer.gui.tools.dialogs.ButtonDialog;

/** Dialog asking for username and passwords. Answers may be cached (if chosen by user).
 * 
 * @author Simon Fischer
 *
 */
public class PasswordDialog extends ButtonDialog {

	/** Maps URLs to authentications. */
	private static Map<String,PasswordAuthentication> CACHE = new HashMap<String,PasswordAuthentication>();
	
	private static final long serialVersionUID = 1L;

	private JTextField usernameField = new JTextField(20);
	private JPasswordField passwordField = new JPasswordField(20);
	private JCheckBox rememberBox = new JCheckBox(new ResourceActionAdapter("authentication.remember"));
	
	private PasswordDialog(PasswordAuthentication preset, String url) {
		super("authentication", url);
		setModal(true);
		if (preset != null) {
			usernameField.setText(preset.getUserName());
		}
		if (preset!= null) {
			passwordField.setText(new String(preset.getPassword()));
		}
		
		JPanel main = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets(4,4,4,4);
		
		JLabel label = new ResourceLabel("authentication.username", url);
		label.setLabelFor(usernameField);
		c.gridwidth = GridBagConstraints.RELATIVE;
		main.add(label, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		main.add(usernameField, c);
		
		label = new ResourceLabel("authentication.password", url);
		label.setLabelFor(passwordField);
		c.gridwidth = GridBagConstraints.RELATIVE;
		main.add(label, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		main.add(passwordField, c);
		
		main.add(rememberBox, c);
		
		layoutDefault(main, makeCancelButton(), makeOkButton());		
	}

	public PasswordAuthentication makeAuthentication() {
		return new PasswordAuthentication(usernameField.getText(), passwordField.getPassword());
	}
	
	public static PasswordAuthentication getPasswordAuthentication(String forUrl, boolean forceRefresh) {
		PasswordAuthentication authentication = CACHE.get(forUrl);
		if (forceRefresh && authentication != null) {
			authentication = new PasswordAuthentication(authentication.getUserName(), null);
			CACHE.put(forUrl, authentication);
		} 
		PasswordDialog pd = new PasswordDialog(authentication, forUrl);
		pd.setVisible(true);
		if (pd.wasConfirmed()) {
			PasswordAuthentication result = pd.makeAuthentication();
			if (pd.rememberBox.isSelected()) {
				CACHE.put(forUrl, result);
			} else {
				CACHE.remove(forUrl);
			}
			return result;
		} else {
			return null;
		}
	}
}
