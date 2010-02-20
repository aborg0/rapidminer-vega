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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.rapidminer.gui.tools.CharTextField;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataEditor;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.DateParser;
import com.rapidminer.tools.StrictDecimalFormat;


/**
 * 
 * @author Tobias Malbrecht
 */
public abstract class ParseValueTypesWizardStep extends WizardStep {
	private final KeyAdapter textFieldKeyListener = new KeyAdapter() {
		@Override
		public void keyReleased(KeyEvent e) {
			settingsChanged();
		}
	};
	
//	private static final String DEFAULT_USER_MISSING = "?";
	
	private final DataEditor editor = new DataEditor(true, true);
		
//	private final JComboBox localeComboBox = new JComboBox(AbstractDateDataProcessing.availableLocaleNames);
	
	private final CharTextField decimalPointCharacterTextField = new CharTextField(StrictDecimalFormat.DEFAULT_DECIMAL_CHARACTER);
	{
		decimalPointCharacterTextField.addKeyListener(textFieldKeyListener);
	}

	private final CharTextField groupingCharacterTextField = new CharTextField(StrictDecimalFormat.DEFAULT_GROUPING_CHARACTER);
	{
		groupingCharacterTextField.addKeyListener(textFieldKeyListener);
		groupingCharacterTextField.setEnabled(false);
	}
	
	private final JCheckBox groupNumbersBox = new JCheckBox("Digit Grouping", false);
	{
		groupNumbersBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				groupingCharacterTextField.setEnabled(groupNumbersBox.isSelected());
				settingsChanged();
			}
		});
	}

	// TODO add cell editor of custom date format parameter type (still to implement also)
	private final JTextField dateFormatTextField = new JTextField(DateParser.DEFAULT_DATE_FORMAT);
	{
		dateFormatTextField.addKeyListener(textFieldKeyListener);
	}
	
//	private final JTextField userMissingTextField = new JTextField(DEFAULT_USER_MISSING);
//	{
//		// TODO add action listener
//	}
//	
//	private final JCheckBox defineUserMissingBox = new JCheckBox("Missing Value");
//	{
//		// TODO add action listener
//	}
	
	public ParseValueTypesWizardStep(String key) {
		super(key);
	}
	
	protected abstract void settingsChanged();
	
	protected void setData(ExampleSetMetaData metaData, LinkedList<Object[]> data) {
		editor.setData(metaData, data);
	}
	
	protected char getDecimalPointCharacter() {
		return decimalPointCharacterTextField.getCharacter();
	}
	
	protected boolean groupDigits() {
		return groupNumbersBox.isSelected();
	}
	
	protected char getGroupingSeparator() {
		return groupingCharacterTextField.getCharacter();
	}
	
	protected String getDateFormat() {
		return dateFormatTextField.getText();
	}
	
	@Override
	protected JComponent getComponent() {
		JPanel detectionPanel = new JPanel(ButtonDialog.createGridLayout(3, 2));
//		detectionPanel.add(new JLabel("Locale"));
//		detectionPanel.add(localeComboBox);
		detectionPanel.add(new JLabel("Decimal Character"));
		detectionPanel.add(decimalPointCharacterTextField);
		detectionPanel.add(groupNumbersBox);
		detectionPanel.add(groupingCharacterTextField);
		detectionPanel.add(new JLabel("Date Format"));
		detectionPanel.add(dateFormatTextField);
		detectionPanel.setBorder(ButtonDialog.createTitledBorder("Type Detection"));

//		JPanel optionPanel = new JPanel(ButtonDialog.createGridLayout(2, 2));
//		optionPanel.add(defineUserMissingBox);
//		optionPanel.add(userMissingTextField);
//		optionPanel.add(new JCheckBox("Trim Nominal"));
//		optionPanel.setBorder(ButtonDialog.createTitledBorder("Value Transformation"));
		
		JPanel parsingPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
		parsingPanel.add(detectionPanel);
//		parsingPanel.add(optionPanel);

		editor.setBorder(null);
		ExtendedJScrollPane tablePane = new ExtendedJScrollPane(editor);
		tablePane.setBorder(ButtonDialog.createBorder());

		JPanel panel = new JPanel(new BorderLayout(0, ButtonDialog.GAP));
		panel.add(parsingPanel, BorderLayout.NORTH);
		panel.add(tablePane, BorderLayout.CENTER);
		return panel;
	}
}
