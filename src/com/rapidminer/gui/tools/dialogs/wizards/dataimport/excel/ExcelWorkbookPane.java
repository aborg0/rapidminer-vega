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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel;

import java.awt.BorderLayout;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import jxl.Sheet;
import jxl.Workbook;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.tools.LogService;

/**
 * 
 * @author Tobias Malbrecht
 */
public class ExcelWorkbookPane extends JPanel {
	
	public class ExcelWorkbookSelection {
		private int sheetIndex;
		private int columnIndexStart;
		private int rowIndexStart;
		private int columnIndexEnd;
		private int rowIndexEnd;

		public ExcelWorkbookSelection(int sheetIndex, int columnIndexStart,
				int rowIndexStart, int columnIndexEnd, int rowIndexEnd) {
			this.sheetIndex = sheetIndex;
			this.columnIndexStart = columnIndexStart;
			this.rowIndexStart = rowIndexStart;
			this.columnIndexEnd = columnIndexEnd;
			this.rowIndexEnd = rowIndexEnd;
		}

		@Override
		public String toString() {
			return sheetIndex + ": " + columnIndexStart + ":" + rowIndexStart
					+ " - " + columnIndexEnd + ":" + rowIndexEnd;
		}

		public int getSheetIndex() {
			return sheetIndex;
		}

		public int getColumnIndexEnd() {
			return columnIndexEnd;
		}

		public int getColumnIndexStart() {
			return columnIndexStart;
		}

		public int getRowIndexEnd() {
			return rowIndexEnd;
		}

		public int getRowIndexStart() {
			return rowIndexStart;
		}

		public int getSelectionWidth() {
			return columnIndexEnd - columnIndexStart + 1;
		}

		public int getSelectionHeight() {
			return rowIndexEnd - rowIndexStart + 1;
		}

		public Map<Integer,String> getAnnotationMap() {
			return ((ExcelTableModel)tables[sheetIndex].getModel()).getAnnotationMap();
		}
	}

	private static final long serialVersionUID = 9179757216097316344L;

	private Workbook excelWorkbook;
	private ExtendedJTabbedPane sheetsPane;
	private JTable[] tables;
	private ExcelWorkbookSelection selectedView;

	private WizardStep wizardStep;
	
	public ExcelWorkbookPane(WizardStep wizardStep) {
		super();
		this.wizardStep = wizardStep;
		sheetsPane = new ExtendedJTabbedPane();
		sheetsPane.setBorder(null);
		this.setLayout(new BorderLayout());
		this.add(sheetsPane);
	}

	public ExcelWorkbookPane(String fileName) {
		this((WizardStep)null);
		loadWorkbook(fileName);
	}
	
	public ExcelWorkbookPane(File file) {
		this((WizardStep)null);
		loadWorkbook(file);
	}

	public void loadWorkbook(String fileName) {
		loadWorkbook(new File(fileName));
	}

	public void loadWorkbook(File file) {
		Workbook workbook = null;
		try {
			workbook = Workbook.getWorkbook(file);
		} catch (Exception e) {
			// TODO correct error handling
			LogService.getRoot().log(Level.WARNING, "Error loading workbook: "+e, e);
		}
		excelWorkbook = workbook;
		loadWorkbook();
	}

	public ExcelWorkbookSelection getSelection() {
		if (selectedView == null) {
			int sheetIndex = sheetsPane.getSelectedIndex();
			int columnIndexStart = tables[sheetIndex].getSelectedColumn();
			int rowIndexStart = tables[sheetIndex].getSelectedRow();
			int columnIndexEnd = columnIndexStart
					+ tables[sheetIndex].getSelectedColumnCount() - 1;
			int rowIndexEnd = rowIndexStart
					+ tables[sheetIndex].getSelectedRowCount() - 1;
			if (columnIndexStart == -1) {
				// then use complete selected table
				return new ExcelWorkbookSelection(sheetIndex, 0, 0,
						tables[sheetIndex].getColumnCount() - 1,
						tables[sheetIndex].getRowCount() - 1);
			} else {
				return new ExcelWorkbookSelection(sheetIndex, columnIndexStart,
						rowIndexStart, columnIndexEnd, rowIndexEnd);
			}
		} else {
			int sheetIndex = selectedView.getSheetIndex();
			int columnIndexStart = tables[0].getSelectedColumn()
					+ selectedView.getColumnIndexStart();
			int rowIndexStart = tables[0].getSelectedRow()
					+ selectedView.getRowIndexStart();
			int columnIndexEnd = columnIndexStart
					+ tables[0].getSelectedColumnCount() - 1
					+ selectedView.getColumnIndexStart();
			int rowIndexEnd = rowIndexStart + tables[0].getSelectedRowCount()
					- 1 + selectedView.getRowIndexStart();
			if (columnIndexStart == -1) {
				// then use complete selected table
				return new ExcelWorkbookSelection(sheetIndex, selectedView
						.getColumnIndexStart(),
						selectedView.getRowIndexStart(), selectedView
								.getColumnIndexEnd(), selectedView
								.getRowIndexEnd());
			} else {
				return new ExcelWorkbookSelection(sheetIndex, columnIndexStart,
						rowIndexStart, columnIndexEnd, rowIndexEnd);
			}
		}
	}

	public void createView(ExcelWorkbookSelection selection) {
		createView(selection, null);
	}

	public void createView(ExcelWorkbookSelection selection,
			List<String> columnNames) {
		sheetsPane.removeAll();
		tables = new JTable[1];
		int sheetIndex = selection.getSheetIndex();
		Sheet sheet = excelWorkbook.getSheet(sheetIndex);
		ExcelTableModel sheetModel = new ExcelTableModel(sheet);
		sheetModel.createView(selection);
		sheetModel.setNames(columnNames);
		tables[0] = new ExtendedJTable(sheetModel, false, false);		
		tables[0].setBorder(null);
		tables[0].setAutoResizeMode(JTable.AUTO_RESIZE_OFF);		
		tables[0].getColumnModel().getColumn(0).setCellEditor(new AnnotationCellEditor());
		/*
	    tables[0].setMaximumSize(new Dimension(5000, 50000));
		TableColumnModel columnModel = tables[0].getColumnModel();
		for (int columnIndex = 0; columnIndex < tables[0].getColumnCount(); columnIndex++) {
			columnModel.getColumn(columnIndex).setPreferredWidth(
					sheet.getColumnView(columnIndex).getSize() / 36);
		}
		*/
		//tables[sheetIndex].doLayout();
		ExtendedJScrollPane pane = new ExtendedJScrollPane(tables[0]);
		pane.setBorder(null);
		sheetsPane.addTab(sheet.getName(), pane);
		selectedView = selection;
	}

	public void resetView() {
		selectedView = null;
		loadWorkbook();
	}

	public Workbook getWorkbook() {
		return excelWorkbook;
	}

	private void loadWorkbook() {
		sheetsPane.removeAll();
		tables = new JTable[excelWorkbook.getNumberOfSheets()];
		for (int sheetIndex = 0; sheetIndex < excelWorkbook.getNumberOfSheets(); sheetIndex++) {
			Sheet sheet = excelWorkbook.getSheet(sheetIndex);
			TableModel sheetModel = new ExcelTableModel(sheet);
			sheetModel.addTableModelListener(new TableModelListener() {
				@Override
				public void tableChanged(TableModelEvent e) {
					if (wizardStep != null) {
						wizardStep.fireStateChanged();
					}
				}
			});
			tables[sheetIndex] = new ExtendedJTable(sheetModel, false, false);
			tables[sheetIndex].setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			tables[sheetIndex].setBorder(null);

			tables[sheetIndex].getColumnModel().getColumn(0).setCellEditor(new AnnotationCellEditor());
			
			// momentary disable selection in tables
			tables[sheetIndex].setRowSelectionAllowed(false);
			tables[sheetIndex].setColumnSelectionAllowed(false);
			tables[sheetIndex].setCellSelectionEnabled(false);
			/*
			tables[sheetIndex].setMaximumSize(new Dimension(5000, 50000));
			TableColumnModel columnModel = tables[sheetIndex].getColumnModel();
			for (int columnIndex = 0; columnIndex < tables[sheetIndex]
					.getColumnCount(); columnIndex++) {
				columnModel.getColumn(columnIndex).setPreferredWidth(
						sheet.getColumnView(columnIndex).getSize() / 36);
			}
			tables[sheetIndex].doLayout();
			*/
			ExtendedJScrollPane pane = new ExtendedJScrollPane(tables[sheetIndex]);
			pane.setBorder(null);
			sheetsPane.addTab(sheet.getName(), pane);
		}
	}

	public String getColumnName(int sheet, int index) {
		return tables[sheet].getColumnName(index);
	}

	public boolean displayErrorStatus(JLabel label) {
		if (tables == null) {
			return true;
		}
		Set<String> usedAnnotations = new HashSet<String>();
		for (String an : ((ExcelTableModel)tables[getSelection().getSheetIndex()].getModel()).getAnnotationMap().values()) {
			if (AnnotationCellEditor.NONE.equals(an)) {
				continue;
			}
			if (usedAnnotations.contains(an)) {
				label.setText("Duplicate annotation: "+an);
				return false;	
			} else {
				usedAnnotations.add(an);
			}
		}		
		label.setText("");
		return true;
	}
}
