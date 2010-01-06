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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.CharTextField;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataEditor;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.io.Encoding;


/**
 * @author Tobias Malbrecht
 */
public abstract class ParseFileWizardStep extends WizardStep {
	
	private final JCheckBox trimLinesBox = new JCheckBox("Trim Lines", true);
	
	private final JComboBox encodingComboBox = new JComboBox(Encoding.CHARSETS);
	{
		String encoding = RapidMiner.SYSTEM_ENCODING_NAME;
		String encodingProperty = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING);
		if (encodingProperty != null) {
			encoding = encodingProperty;
		}
		encodingComboBox.setSelectedItem(encoding);
		encodingComboBox.setPreferredSize(new Dimension(encodingComboBox.getPreferredSize().width, 25));
		encodingComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				settingsChanged();
			}
		});
	}

 	private final JCheckBox skipCommentsBox = new JCheckBox("Skip Comments", true);
	
	private final JCheckBox useFirstRowAsColumnNamesBox = new JCheckBox("Use First Row as Column Names", true);

	private final JCheckBox useQuotesBox = new JCheckBox("Use Quotes", true);

	private final JTextField commentCharacterTextField = new JTextField(LineParser.DEFAULT_COMMENT_CHARACTER_STRING);

	private final CharTextField quoteCharacterTextField = new CharTextField(LineParser.DEFAULT_QUOTE_CHARACTER);
	
	private final JRadioButton commaButton = new JRadioButton("Comma");

	private final JRadioButton semicolonButton = new JRadioButton("Semicolon");

	private final JRadioButton tabButton = new JRadioButton("Tab"); 

	private final JRadioButton spaceButton = new JRadioButton("Space"); 

	private final JRadioButton regexButton = new JRadioButton("Regular Expression");

	private final JTextField regexTextField = new JTextField(LineParser.DEFAULT_SPLIT_EXPRESSION);
	
	private final DataEditor editor = new DataEditor(false);
		
	{
		skipCommentsBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				commentCharacterTextField.setEnabled(skipCommentsBox.isSelected());
				settingsChanged();
			}
		});
		commentCharacterTextField.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				settingsChanged();
			}

			@Override
			public void keyTyped(KeyEvent e) {}
		});
		useFirstRowAsColumnNamesBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				settingsChanged();
			}
		});
		useQuotesBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quoteCharacterTextField.setEnabled(useQuotesBox.isSelected());
				settingsChanged();
			}
		});
		quoteCharacterTextField.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				settingsChanged();
			}

			@Override
			public void keyTyped(KeyEvent e) {}
		});
		regexButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				regexTextField.setEnabled(regexButton.isSelected());
				settingsChanged();
			}
		});
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(commaButton);
		buttonGroup.add(semicolonButton);
		buttonGroup.add(spaceButton);
		buttonGroup.add(tabButton);
		buttonGroup.add(regexButton);
		regexButton.setSelected(true);
		ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				settingsChanged();
			}
		};
		commaButton.addActionListener(listener);
		semicolonButton.addActionListener(listener);
		spaceButton.addActionListener(listener);
		tabButton.addActionListener(listener);
		regexButton.addActionListener(listener);
		regexTextField.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				settingsChanged();
			}

			@Override
			public void keyTyped(KeyEvent e) {}
		});
	}

	public ParseFileWizardStep(String i18nKey) {
		super(i18nKey);
	}

	protected Charset getEncoding() {
		return Encoding.getEncoding((String) encodingComboBox.getSelectedItem());
	}
	
	protected boolean trimLines() {
		return trimLinesBox.isSelected();
	}
	
	protected boolean skipComments() {
		return skipCommentsBox.isSelected();
	}
	
	protected String getCommentCharacters() {
		return commentCharacterTextField.getText();
	}
	
	protected boolean useQuotes() {
		return useQuotesBox.isSelected();
	}
	
	protected char getQuotesCharacter() {
		return quoteCharacterTextField.getCharacter();
	}
	
	protected String getSplitExpression() {
		String splitExpression = null;
		if (regexButton.isSelected()) {
			splitExpression = regexTextField.getText();
			if ("".equals(splitExpression)) {
				splitExpression = null;
			} else {
				try {
					Pattern.compile(splitExpression);
				} catch (PatternSyntaxException pse) {
					splitExpression = null;
				}
			}
		} else if (commaButton.isSelected()) {
			splitExpression = LineParser.SPLIT_BY_COMMA_EXPRESSION;
		} else if (semicolonButton.isSelected()) {
			splitExpression = LineParser.SPLIT_BY_SEMICOLON_EXPRESSION;
		} else if (tabButton.isSelected()) {
			splitExpression = LineParser.SPLIT_BY_TAB_EXPRESSION;
		} else if (spaceButton.isSelected()) {
			splitExpression = LineParser.SPLIT_BY_SPACE_EXPRESSION;
		}
		return splitExpression;
	}
	
	protected abstract void settingsChanged();
	
	protected void setData(ExampleSetMetaData metaData, LinkedList<Object[]> data) {
		editor.setData(metaData, data);
	}
	
	protected boolean getUseFirstRowAsColumnNames() {
		return useFirstRowAsColumnNamesBox.isSelected();
	}
		
	@Override
	protected JComponent getComponent() {
		JPanel optionPanel = new JPanel(ButtonDialog.createGridLayout(4, 1));
		optionPanel.add(new JPanel(ButtonDialog.createGridLayout(1, 2)) {
			private static final long serialVersionUID = -1726235838693547187L;
			{
				add(new JLabel("File Encoding"));
				add(encodingComboBox);
			}
		});
		optionPanel.add(new JPanel(ButtonDialog.createGridLayout(1, 1)) {
			private static final long serialVersionUID = -1726235838693547187L;
			{
				add(trimLinesBox);
			}
		});
		optionPanel.add(new JPanel(ButtonDialog.createGridLayout(1, 2)) {
			private static final long serialVersionUID = -1726235838693547187L;
			{
				add(skipCommentsBox);
				add(commentCharacterTextField);
			}
		});
		optionPanel.add(new JPanel(ButtonDialog.createGridLayout(1, 1)) {
			private static final long serialVersionUID = -1726235838693547187L;
			{
				add(useFirstRowAsColumnNamesBox);
			}
		});
		optionPanel.setBorder(ButtonDialog.createTitledBorder("File Reading"));
		
		JPanel separationPanel = new JPanel(ButtonDialog.createGridLayout(4, 2));
		separationPanel.add(commaButton);
		separationPanel.add(spaceButton);
		separationPanel.add(semicolonButton);
		separationPanel.add(tabButton);
		separationPanel.add(regexButton);
		separationPanel.add(regexTextField);
		separationPanel.add(useQuotesBox);
		separationPanel.add(quoteCharacterTextField);
		separationPanel.setBorder(ButtonDialog.createTitledBorder("Column Separation"));
		
		JPanel parsingPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
		parsingPanel.add(optionPanel);
		parsingPanel.add(separationPanel);

		editor.setBorder(null);
		ExtendedJScrollPane tablePane = new ExtendedJScrollPane(editor);
		tablePane.setBorder(ButtonDialog.createBorder());

		JPanel panel = new JPanel(new BorderLayout(0, ButtonDialog.GAP));
		panel.add(parsingPanel, BorderLayout.NORTH);
		panel.add(tablePane, BorderLayout.CENTER);
		return panel;
	}
}
