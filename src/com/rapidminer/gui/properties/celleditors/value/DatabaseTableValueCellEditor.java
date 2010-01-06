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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDatabaseTable;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.jdbc.ColumnIdentifier;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.ConnectionProvider;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;

/**
 * @author Tobias Malbrecht
 */
public class DatabaseTableValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {
	
	private static final long serialVersionUID = -771727412083431607L;
	
	class DatabaseTableComboBoxModel extends AbstractListModel implements ComboBoxModel, Serializable {
		private static final long serialVersionUID = -2984664300141879731L;
		
		private String lastURL = null;
		
		private LinkedList<Object> list = new LinkedList<Object>();
		
		private Object selected = null;

		public boolean updateModel() {
			final Object selected = getValue();
			if (connectionProvider != null) {
				final ConnectionEntry entry = connectionProvider.getConnectionEntry();
				if (entry != null && (lastURL == null || !lastURL.equals(entry.getURL()))) {
					lastURL = entry.getURL();
					Thread t = new Thread() { 
						@Override
						public void run() {
							list.clear();
							DatabaseHandler handler = null;
							try {
								handler = DatabaseConnectionService.connect(entry);
							} catch (SQLException e1) {
							}

							if (handler != null) {
								Map<String, List<ColumnIdentifier>> tableMap;
								try {
									tableMap = handler.getAllTableMetaData();
									list.addAll(tableMap.keySet());
								} catch (SQLException e) {
									// TODO: appropriate error message
									SwingTools.showVerySimpleErrorMessage("Retrieval of table names failed");
								}
								try {
									handler.disconnect();
								} catch (SQLException e) {
								}
							}
							if (model.getSize() == 0) {
								setSelectedItem(null);
							} else {
								if (selected != null) {
									setSelectedItem(selected);
								} else {
									if (model.getSize() > 0) {
										setSelectedItem(model.getElementAt(0));
									}
								}
							}
							fireContentsChanged(this, 0, list.size() - 1);
						}
					};
					t.start();
					return true;
				}
			}
			return false;
		}

		@Override
		public Object getSelectedItem() {
			return selected;
		}

		@Override
		public void setSelectedItem(Object object) {
			if ((selected != null && !selected.equals(object)) || selected == null && object != null) {
				selected = object;
				fireContentsChanged(this, -1, -1);
			}
		}

		@Override
		public Object getElementAt(int index) {
	        if (index >= 0 && index < list.size()) {
	        	return list.get(index);
	        }
	        return null;
		}

		@Override
		public int getSize() {
			return list.size();
		}
	}

	class DatabaseTableComboBox extends JComboBox {
		private static final long serialVersionUID = 7641636749562465262L;

		private DatabaseTableComboBox() {
			super(model);
			setEditable(true);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					fireEditingStopped();
				}			
			});
			addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent e) {
					fireEditingStopped();
				}

				@Override
				public void focusGained(FocusEvent e) {
					model.updateModel();
				}
			});
			addPopupMenuListener(new PopupMenuListener() {
				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					if (model.updateModel()) {
						hidePopup();
						showPopup();
					}
				}
			});
		}
	}

	private DatabaseTableComboBoxModel model = new DatabaseTableComboBoxModel();

	private JComboBox comboBox = new DatabaseTableComboBox();
	
	private Operator operator;

	private ParameterType type;
	
	private ConnectionProvider connectionProvider;
	
	public DatabaseTableValueCellEditor(final ParameterTypeDatabaseTable type) {
		this.type = type;
		comboBox.setToolTipText(type.getDescription());
	}
	
	private String getValue() {
		String value = null;
		try {
			value = operator.getParameterAsString(type.getKey());
		} catch (UndefinedParameterError e1) {
		}
		return value;
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		model.updateModel();
		comboBox.setSelectedItem(value);
		return comboBox;
	}

	@Override
	public Object getCellEditorValue() {
		return comboBox.getSelectedItem();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		model.updateModel();
		comboBox.setSelectedItem(value);
		return comboBox;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
		if (operator != null && operator instanceof ConnectionProvider) {
			this.connectionProvider = (ConnectionProvider) operator; 
		}
	}
}
