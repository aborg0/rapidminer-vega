package com.rapidminer.operator.nio;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.TableModel;

import com.rapidminer.gui.tools.CharTextField;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.io.Encoding;

/**
 * 
 * @author Sebastian Loh, Simon Fischer
 *
 */
public class CSVSyntaxConfigurationWizardStep extends WizardStep {

	private JPanel panel;

	private final JCheckBox trimLinesBox = new JCheckBox("Trim Lines", true);
	private final JComboBox encodingComboBox = new JComboBox(Encoding.CHARSETS);
	private final JCheckBox skipCommentsBox = new JCheckBox("Skip Comments", true); // just temp preselection, real value is defined in the constructor
	//private final JCheckBox useFirstRowAsColumnNamesBox = new JCheckBox("Use First Row as Column Names",true);  // just temp preselection, real value is defined in the constructor
	private final JCheckBox useQuotesBox = new JCheckBox("Use Quotes",true); // just temp preselection, real value is defined in the constructor 
	private final JTextField commentCharacterTextField = new JTextField(LineParser.DEFAULT_COMMENT_CHARACTER_STRING);
	private final CharTextField quoteCharacterTextField = new CharTextField(LineParser.DEFAULT_QUOTE_CHARACTER);
	private final JLabel escapeCharacterLabel =  new JLabel("Escape Character for Seperator:");
	private final CharTextField escapeCharacterTextField = new CharTextField(LineParser.DEFAULT_QUOTE_ESCAPE_CHARACTER);
	private final JRadioButton commaButton = new JRadioButton("Comma \",\" ");
	private final JRadioButton semicolonButton = new JRadioButton("Semicolon \";\"");
	private final JRadioButton tabButton = new JRadioButton("Tab");
	private final JRadioButton spaceButton = new JRadioButton("Space");
	private final JRadioButton regexButton = new JRadioButton("Regular Expression");
	private final JTextField regexTextField = new JTextField(LineParser.DEFAULT_SPLIT_EXPRESSION);

	private CSVResultSetConfiguration configuration;

	private JTable previewTable;

	private JScrollPane tablePane;

	public CSVSyntaxConfigurationWizardStep(CSVImportWizard csvImportWizard, CSVResultSetConfiguration csvConfiguration) {	
		super("specify_csv_parsing_options");
		this.configuration = csvConfiguration;

		// configuration -> UI components
		skipCommentsBox.setSelected(configuration.isSkipComments());
		//useFirstRowAsColumnNamesBox.setSelected(configuration.isFirstRowAsAttributeNames());
		useQuotesBox.setSelected(configuration.isUseQuotes());
		trimLinesBox.setSelected(configuration.isTrimLines());
		commentCharacterTextField.setText(configuration.getCommentCharacters());
		escapeCharacterTextField.setText(String.valueOf(configuration.getEscapeCharacter()));
		quoteCharacterTextField.setText(String.valueOf(configuration.getQuoteCharacter()));
		encodingComboBox.setSelectedItem(configuration.getEncoding().name());

		String sep = configuration.getColumnSeparators();
		regexButton.setSelected(true);
		if (sep.equals(LineParser.SPLIT_BY_COMMA_EXPRESSION)){
			commaButton.setSelected(true);
		}
		if (sep.equals(LineParser.SPLIT_BY_SEMICOLON_EXPRESSION)){
			semicolonButton.setSelected(true);
		}
		if (sep.equals(LineParser.SPLIT_BY_TAB_EXPRESSION)){
			tabButton.setSelected(true);
		}
		if (sep.equals(LineParser.SPLIT_BY_SPACE_EXPRESSION)){
			spaceButton.setSelected(true);
		}

		registerListeners();
		makePanel();
	}

	private void registerListeners() {
		encodingComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setEncoding(Encoding.getEncoding(encodingComboBox.getSelectedItem().toString()));
				settingsChanged();
			}
		});

		trimLinesBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setTrimLines(trimLinesBox.isSelected());
				settingsChanged();
			}
		});
		skipCommentsBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				commentCharacterTextField.setEnabled(skipCommentsBox.isSelected());
				configuration.setSkipComments(skipCommentsBox.isSelected());
				settingsChanged();
			}
		});
//		useFirstRowAsColumnNamesBox.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				configuration.setFirstRowAsAttributeNames(useFirstRowAsColumnNamesBox.isSelected());
//				settingsChanged();
//			}
//		});
		useQuotesBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quoteCharacterTextField.setEnabled(useQuotesBox.isSelected());
				escapeCharacterTextField.setEnabled(useQuotesBox.isSelected());
				configuration.setUseQuotes(useQuotesBox.isSelected());
				settingsChanged();
			}
		});
		quoteCharacterTextField.addKeyListener(new KeyAdapter() {
			@Override public void keyReleased(KeyEvent e) { 
				configuration.setQuoteCharacter(quoteCharacterTextField.getText().charAt(0));
				settingsChanged();
			}
		});
		escapeCharacterTextField.addKeyListener(new KeyAdapter() {
			@Override public void keyReleased(KeyEvent e) { 
				configuration.setEscapeCharacter(escapeCharacterTextField.getText().charAt(0));
				settingsChanged();
			}
		});
		commentCharacterTextField.addKeyListener(new KeyAdapter() {
			@Override public void keyTyped(KeyEvent e) { 
				configuration.setCommentCharacters(commentCharacterTextField.getText());
				settingsChanged();
			}
		});		
		regexButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				regexTextField.setEnabled(regexButton.isSelected());
				configuration.setColumnSeparators(getSplitExpression());
				settingsChanged();
			}
		});

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(commaButton);
		buttonGroup.add(semicolonButton);
		buttonGroup.add(spaceButton);
		buttonGroup.add(tabButton);
		buttonGroup.add(regexButton);
		ActionListener separatorListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setColumnSeparators(getSplitExpression());
				settingsChanged();
			}
		};
		commaButton.addActionListener(separatorListener);
		semicolonButton.addActionListener(separatorListener);
		spaceButton.addActionListener(separatorListener);
		tabButton.addActionListener(separatorListener);
		regexButton.addActionListener(separatorListener);		
		regexTextField.addKeyListener(new KeyAdapter() {
			private Timer timer = new Timer(2000, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					timer.stop();
					settingsChanged();
				}
			});			
			@Override
			public void keyTyped(KeyEvent e) {
				timer.stop();
				timer.start();
			}
		});
	}

	private void makePanel() {
		JPanel optionPanel = new JPanel(ButtonDialog.createGridLayout(4, 1));
		
		JPanel tmpPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
		tmpPanel.add(new JLabel("File Encoding"));
		tmpPanel.add(encodingComboBox);
		optionPanel.add(tmpPanel);

		optionPanel.add(trimLinesBox);
		
		tmpPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
		tmpPanel.add(skipCommentsBox);
		tmpPanel.add(commentCharacterTextField);
		optionPanel.add(tmpPanel);

		optionPanel.setBorder(ButtonDialog.createTitledBorder("File Reading"));

		JPanel separationPanel = new JPanel(ButtonDialog.createGridLayout(5, 2));
		separationPanel.add(commaButton);
		separationPanel.add(spaceButton);
		separationPanel.add(semicolonButton);
		separationPanel.add(tabButton);
		separationPanel.add(regexButton);
		separationPanel.add(regexTextField);
		separationPanel.add(escapeCharacterLabel);
		separationPanel.add(escapeCharacterTextField);
		separationPanel.add(useQuotesBox);
		separationPanel.add(quoteCharacterTextField);

		separationPanel.setBorder(ButtonDialog.createTitledBorder("Column Separation"));

		JPanel parsingPanel = new JPanel(ButtonDialog.createGridLayout(1, 2));
		parsingPanel.add(optionPanel);
		parsingPanel.add(separationPanel);

		previewTable = new ExtendedJTable(false, false, false);
		//previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tablePane = new JScrollPane(previewTable);
		
		tablePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		tablePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		tablePane.setBorder(ButtonDialog.createBorder());

		panel = new JPanel(new BorderLayout(0, ButtonDialog.GAP));
		panel.add(parsingPanel, BorderLayout.NORTH);
		
		//panel.add(new JScrollPane(previewTable), BorderLayout.CENTER);
		panel.add(tablePane, BorderLayout.CENTER);
	}

	private String getSplitExpression() {
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

	private static UpdateQueue updateQueue;
	
	private void settingsChanged() {
		updateQueue.executeBackgroundJob(new ProgressThread("loading_data") {
			public void run() {
				try {
					final TableModel model = configuration.makePreviewTableModel(getProgressListener());
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							//panel.remove(tablePane);
							
							previewTable.setModel(model);
//							tablePane = new JScrollPane(previewTable);
//							tablePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//							tablePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//							tablePane.setBorder(ButtonDialog.createBorder());

							//panel.add(tablePane, BorderLayout.CENTER);

//							previewTable.revalidate();
//							tablePane.revalidate();
						}
					});
				} catch (Exception e) {
					ImportWizardUtils.showErrorMessage(configuration.getResourceName(), e.toString(), e);
				}
			}});
	}
	
	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {		
		updateQueue = new UpdateQueue("CSV-Preview-Fetcher");
		updateQueue.start();
		settingsChanged();
		return true;
	}
	
	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (updateQueue != null) {
			updateQueue.shutdown();
			updateQueue = null;
		}
		return super.performLeavingAction(direction);
	}
	
	@Override
	protected JComponent getComponent() {
		return panel;
	}

	@Override
	protected boolean canProceed() {
		return true;
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}
}
