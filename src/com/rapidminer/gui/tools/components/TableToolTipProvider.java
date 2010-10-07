package com.rapidminer.gui.tools.components;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JTable;

import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.tools.container.Pair;

/** A tool tip provider showing the contents of a table cell (that might otherwise not fit into the cell.)
 * 
 * @author Simon Fischer
 *
 */
public class TableToolTipProvider implements TipProvider {

	private JTable table;
	
	@Override
	public Component getCustomComponent(Object id) {
		return null;
	}

	@Override
	public Object getIdUnder(Point point) {
		Pair<Integer, Integer> cellId = new Pair<Integer, Integer>(table.columnAtPoint(point), table.rowAtPoint(point));
		return cellId;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getTip(Object id) {
		Pair<Integer, Integer> cellId = (Pair<Integer, Integer>) id;
		final Object value = table.getValueAt(cellId.getFirst(), cellId.getSecond());
		if (value != null) {
			return value.toString();
		} else {
			return value.toString();
		}
		//return getToolTipText(cellId.getFirst(), cellId.getSecond());
	}
}
