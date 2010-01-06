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
package com.rapidminer.gui.tools.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.NoBugError;
import com.rapidminer.gui.dialog.BugAssistant;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;


/**
 * The error message dialog. Several buttons are provided in addition to the
 * error message. Details about the exception can be shown and an edit button
 * can jump to the source code if an editor was defined in the properties /
 * settings. In case of a non-expected error (i.e. all non-user errors) a button
 * for sending a bug report is also provided.
 * 
 * @author Ingo Mierswa, Simon Fischer, Tobias Malbrecht
 */
public class ExtendedErrorDialog extends ButtonDialog {

	private static final long serialVersionUID = -8136329951869702133L;

	private final JButton editButton = new JButton("Edit");
	
	/**
	 * Indicates if the exception message will be add to the 
	 * internationalized message. Default is <code>false</code>
	 */
	private boolean displayExceptionMessage = false;
	
	private Throwable error;

	/**
	 * Creates a dialog with the internationalized I18n-message from the
	 * given key and a panel for detailed stack trace.
	 * 
	 * @param key						the I18n-key which will be used to display the internationalized message
	 * @param error						the exception associated to this message
	 * @param displayExceptionMessage	indicates if the exception message will be displayed in the dialog or just in the detailed panel
	 * @param arguments					additional arguments for the internationalized message, which replace <code>{0}</code>, <code>{1}</code>, etcpp.
	 */
	public ExtendedErrorDialog( String key, Throwable error, boolean displayExceptionMessage, Object...arguments ) {
		super("error."+key,true , arguments);
		
		this.displayExceptionMessage = displayExceptionMessage;
		this.error = error;
		
		setLayout(new BorderLayout());
		addInfoPanel();
		
		boolean hasError = error!=null;
		boolean isBug = isBugReportException(error);
		
		JScrollPane detailedPane = hasError ? createDetailPanel( error ) : null;

		addButtons( hasError, isBug, detailedPane, error );
		
		setDefaultSize(DEFAULT_SIZE);
		setDefaultLocation();
		setTitle(getDialogTitle());			
		

	}
	
	/**
	 * Creates a dialog with the internationalized I18n-message from the
	 * given key and a panel for detailed stack trace.
	 * 
	 * @param key						the I18n-key which will be used to display the internationalized message
	 * @param error						the exception associated to this message
	 * @param arguments					additional arguments for the internationalized message, which replace <code>{0}</code>, <code>{1}</code>, etcpp.
	 */
	public ExtendedErrorDialog( String key, Throwable error, Object...arguments ) {
		this(key, error, false, arguments);
	}
	
	/**
	 * Creates a dialog with the internationalized I18n-message from the
	 * given key.
	 * 
	 * @param key						the I18n-key which will be used to display the internationalized message
	 * @param errorMessage				the error message associated to this message
	 * @param arguments					additional arguments for the internationalized message, which replace <code>{0}</code>, <code>{1}</code>, etcpp.
	 */
	public ExtendedErrorDialog( String key, String errorMessage, Object...arguments ) {
		super("error."+key, true , arguments);
		
		setLayout(new BorderLayout());
		addInfoPanel();
		
		boolean hasError = errorMessage!=null && !errorMessage.isEmpty();
		boolean isBug = false;
		
		JScrollPane detailedPane = hasError ? createDetailPanel( errorMessage ) : null;

		addButtons( hasError, isBug, detailedPane, null );
		
		setDefaultSize(DEFAULT_SIZE);
		setDefaultLocation();
		setTitle(getDialogTitle());
	}

	
	/**
	 * Creates and adds the InfoPanel with the internationalized message
	 * to the dialog. 
	 */
	private JPanel addInfoPanel() {
		final JPanel infoPanel = makeInfoPanel(
					getInfoText(), 		// Add already internationalized message
					SwingTools.createIcon("48/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.error.icon")));
		add(infoPanel, BorderLayout.NORTH);
		
		return infoPanel;
	}
	
	/**
	 * Creates a Panel for the error details and attaches the exception to it, but doesn't
	 * add the Panel to the dialog.
	 * 
	 * @param error
	 * @return
	 */
	private JScrollPane createDetailPanel( Throwable error ) {
		StackTraceList stl = new StackTraceList(error);
		JScrollPane detailPane = new ExtendedJScrollPane(stl);
		detailPane.setPreferredSize(new Dimension(getWidth(), 200));
		return detailPane;
	}
	
	/**
	 * Creates a Panel for the error details and attaches the error message to it, but doesn't
	 * add the Panel to the dialog.
	 * 
	 * @param error
	 * @return
	 */
	private JScrollPane createDetailPanel( String errorMessage ) {
		
		JTextArea textArea = new JTextArea(errorMessage);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		JScrollPane detailPane = new ExtendedJScrollPane(textArea);
		detailPane.setPreferredSize(new Dimension(getWidth(), 200));
		return detailPane;
	}
	
	/**
	 * Adds all necessary buttons to the dialog.
	 * 
	 * @param hasError
	 * @param isBug
	 * @param detailedPane	the Panel which will be shown, if the user clicks on the 'Show Details' Button
	 * @param error			The error occurred
	 */
	private void addButtons( boolean hasError, boolean isBug, final JScrollPane detailedPane, final Throwable error ) {
		Collection<AbstractButton> buttons = new LinkedList<AbstractButton>();
		if( hasError ) {
			buttons.add(new JToggleButton(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.error.show_details.label"),
					SwingTools.createIcon("24/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.error.show_details.icon"))) {
				private static final long serialVersionUID = 8889251336231161227L;

				private boolean detailsShown = false;

				{
					setSelected(false);
					addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if (detailsShown) {
								getContentPane().remove(detailedPane);
							} else {
								getContentPane().add(detailedPane, BorderLayout.CENTER);
								pack();
							}
							pack();
							detailsShown = !detailsShown;
						}
					});	
				}
			});
		}
		if( isBug ) {
			buttons.add(new JButton(new ResourceAction("send_bugreport") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					new BugAssistant(error).setVisible(true);
				}
			}));
		}
		
		buttons.add(makeCloseButton());
		
		add(makeButtonPanel(buttons), BorderLayout.SOUTH);
	}
	
	/**
	 * Returns <code>true</code> if this is a "real" bug, <code>false</code>
	 * otherwise.
	 * 
	 * @param t
	 * @return
	 */
	protected boolean isBugReportException(Throwable t) {
		return !(t instanceof NoBugError ||
				 t instanceof XMLException);
	}
	
	/**
	 * Overrides the {@link ButtonDialog} method to add the exception message
	 * to the internationalized message
	 */
	@Override
	protected String getInfoText() {
		
		if( !displayExceptionMessage )
			return super.getInfoText();
		else if ( error != null ) {
			
			StringBuilder infoText = new StringBuilder();
			infoText.append("<p>");
			infoText.append(super.getInfoText());
			infoText.append("</p>");

//			infoText.append("<p>");
//			infoText.append("<b>Exception: </b>");
//			infoText.append(error.getClass().getSimpleName());
//			infoText.append("</p>");
			

			String message = error.getMessage();
			if (message == null) {
				message = error.toString();
			}
			// Find deepest non-null message
			//			Throwable e = error;
//			while (e != null) {
//				if (e.getMessage() != null) {
//					message = e.getLocalizedMessage();
//				}
//				e = e.getCause();
//			}
//			if (message == null) {
//				message = error.toString();
//			}			
			
			if (!"null".equals(message)) {				
				infoText.append("<p><strong>Reason: </strong>");
				infoText.append(message);
				infoText.append("</p>");
			}
			return infoText.toString();
		} else {
			return super.getInfoText();
		}
		
	}
	
	private static class FormattedStackTraceElement {

		private final StackTraceElement ste;

		private FormattedStackTraceElement(StackTraceElement ste) {
			this.ste = ste;
		}

		@Override
		public String toString() {
			return "  " + ste;
		}
	}

	private class StackTraceList extends JList {

		private static final long serialVersionUID = -2482220036723949144L;

		public StackTraceList(Throwable t) {
			super(new DefaultListModel());
			setFont(getFont().deriveFont(Font.PLAIN));
			setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			appendAllStackTraces(t);
			addListSelectionListener(new ListSelectionListener() {

				public void valueChanged(ListSelectionEvent e) {
					if (getSelectedIndex() >= 0) {
						if (!(getSelectedValue() instanceof FormattedStackTraceElement)) {
							editButton.setEnabled(false);
						} else {
							editButton.setEnabled(true);
						}
					} else {
						editButton.setEnabled(true);
					}
				}
			});
		}

		private DefaultListModel model() {
			return (DefaultListModel) getModel();
		}

		private void appendAllStackTraces(Throwable throwable) {
			while (throwable != null) {
				appendStackTrace(throwable);
				throwable = throwable.getCause();
				if (throwable != null) {
					model().addElement("");
					model().addElement("Cause");
				}
			}
		}

		private void appendStackTrace(Throwable throwable) {
			model().addElement("Exception: " + throwable.getClass().getName());
			model().addElement("Message: " + throwable.getMessage());
			model().addElement("Stack trace:" + Tools.getLineSeparator());
			for (int i = 0; i < throwable.getStackTrace().length; i++) {
				model().addElement(new FormattedStackTraceElement(throwable.getStackTrace()[i]));
			}
		}
	}
}
