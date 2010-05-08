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
import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.io.Base64;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.XMLException;

/** Dialog asking for username and passwords. Answers may be cached (if chosen by user).
 * 
 * @author Simon Fischer
 *
 */
public class PasswordDialog extends ButtonDialog {

	private static final String CACHE_FILE_NAME = "secrets.xml";

	/** Maps URLs to authentications. */
	private static Map<String,PasswordAuthentication> CACHE = new HashMap<String,PasswordAuthentication>();
	static {
		readCache();
	}
	
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
			rememberBox.setSelected(true);
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
		// clear cache if refresh forced
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
			saveCache();
			return result;
		} else {
			return null;
		}
	}
	
	private static void saveCache() {
		LogService.getRoot().config("Saving secrets file.");
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			LogService.getRoot().log(Level.WARNING, "Failed to create XML document: "+e, e);
			return;
		}
		Element root = doc.createElement(CACHE_FILE_NAME);
		doc.appendChild(root);
		for (Entry<String, PasswordAuthentication> entry : CACHE.entrySet()) {
			Element entryElem = doc.createElement("secret");
			root.appendChild(entryElem);
			XMLTools.setTagContents(entryElem, "url", entry.getKey());
			XMLTools.setTagContents(entryElem, "user", entry.getValue().getUserName());
			XMLTools.setTagContents(entryElem, "password", Base64.encodeBytes(new String(entry.getValue().getPassword()).getBytes()));
		}
		File file = ParameterService.getUserConfigFile(CACHE_FILE_NAME);
		try {
			XMLTools.stream(doc, file, null);
		} catch (XMLException e) {
			LogService.getRoot().log(Level.WARNING, "Failed to save secrets file: "+e, e);
		}
	}
	
	private static void readCache() {
		final File userConfigFile = ParameterService.getUserConfigFile(CACHE_FILE_NAME);
		if (!userConfigFile.exists()) {
			return;
		}
		LogService.getRoot().config("Reading secrets file.");
		Document doc;
		try {			 
			doc = XMLTools.parse(userConfigFile);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Failed to read secrets file: "+e, e);
			return;
		}
		NodeList secretElems = doc.getDocumentElement().getElementsByTagName("secret");
		for (int i = 0; i < secretElems.getLength(); i++) {
			Element secretElem = (Element) secretElems.item(i);
			String url = XMLTools.getTagContents(secretElem, "url");
			String user = XMLTools.getTagContents(secretElem, "user");
			String password;
			try {
				password = new String(Base64.decode(XMLTools.getTagContents(secretElem, "password")));
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "Failed to read entry in secrets file: "+e, e);
				continue;
			}
			CACHE.put(url, new PasswordAuthentication(user, password.toCharArray()));
		}
	}
}
