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
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.actions.AddToSortingColumnsAction;
import com.rapidminer.gui.tools.actions.EqualColumnWidthsAction;
import com.rapidminer.gui.tools.actions.FitAllColumnWidthsAction;
import com.rapidminer.gui.tools.actions.FitColumnWidthAction;
import com.rapidminer.gui.tools.actions.RestoreOriginalColumnOrderAction;
import com.rapidminer.gui.tools.actions.SelectColumnAction;
import com.rapidminer.gui.tools.actions.SelectRowAction;
import com.rapidminer.gui.tools.actions.SortByColumnAction;
import com.rapidminer.gui.tools.actions.SortColumnsAccordingToNameAction;
import com.rapidminer.report.Tableable;
import com.rapidminer.tools.Tools;

/**
 * <p>This class extends a JTable in a way that editing is handled like it is expected, i.e. 
 * editing is properly stopped during focus losts, resizing, or column movement. The current
 * value is then set to the model. The only way to abort the value change is by pressing
 * the escape key.</p>
 * 
 * <p>The extended table is sortable per default. Developers should note that this feature 
 * might lead to problems if the columns contain different class types end different editors.
 * In this case one of the constructors should be used which set the sortable flag to false.
 * </p>
 *   
 * @author Ingo Mierswa
 */
public class ExtendedJTable extends JTable implements Tableable, MouseListener {

    private static final long serialVersionUID = 4840252601155251257L;

    private static final int DEFAULT_MAX_ROWS_FOR_SORTING = 100000;
    
    private static final int DEFAULT_COLUMN_WIDTH = 100;
    
    public static final int NO_DATE_FORMAT = -1;
    public static final int DATE_FORMAT = 0;
    public static final int TIME_FORMAT = 1;
    public static final int DATE_TIME_FORMAT = 2;
    
    
    private Action ROW_ACTION = new SelectRowAction(this, IconSize.SMALL);
    private Action COLUMN_ACTION = new SelectColumnAction(this, IconSize.SMALL);
    private Action FIT_COLUMN_ACTION = new FitColumnWidthAction(this, IconSize.SMALL);
    private Action FIT_ALL_COLUMNS_ACTION = new FitAllColumnWidthsAction(this, IconSize.SMALL);
    private Action EQUAL_WIDTHS_ACTION = new EqualColumnWidthsAction(this, IconSize.SMALL);
    private Action SORTING_DESCENDING_ACTION = new SortByColumnAction(this, ExtendedJTableSorterModel.DESCENDING, IconSize.SMALL);
    private Action SORTING_ASCENDING_ACTION = new SortByColumnAction(this, ExtendedJTableSorterModel.ASCENDING, IconSize.SMALL);
    private Action ADD_TO_SORTING_DESCENDING_ACTION = new AddToSortingColumnsAction(this, ExtendedJTableSorterModel.DESCENDING, IconSize.SMALL);
    private Action ADD_TO_SORTING_ASCENDING_ACTION = new AddToSortingColumnsAction(this, ExtendedJTableSorterModel.ASCENDING, IconSize.SMALL);
    private Action SORT_COLUMNS_BY_NAME_ACTION = new SortColumnsAccordingToNameAction(this, IconSize.SMALL);
    private Action RESTORE_COLUMN_ORDER_ACTION = new RestoreOriginalColumnOrderAction(this, IconSize.SMALL);
    
    
    private boolean sortable = true;
    
    private CellColorProvider cellColorProvider = new CellColorProviderAlternating();
    
    private boolean useColoredCellRenderer = true;
        
    private transient ColoredTableCellRenderer renderer = new ColoredTableCellRenderer();
    
    private ExtendedJTableSorterModel tableSorter = null;
    
    private ExtendedJScrollPane scrollPaneParent = null;
    
    private ExtendedJTablePacker packer = null;
    
    private boolean fixFirstColumn = false;
    
    private String[] originalOrder = null;
    
    private boolean showPopopUpMenu = true;
    
    
    public ExtendedJTable() {
        this(null, true);
    }

    public ExtendedJTable(boolean sortable) {
        this(null, sortable);
    }
    
    public ExtendedJTable(TableModel model, boolean sortable) {
        this(model, sortable, true);
    }

    public ExtendedJTable(TableModel model, boolean sortable, boolean columnMovable) {
    	this(model, sortable, columnMovable, true);
    }

    public ExtendedJTable(boolean sortable, boolean columnMovable, boolean autoResize) {
    	this(null, sortable, columnMovable, autoResize);
    }
    
    public ExtendedJTable(TableModel model, boolean sortable, boolean columnMovable, boolean autoResize) {
    	this(model, sortable, columnMovable, autoResize, true, false);
    }
    
    public ExtendedJTable(TableModel model, boolean sortable, boolean columnMovable, boolean autoResize, boolean useColoredCellRenderer, boolean fixFirstColumn) {
        super();
        this.sortable = sortable;
        this.useColoredCellRenderer = useColoredCellRenderer;
        this.fixFirstColumn = fixFirstColumn;
        
        // allow all kinds of selection (e.g. for copy and paste)
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setColumnSelectionAllowed(true);
        setRowSelectionAllowed(true);
        
        setRowHeight(getRowHeight() + SwingTools.TABLE_ROW_EXTRA_HEIGHT);
        getTableHeader().setReorderingAllowed(columnMovable);
        
        // necessary in order to fix changes after focus was lost
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        
        // auto resize?
        if (!autoResize)
        	setAutoResizeMode(AUTO_RESIZE_OFF);
        
        if (model != null) {
            setModel(model);
        }
        
        // add listener for automatically resizing the table for double clicking the header border
        getTableHeader().addMouseListener(new ExtendedJTableColumnFitMouseListener()); 
        
        addMouseListener(this);
    }
    
    protected Object readResolve() {
    	this.renderer = new ColoredTableCellRenderer();
    	return this;
    }
    
    protected ExtendedJTableSorterModel getTableSorter() {
    	return this.tableSorter;
    }

    /** Subclasses might overwrite this method which by default simply returns NO_DATE. 
     *  The returned format should be one out of NO_DATE_FORMAT, DATE_FORMAT, TIME_FORMAT, 
     *  or DATE_TIME_FORMAT. This information will be used for the cell renderer. */
    public int getDateFormat(int row, int column) {
    	return NO_DATE_FORMAT;
    }

    /** The given color provider will be used for the cell renderer. 
     *  The default method implementation returns {@link SwingTools#LIGHTEST_BLUE} and white for 
     *  alternating rows. If no colors should be used at all, set the cell color provider to
     *  null or to the default white color provider {@link CellColorProviderWhite}. */
    public void setCellColorProvider(CellColorProvider cellColorProvider) {
    	this.cellColorProvider = cellColorProvider;
    }
    
    /** The returned color provider will be used for the cell renderer. 
     *  The default method implementation returns {@link SwingTools#LIGHTEST_BLUE} and white for 
     *  alternating rows. If no colors should be used at all, set the cell color provider to
     *  null or to the default white color provider {@link CellColorProviderWhite}. */
    public CellColorProvider getCellColorProvider() {
    	return this.cellColorProvider;
    }
    
    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }
    
    public boolean isSortable() {
        return sortable;
    }
    
    public void setShowPopupMenu(boolean showPopupMenu) {
    	this.showPopopUpMenu = showPopupMenu;
    }
    
    public void setFixFirstColumnForRearranging(boolean fixFirstColumn) {
    	this.fixFirstColumn = fixFirstColumn;
    }
    
    @Override
	public void setModel(TableModel model) {
    	boolean shouldSort = this.sortable && checkIfSortable(model);
    	
        if (shouldSort) {
            this.tableSorter = new ExtendedJTableSorterModel(model);
            this.tableSorter.setTableHeader(getTableHeader());
            super.setModel(this.tableSorter);
        } else {
            super.setModel(model);
            this.tableSorter = null;
        }
        
        originalOrder = new String[model.getColumnCount()];
        for (int c = 0; c < model.getColumnCount(); c++) {
        	originalOrder[c] = model.getColumnName(c);
        }
    }
    
    public void setSortingStatus(int status, boolean cancelSorting) {
    	if (getModel() instanceof ExtendedJTableSorterModel) {
    		ExtendedJTableSorterModel sorterModel = (ExtendedJTableSorterModel)getModel();
    		
    		JTableHeader h = getTableHeader();
    		TableColumnModel columnModel = h.getColumnModel();
    		int viewColumn = getSelectedColumn();
    		if (viewColumn != -1) {
    			int column = columnModel.getColumn(viewColumn).getModelIndex();
    			if (column != -1) {
    	    		if (sorterModel.isSorting()) {
    	    			if (cancelSorting) {
    	    				sorterModel.cancelSorting();
    	    			}
    	    		}    				
    	    		sorterModel.setSortingStatus(column, status);    
    			}
    		}		
    	}
    }
    
    public void pack() {
    	packer = new ExtendedJTablePacker(true);
    	if (isShowing()) {
    		packer.pack(this);
    		packer = null;
    	}
    }

    @Override
	public void addNotify(){
    	super.addNotify();
    	if (packer != null){
    		packer.pack(this);
    		packer = null;
    	}
    } 
    
    public void unpack() {
    	JTableHeader header = getTableHeader();
    	if (header != null) {
    		for (int c = 0; c < getColumnCount(); c++) {
    			TableColumn tableColumn = header.getColumnModel().getColumn(c);
    			header.setResizingColumn(tableColumn); // this line is very important 

    			int width = DEFAULT_COLUMN_WIDTH;
    			if (getWidth() / width > getColumnCount()) {
    				width = getWidth() / getColumnCount();
    			}
    			tableColumn.setWidth(width);
    		}
    	}
    }
    
    public void packColumn() {
		JTableHeader header = getTableHeader();
		if (header != null) {
			int col = getSelectedColumn();
			if (col >= 0) {
				TableColumn tableColumn = header.getColumnModel().getColumn(col);

				if (tableColumn != null) {
					int width = (int)header.getDefaultRenderer().getTableCellRendererComponent(this, tableColumn.getIdentifier(), false, false, -1, col).getPreferredSize().getWidth();

					int firstRow = 0;
					int lastRow = getRowCount();

					ExtendedJScrollPane scrollPane = getExtendedScrollPane();
					if (scrollPane != null) {
						JViewport viewport = scrollPane.getViewport();
						Rectangle viewRect = viewport.getViewRect();
						if (viewport.getHeight() < getHeight()) {
							firstRow = rowAtPoint(new Point(0, viewRect.y));
							firstRow = Math.max(0, firstRow);
							lastRow = rowAtPoint(new Point(0, viewRect.y + viewRect.height - 1));
							lastRow = Math.min(lastRow, getRowCount());		
						}
					}


					for (int row = firstRow; row < lastRow; row++){ 
						int preferedWidth = (int)getCellRenderer(row, col).getTableCellRendererComponent(this, getValueAt(row, col), false, false, row, col).getPreferredSize().getWidth(); 
						width = Math.max(width, preferedWidth); 
					} 

					header.setResizingColumn(tableColumn); // this line is very important 

					tableColumn.setWidth(width + getIntercellSpacing().width);
				}
			}
		}
    }
    
    public void sortColumnsAccordingToNames() {
    	int offset = 0;
    	if (fixFirstColumn) {
    		offset = 1;
    	}
    	for (int i = offset; i < getColumnCount(); i++) {
    		int minIndex = -1;
    		String minName = null;
    		for (int j = i; j < getColumnCount(); j++) {
    			String currentName = getColumnName(j);
    			if (minName == null || currentName.compareTo(minName) < 0) {
    				minName = currentName;
    				minIndex = j;
    			}
    		}
    		moveColumn(minIndex, i);	
    	}
    }
    
    public void restoreOriginalColumnOrder() {
    	for (int i = 0; i < originalOrder.length; i++) {
    		String nextColumn = originalOrder[i];
    		for (int j = i; j < getColumnCount(); j++) {
    			String candidateName = getColumnName(j);
    			if (nextColumn.equals(candidateName)) {
    				moveColumn(j, i);
    				break;
    			}
    		}
    	}
    }
    
    @Override
	public Dimension getIntercellSpacing() {
    	Dimension dimension = super.getIntercellSpacing();
    	dimension.width = dimension.width + 6;
    	return dimension;
    }
    
    private boolean checkIfSortable(TableModel model) {
    	int maxSortableRows = DEFAULT_MAX_ROWS_FOR_SORTING;
    	String maxString = System.getProperty(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_MAX_SORTABLE_ROWS);
    	if (maxString != null) {
    		try {
    			maxSortableRows = Integer.parseInt(maxString);
    		} catch (NumberFormatException e) {
    			// do nothing
    		}
    	}
    	
    	if (model.getRowCount() > maxSortableRows) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    /** Necessary to properly stopping the editing when a column is moved (dragged). */
    @Override
	public void columnMoved(TableColumnModelEvent e) {
        if (isEditing()) {
            cellEditor.stopCellEditing();
        }
        super.columnMoved(e);
    }

    /** Necessary to properly stopping the editing when a column is resized. */
    @Override
	public void columnMarginChanged(ChangeEvent e) {
        if (isEditing()) {
            cellEditor.stopCellEditing();
        }
        super.columnMarginChanged(e);
    }
    
    public boolean shouldUseColoredCellRenderer() {
    	return this.useColoredCellRenderer;
    }
    
    @Override
	public TableCellRenderer getCellRenderer(int row, int col) {
    	if (useColoredCellRenderer) {
    		Color color = null;
    		CellColorProvider usedColorProvider = getCellColorProvider();
    		if (usedColorProvider != null) {
    			color = usedColorProvider.getCellColor(row, col);	
    		}
    		
    		if (color != null)
    			renderer.setColor(color);
    		
    		renderer.setDateFormat(getDateFormat(row, col));
    		return renderer;
    	} else {
    		return super.getCellRenderer(row, col);
    	}
    }
    
    /** This method ensures that the correct tool tip for the current table cell is delivered. */
    @Override
	public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int colIndex = columnAtPoint(p);
        int realColumnIndex = convertColumnIndexToModel(colIndex);
        int rowIndex = rowAtPoint(p);
        
		String text = null;
		if (rowIndex >= 0 && rowIndex < getRowCount() && realColumnIndex >= 0 && realColumnIndex < getModel().getColumnCount()) {
	        Object value = getModel().getValueAt(rowIndex, realColumnIndex);
			if (value instanceof Number) {
				Number number = (Number)value;
				double numberValue = number.doubleValue();
				text = Tools.formatIntegerIfPossible(numberValue);
			} else {
				if (value != null) {
					if (value instanceof Date) {
						int dateFormat = getDateFormat(rowIndex, realColumnIndex);
						switch (dateFormat) {
						case ExtendedJTable.DATE_FORMAT: text = Tools.formatDate((Date)value); break;
						case ExtendedJTable.TIME_FORMAT: text = Tools.formatTime((Date)value); break;
						case ExtendedJTable.DATE_TIME_FORMAT: text = Tools.formatDateTime((Date)value); break;
						default: text = value.toString(); break;
						}
					} else {
						text = value.toString();
					}
				} else {
					text = "?";
				}
			}
		}		
        if (text != null && !text.equals(""))
            return SwingTools.transformToolTipText(text);
        else
            return super.getToolTipText();
    }

	public String getCell(int row, int column) {
		String text = null;
		if (getTableHeader() != null) {
			if (row == 0) {
				// titel row
				return getTableHeader().getColumnModel().getColumn(column).getHeaderValue().toString();
			} else {
				row--;
			}
		}
		// data area
		Object value = getModel().getValueAt(row, column);
		if (value instanceof Number) {
			Number number = (Number)value;
			double numberValue = number.doubleValue();
			text = Tools.formatIntegerIfPossible(numberValue);
		} else {
			if (value != null)
				text = value.toString();
			else
				text = "?";
		}
		return text;
	}

	public int getColumnNumber() {
		return getColumnCount();
	}

	public int getRowNumber() {
		if (getTableHeader() != null) {
			return getRowCount() + 1;
		} else {
			return getRowCount();
		}
	}
	
	public void prepareReporting() {}
	public void finishReporting() {}
	
	public boolean isFirstLineHeader() { return false; }
	
	public boolean isFirstColumnHeader() { return false; }
	
	public int getModelIndex(int rowIndex) {
		return tableSorter.modelIndex(rowIndex);
	}
	
	public void setExtendedScrollPane(ExtendedJScrollPane scrollPane) {
		this.scrollPaneParent = scrollPane;
	}
	
	public ExtendedJScrollPane getExtendedScrollPane() {
		return this.scrollPaneParent;
	}

	public void selectCompleteRow() {
		addColumnSelectionInterval(0, getColumnCount() - 1);
	}

	public void selectCompleteColumn() {
		addRowSelectionInterval(0, getRowCount() - 1);
	}

	
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	
	public void mouseReleased(MouseEvent e) {	
		if (showPopopUpMenu) {
			if (e.isPopupTrigger()) {
				Point p = e.getPoint();
				int row = rowAtPoint(p);
				int c = columnAtPoint(p);
				int column = convertColumnIndexToModel(c);

				setRowSelectionInterval(row, row);
				setColumnSelectionInterval(column, column);

				JPopupMenu menu = createPopupMenu();

				menu.show(this, e.getX(), e.getY());
			}
		}
	}

	public JPopupMenu createPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		populatePopupMenu(menu);
		return menu;
	}

	public void populatePopupMenu(JPopupMenu menu) {
		menu.add(ROW_ACTION);
		menu.add(COLUMN_ACTION);
		
		if (getTableHeader() != null) {
			menu.addSeparator();
			menu.add(FIT_COLUMN_ACTION);
			menu.add(FIT_ALL_COLUMNS_ACTION);
			menu.add(EQUAL_WIDTHS_ACTION);
		}

		if (isSortable()) {
			menu.addSeparator();
			menu.add(SORTING_ASCENDING_ACTION);
			menu.add(SORTING_DESCENDING_ACTION);
			menu.addSeparator();
			menu.add(ADD_TO_SORTING_ASCENDING_ACTION);
			menu.add(ADD_TO_SORTING_DESCENDING_ACTION);
		}

		if (getTableHeader() != null) {
			if (getTableHeader().getReorderingAllowed()) {
				menu.addSeparator();
				menu.add(SORT_COLUMNS_BY_NAME_ACTION);
				menu.add(RESTORE_COLUMN_ORDER_ACTION);
			}
		}		
	}
}
