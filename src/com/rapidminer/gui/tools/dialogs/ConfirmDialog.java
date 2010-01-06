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

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * @author Tobias Malbrecht
 */
public class ConfirmDialog extends ButtonDialog {

	private static final long serialVersionUID = -5825873580778775409L;
	
	public static final int OK_OPTION = JOptionPane.OK_OPTION;
	
	public static final int YES_OPTION = JOptionPane.YES_OPTION;
	
	public static final int NO_OPTION = JOptionPane.NO_OPTION;
	
	public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
	
	public static final int CLOSED_OPTION = JOptionPane.CANCEL_OPTION;
	
	public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;
	
	public static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;
	
	public static final int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;
	
	
	private int returnOption = CANCEL_OPTION;
	
	public ConfirmDialog(String key, int mode, Object...arguments) {
//		super(ApplicationFrame.getApplicationFrame(), "confirm." + key, true, arguments);
		super("confirm." + key, true, arguments);
		Collection<AbstractButton> buttons = new LinkedList<AbstractButton>();
		switch (mode) {
		case OK_CANCEL_OPTION:
			buttons.add(makeOkButton());
			buttons.add(makeCancelButton());
			break;
		case YES_NO_OPTION:
			buttons.add(makeYesButton());
			buttons.add(makeNoButton());
			break;
		case YES_NO_CANCEL_OPTION:
			buttons.add(makeYesButton());
			buttons.add(makeNoButton());
			buttons.add(makeCancelButton());
			break;
		}
		layoutDefault(null, buttons);
	}
	
	@Override
	protected Icon getInfoIcon() {
		String iconKey = I18N.getMessageOrNull(I18N.getGUIBundle(), getKey() + ".icon");
		if (iconKey == null) {
			return SwingTools.createIcon("48/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.confirm.icon"));
		} else {
			return SwingTools.createIcon("48/" + iconKey);
		}
	}

	@Override
	protected JButton makeOkButton() {
		return new JButton(new ResourceAction("ok") {
			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void actionPerformed(ActionEvent e) {
				returnOption = OK_OPTION;
				ok();
			}
		});
	}

	@Override
	protected JButton makeCancelButton() {
		return new JButton(new ResourceAction("cancel") {
			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void actionPerformed(ActionEvent e) {
				returnOption = CANCEL_OPTION;
				cancel();
			}
		});
	}
	
	protected JButton makeYesButton() {
		return new JButton(new ResourceAction("confirm.yes") {
			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void actionPerformed(ActionEvent e) {
				returnOption = YES_OPTION;
				yes();
			}
		});
	}
	
	protected JButton makeNoButton() {
		return new JButton(new ResourceAction("confirm.no") {
			private static final long serialVersionUID = -8887199234055845095L;

			@Override
			public void actionPerformed(ActionEvent e) {
				returnOption = NO_OPTION;
				no();
			}
		});
	}
	
	@Override
	protected void ok() {
		dispose();
	}
	
	@Override
	protected void cancel() {
		dispose();
	}
	
	protected void yes() {
		dispose();
	}
	
	protected void no() {
		dispose();
	}

	public int getReturnOption() {
		return returnOption;
	}
}
