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
package com.rapidminer.operator.nio;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;

/**
 * This is a pane, showing the contents of a complete excel workbook. There's one tab per sheet.
 * 
 * @author Tobias Malbrecht, Sebastian Land
 */
public class ExcelWorkbookPane extends JPanel {

	public class ExcelWorkbookSelection {
		private int sheetIndex;
		private int columnIndexStart;
		private int rowIndexStart;
		private int columnIndexEnd;
		private int rowIndexEnd;

		public ExcelWorkbookSelection(int sheetIndex, int columnIndexStart, int rowIndexStart, int columnIndexEnd, int rowIndexEnd) {
			this.sheetIndex = sheetIndex;
			this.columnIndexStart = columnIndexStart;
			this.rowIndexStart = rowIndexStart;
			this.columnIndexEnd = columnIndexEnd;
			this.rowIndexEnd = rowIndexEnd;
		}

		@Override
		public String toString() {
			return sheetIndex + ": " + columnIndexStart + ":" + rowIndexStart + " - " + columnIndexEnd + ":" + rowIndexEnd;
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
	}

	public class ExcelSheetTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		private Sheet sheet;

		public ExcelSheetTableModel(Sheet sheet) {
			this.sheet = sheet;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return sheet.getCell(columnIndex, rowIndex).getContents();
		}

		@Override
		public int getRowCount() {
			return sheet.getRows();
		}

		@Override
		public int getColumnCount() {
			return sheet.getColumns();
		}

		@Override
		public String getColumnName(int columnIndex) {
			return Tools.getExcelColumnName(columnIndex);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
	}

	private static final long serialVersionUID = 9179757216097316344L;

	private ExcelResultSetConfiguration configuration;

	private ExtendedJTabbedPane sheetsPane;
	private ExtendedJTable[] tables;
	private ExcelWorkbookSelection selectedView;

	public ExcelWorkbookPane(WizardStep wizardStep, ExcelResultSetConfiguration configuration) {
		super();
		this.configuration = configuration;

		// creating gui
		sheetsPane = new ExtendedJTabbedPane();
		sheetsPane.setBorder(null);
		this.setLayout(new BorderLayout());
		this.add(sheetsPane);
	}

	public void loadWorkbook() {
		sheetsPane.removeAll();
		new ProgressThread("load_workbook") {
			@Override
			public void run() {
				// initializing progress
				getProgressListener().setTotal(1);

				// loading workbook if necessary
				try {
					Workbook workbook;
					if (configuration.hasWorkbook()) {
						workbook = configuration.getWorkbook();
					} else {
						File file = configuration.getFile();

						try {
							workbook = Workbook.getWorkbook(file);
						} catch (BiffException e1) {
							LogService.getRoot().log(Level.WARNING, "Error loading workbook: " + e1, e1);
							return;
						} catch (IOException e1) {
							LogService.getRoot().log(Level.WARNING, "Error loading workbook: " + e1, e1);
							return;
						}
					}
					final Workbook finalWorkbook = workbook;

					// now add everything to gui 
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							// add dummy
							JPanel dummy = new JPanel();
							dummy.add(new JLabel("Loading Excel Sheets"));
							sheetsPane.addTab("  ", dummy);
							tables = new ExtendedJTable[finalWorkbook.getNumberOfSheets()];

							String[] sheetNames = finalWorkbook.getSheetNames();
							for (int sheetIndex = 0; sheetIndex < finalWorkbook.getNumberOfSheets(); sheetIndex++) {
								ExcelSheetTableModel sheetModel = new ExcelSheetTableModel(finalWorkbook.getSheet(sheetIndex));
								tables[sheetIndex] = new ExtendedJTable(sheetModel, false, false);
								tables[sheetIndex].setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
								tables[sheetIndex].setBorder(null);

								// momentary disable selection in tables
								tables[sheetIndex].setRowSelectionAllowed(false);
								tables[sheetIndex].setColumnSelectionAllowed(false);
								tables[sheetIndex].setCellSelectionEnabled(false);

								// add table to gui
								ExtendedJScrollPane pane = new ExtendedJScrollPane(tables[sheetIndex]);
								pane.setBorder(null);
								if (sheetIndex == 0) {
									sheetsPane.removeAll();
								}
								sheetsPane.addTab(sheetNames[sheetIndex], pane);
							}
						}
					});
				} finally {
					getProgressListener().complete();
				}
			}
		}.start();
	}

	public ExcelWorkbookSelection getSelection() {
		if (selectedView == null) {
			int sheetIndex = sheetsPane.getSelectedIndex();
			int columnIndexStart = tables[sheetIndex].getSelectedColumn();
			int rowIndexStart = tables[sheetIndex].getSelectedRow();
			int columnIndexEnd = columnIndexStart + tables[sheetIndex].getSelectedColumnCount() - 1;
			int rowIndexEnd = rowIndexStart + tables[sheetIndex].getSelectedRowCount() - 1;
			if (columnIndexStart == -1) {
				// then use complete selected table
				return new ExcelWorkbookSelection(sheetIndex, 0, 0, tables[sheetIndex].getColumnCount() - 1, tables[sheetIndex].getRowCount() - 1);
			} else {
				return new ExcelWorkbookSelection(sheetIndex, columnIndexStart, rowIndexStart, columnIndexEnd, rowIndexEnd);
			}
		} else {
			int sheetIndex = selectedView.getSheetIndex();
			int columnIndexStart = tables[0].getSelectedColumn() + selectedView.getColumnIndexStart();
			int rowIndexStart = tables[0].getSelectedRow() + selectedView.getRowIndexStart();
			int columnIndexEnd = columnIndexStart + tables[0].getSelectedColumnCount() - 1 + selectedView.getColumnIndexStart();
			int rowIndexEnd = rowIndexStart + tables[0].getSelectedRowCount() - 1 + selectedView.getRowIndexStart();
			if (columnIndexStart == -1) {
				// then use complete selected table
				return new ExcelWorkbookSelection(sheetIndex, selectedView.getColumnIndexStart(), selectedView.getRowIndexStart(), selectedView.getColumnIndexEnd(), selectedView.getRowIndexEnd());
			} else {
				return new ExcelWorkbookSelection(sheetIndex, columnIndexStart, rowIndexStart, columnIndexEnd, rowIndexEnd);
			}
		}
	}
}
