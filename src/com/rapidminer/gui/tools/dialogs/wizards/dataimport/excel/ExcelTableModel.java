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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import jxl.Sheet;

import com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel.ExcelWorkbookPane.ExcelWorkbookSelection;

/**
 * 
 * @author Tobias Malbrecht
 */
public class ExcelTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -2650777734917514059L;

	private Sheet excelSheet;

	private ExcelWorkbookSelection reductionSelection;

	private List<String> columnNames;
	
	public ExcelTableModel(Sheet sheet) {
		excelSheet = sheet;
	}
	
	public int getColumnCount() {
		if (reductionSelection == null) {
			return excelSheet.getColumns();
		} else {
			return reductionSelection.getColumnIndexEnd() - reductionSelection.getColumnIndexStart() + 1;
		}
	}

	public int getRowCount() {
		if (reductionSelection == null) {
			return excelSheet.getRows();
		} else {
			return reductionSelection.getRowIndexEnd() - reductionSelection.getRowIndexStart() + 1;
		}
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (reductionSelection == null) {
			return excelSheet.getCell(columnIndex, rowIndex).getContents();
		} else {
			return excelSheet.getCell(columnIndex + reductionSelection.getColumnIndexStart(), rowIndex + reductionSelection.getRowIndexStart()).getContents();
		}
	}
	@Override
	public String getColumnName(int column) {
		if (columnNames == null) {
			if (reductionSelection != null) {
				column += reductionSelection.getColumnIndexStart();
			}
			StringBuffer buffer = new StringBuffer();
			int currentNumber = column % 26;
			buffer.append(((char) (currentNumber + 65)));
			column -= currentNumber;
			while (column > 0) {
				column /= 26;
				currentNumber = column % 26;
				buffer.append(((char) (currentNumber + 65)));
				column -= currentNumber;
			}
			return buffer.toString();
		} else {
			return columnNames.get(column);
		}
	}
	public void createView(ExcelWorkbookSelection selection) {
		reductionSelection = selection;
	}
	public void resetReduction() {
		reductionSelection = null;
	}
	public void setNames(List<String> names) {
		columnNames = names;
	}
	public void resetNames() {
		columnNames = null;
	}

}
