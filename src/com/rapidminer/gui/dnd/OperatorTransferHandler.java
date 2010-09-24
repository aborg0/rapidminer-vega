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
package com.rapidminer.gui.dnd;

import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

import com.rapidminer.gui.operatortree.actions.CutCopyPasteAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.LogService;

/** Transfer handler that supports dragging operators.
 * 
 * @author Simon Fischer
 *
 */
public abstract class OperatorTransferHandler extends TransferHandler {
	
	private static final long serialVersionUID = 1L;

	/** Returns a list of operators selected for dragging out of this component. */
	protected abstract List<Operator> getDraggedOperators();
	
	// General 
	
	@Override
	public Icon getVisualRepresentation(Transferable transferable) {
		if (transferable instanceof TransferableOperator) {
			Operator op;
			try {
				op = (Operator) transferable.getTransferData(transferable.getTransferDataFlavors()[0]);
				return op.getOperatorDescription().getIcon();
			} catch (Exception e) {						
				LogService.getRoot().log(Level.WARNING, "Error while dragging: " + e, e);
				return null;
			}        			
		} else {
			return null;
		}
	}

	// Drag Support
	
	@Override
	public int getSourceActions(JComponent c) {
	    return COPY_OR_MOVE;
	}

	@Override
	public Transferable createTransferable(JComponent c) {
	    List<Operator> operators = getDraggedOperators();
	    if ((operators == null) || operators.isEmpty()) {
	    	return null;
	    }
	    // remove children
	    Iterator<Operator> i = operators.iterator();
	    while (i.hasNext()) {
	    	Operator op = i.next();
	    	Operator parent = op.getParent();
	    	while (parent != null) {
	    		if (operators.contains(parent)) {
	    			i.remove();
	    			continue;
	    		}
	    		parent = parent.getParent();
	    	}
	    }
		return new TransferableOperator(operators.toArray(new Operator[operators.size()]));
	}
	
	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		if (data instanceof TransferableOperator) {
			TransferableOperator top = (TransferableOperator) data;
			switch (action) {
			case MOVE:
				for (Operator operator : top.getOperators()) {
					operator.removeAndKeepConnections(Arrays.asList(top.getOperators()));
				}
				break;
			default:
				// do nothing
			}
		}
		super.exportDone(source, data, action);			
	}

	public static void installMenuItems(JPopupMenu editmenu) {
		editmenu.add(CutCopyPasteAction.CUT_ACTION);
		editmenu.add(CutCopyPasteAction.COPY_ACTION);
		editmenu.add(CutCopyPasteAction.PASTE_ACTION);
	}

	public static void installMenuItems(JMenu editmenu) {
		editmenu.add(CutCopyPasteAction.CUT_ACTION);
		editmenu.add(CutCopyPasteAction.COPY_ACTION);
		editmenu.add(CutCopyPasteAction.PASTE_ACTION);
	}

	public static void addToActionMap(JComponent component) {
		ActionMap actionMap = component.getActionMap();
		actionMap.put(TransferHandler.getCutAction().getValue(Action.NAME),	TransferHandler.getCutAction());
		actionMap.put(TransferHandler.getCopyAction().getValue(Action.NAME),	TransferHandler.getCopyAction());
		actionMap.put(TransferHandler.getPasteAction().getValue(Action.NAME),	TransferHandler.getPasteAction());

		// only required if you have not set the menu accelerators
		InputMap inputMap = component.getInputMap();
		inputMap.put(KeyStroke.getKeyStroke("ctrl X"),	TransferHandler.getCutAction().getValue(Action.NAME));
		inputMap.put(KeyStroke.getKeyStroke("ctrl C"),	TransferHandler.getCopyAction().getValue(Action.NAME));
		inputMap.put(KeyStroke.getKeyStroke("ctrl V"),	TransferHandler.getPasteAction().getValue(Action.NAME));
	}
}
