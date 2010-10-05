package com.rapidminer.operator.nio.model;

import javax.swing.table.AbstractTableModel;

import com.rapidminer.operator.OperatorException;
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
	
	public void read(DataResultSet resultSet, ProgressListener listener) throws OperatorException, ParseException {
		resultSet.reset(listener);
		int row = 0;
		while (resultSet.hasNext() && (row < data.length)) {
			resultSet.next(listener);
			data[row] = new String[resultSet.getNumberOfColumns()];
			for (int i = 0; i < data[row].length; i++) {
				data[row][i] = resultSet.getString(i);
			}
		}
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public int getColumnCount() {
		return data[0].length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return data[rowIndex][columnIndex];
	}
	
}
