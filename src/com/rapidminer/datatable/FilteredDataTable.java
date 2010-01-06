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
package com.rapidminer.datatable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This DataTable filters the contained rows using a stack of FilterConditions. Each time the stack is modified, it
 * informs it's DataTableFilteredListener that they need to update the table.
 * 
 * @author Sebastian Land
 */
public class FilteredDataTable extends AbstractDataTable implements DataTableListener {

	public static interface DataTableFilteredListener {
		/**
		 * This method is called by a datatable, if its content is changed.
		 */
		public void informDataTableChange(DataTable dataTable);
	}

	private List<DataTableFilteredListener> listeners = new LinkedList<DataTableFilteredListener>();
	
	private DataTable parentTable;
	private int[] selectedIndices;
	private int numberOfSelectedRows;
	private ArrayList<DataTableFilterCondition> conditionStack = new ArrayList<DataTableFilterCondition>();
	
	public FilteredDataTable(DataTable parentDataTable) {
		super(parentDataTable.getName());
		this.parentTable = parentDataTable;
		
		// building initial selected indices: All
		selectedIndices = new int[parentTable.getNumberOfRows()];
		for (int i = 0; i < selectedIndices.length; i++) {
			selectedIndices[i] = i;
		}
		numberOfSelectedRows = parentTable.getNumberOfRows();
		
		parentTable.addDataTableListener(this);
	}

	@Override
	public Iterator<DataTableRow> iterator() {
		return new Iterator<DataTableRow>() {
			int nextRow = 0;
			@Override
			public boolean hasNext() {
				return nextRow < getNumberOfRows();
			}

			@Override
			public DataTableRow next() {
				DataTableRow row = getRow(nextRow);
				nextRow++;
				return row;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("remove() not suppported by FilterDataTable");
			}
		};
	}

	@Override
	public DataTableRow getRow(int index) {
		if (index < numberOfSelectedRows)
			return parentTable.getRow(selectedIndices[index]);
		else 
			throw new ArrayIndexOutOfBoundsException("Index exceeds filtered range: " + index);
	}

	@Override
	public int getNumberOfRows() {
		return numberOfSelectedRows;
	}

	public void addCondition(DataTableFilterCondition condtion) {
		conditionStack.add(condtion);
		updateSelection();
	}
	
	public void removeCondition() {
		if (conditionStack.size() > 0) {
			conditionStack.remove(conditionStack.size() - 1);
			updateSelection();
			if (conditionStack.size() == 0) {
				// building initial selected indices: All
				selectedIndices = new int[parentTable.getNumberOfRows()];
				for (int i = 0; i < selectedIndices.length; i++) {
					selectedIndices[i] = i;
				}
				numberOfSelectedRows = parentTable.getNumberOfRows();
			}
		}
		
	}
	
	private void updateSelection() {
		int parentRowIndex = 0;
		numberOfSelectedRows = 0;
		for (DataTableRow row: parentTable) {
			boolean keep = true;
			for (int conditionIndex = conditionStack.size() - 1; conditionIndex >= 0 && keep; conditionIndex--) {
				keep &= conditionStack.get(conditionIndex).keepRow(row);
			}
			if (keep) {
				selectedIndices[numberOfSelectedRows] = parentRowIndex;
				numberOfSelectedRows++;
			}
			parentRowIndex++;
		}
		// now informing listener of new selection
		informDataTableFilteredListener();
	}
	
	/*
	 * Listener Methods
	 */
	public void addDataTableFilteredListener(DataTableFilteredListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeDataTableFilteredListewner(DataTableFilteredListener listener) {
		this.listeners.remove(listener);
	}
	
	private void informDataTableFilteredListener() {
		for (DataTableFilteredListener listener: listeners) {
			listener.informDataTableChange(this);
		}
	}
	/*
	 *  Delegating methods
	 */

	@Override
	public void add(DataTableRow row) {
		parentTable.add(row);
	}

	@Override
	public int getColumnIndex(String name) {
		return parentTable.getColumnIndex(name);
	}

	@Override
	public String getColumnName(int i) {
		return parentTable.getColumnName(i);
	}

	@Override
	public double getColumnWeight(int i) {
		return parentTable.getColumnWeight(i);
	}

	@Override
	public int getNumberOfColumns() {
		return parentTable.getNumberOfColumns();
	}

	@Override
	public int getNumberOfSpecialColumns() {
		return parentTable.getNumberOfSpecialColumns();
	}

	@Override
	public int getNumberOfValues(int column) {
		return parentTable.getNumberOfValues(column);
	}

	@Override
	public boolean isDate(int index) {
		return parentTable.isDate(index);
	}

	@Override
	public boolean isDateTime(int index) {
		return parentTable.isDateTime(index);
	}

	@Override
	public boolean isNominal(int index) {
		return parentTable.isNominal(index);
	}

	@Override
	public boolean isNumerical(int index) {
		return parentTable.isNumerical(index);
	}

	@Override
	public boolean isSpecial(int column) {
		return parentTable.isSpecial(column);
	}

	@Override
	public boolean isSupportingColumnWeights() {
		return parentTable.isSupportingColumnWeights();
	}

	@Override
	public boolean isTime(int index) {
		return parentTable.isTime(index);
	}

	@Override
	public String mapIndex(int column, int index) {
		return parentTable.mapIndex(column, index);
	}

	@Override
	public int mapString(int column, String value) {
		return parentTable.mapString(column, value);
	}

	@Override
	public DataTable sample(int newSize) {
//		return parentTable.sample(newSize);
		return this;
	}

	@Override
	public void dataTableUpdated(DataTable source) {
		fireEvent();
	}
}
