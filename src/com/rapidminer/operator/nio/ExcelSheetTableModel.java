package com.rapidminer.operator.nio;

import java.io.IOException;

import javax.swing.table.AbstractTableModel;

import jxl.Sheet;
import jxl.read.biff.BiffException;

import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.tools.Tools;

/** Returns values backed by an operned excel workbook.
 * 
 * @author Simon Fischer
 *
 */
public class ExcelSheetTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	private Sheet sheet;

	private ExcelResultSetConfiguration config;
	
	public ExcelSheetTableModel(Sheet sheet) {
		this.sheet = sheet;
	}

	public ExcelSheetTableModel(ExcelResultSetConfiguration excelResultSetConfiguration) throws IndexOutOfBoundsException, BiffException, IOException {
		this.config = excelResultSetConfiguration;
		this.sheet = config.getWorkbook().getSheet(config.getSheet()-1);
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (config != null) {
			return sheet.getCell(columnIndex - config.getColumnOffset(), rowIndex - config.getRowOffset()).getContents();
		} else {
			return sheet.getCell(columnIndex, rowIndex).getContents();
		}
	}

	@Override
	public int getRowCount() {
		if (config != null) {
			return config.getRowLast() - config.getRowOffset() + 1;
		} else {
			return sheet.getRows();
		}
	}

	@Override
	public int getColumnCount() {
		if (config != null) {
			return config.getColumnLast() - config.getColumnOffset() + 1;
		} else {
			return sheet.getColumns();
		}
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (config != null) {
			return Tools.getExcelColumnName(columnIndex + config.getColumnOffset());
		} else {
			return Tools.getExcelColumnName(columnIndex);
		}
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