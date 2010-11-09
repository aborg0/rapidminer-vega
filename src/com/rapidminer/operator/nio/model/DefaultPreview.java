package com.rapidminer.operator.nio.model;

import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.ImportWizardUtils;
import com.rapidminer.tools.ProgressListener;

/** Data container and table model for previews. Reads a few lines from a
 *  {@link DataResultSet} to display them.
 * 
 * @author Simon Fischer
 *
 */
public class DefaultPreview extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private String[][] data;

	private String[] columnNames;
	
	private int previewSize = ImportWizardUtils.getPreviewLength();
	
	public DefaultPreview(DataResultSet resultSet, ProgressListener l) throws OperatorException, ParseException {		
		read(resultSet, l);
	}
	
	public void read(DataResultSet resultSet, ProgressListener listener) throws OperatorException, ParseException {
		if (listener != null) {
			listener.setTotal(previewSize);
		}
		List<String[]> dataList = new LinkedList<String[]>();
		resultSet.reset(listener);
		while (resultSet.hasNext() && (dataList.size() < previewSize)) {
			resultSet.next(listener);
			String[] row = new String[resultSet.getNumberOfColumns()];
			for (int i = 0; i < row.length; i++) {
				row[i] = resultSet.getString(i);
			}
			dataList.add(row);			
			if (listener != null) {
				listener.setCompleted(dataList.size());
			}
		}
		// copy to array since will be accessed by index
		this.data = dataList.toArray(new String[dataList.size()][]);
		columnNames = resultSet.getColumnNames();
		if (listener != null) {
			listener.complete();
		}
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}
	
	@Override
	public int getColumnCount() {
		if (columnNames != null) {
			return columnNames.length;
		} else {
			if ((data != null) || (data.length > 0)) {
				return data[0].length;
			} else {
				return 1;
			}
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final String[] row = data[rowIndex];
		if (row == null) {
			return null;
		} else if (columnIndex >= row.length) {
			return null;
		} else {
			return row[columnIndex];
		}
	}
	
}
