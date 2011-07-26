/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
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
package com.rapidminer.gui.tools.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SQLEditor;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.jdbc.ColumnIdentifier;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.TableName;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;


/**
 * 
 * @author Tobias Malbrecht
 */
public class SQLQueryBuilder extends ButtonDialog {

    private static final long serialVersionUID = 1779762368364719191L;

    /** The list with all tables. */
    private final JList tableList = new JList();

    /** The list with all attribute names. */
    private final JList attributeList = new JList();

    /** The text area with the where clause. */
    private final JTextArea whereTextArea = new JTextArea(4, 15);

    /** The text area with the sql query */
    private final SQLEditor sqlQueryTextArea = new SQLEditor();

    /** All attribute names for the available tables. */
    private final Map<TableName, List<ColumnIdentifier>> attributeNameMap = new LinkedHashMap<TableName, List<ColumnIdentifier>>();

    private DatabaseHandler databaseHandler;

    public SQLQueryBuilder(DatabaseHandler databaseHandler) {
        super("build_sql_query", true);
        this.databaseHandler = databaseHandler;
    }

    public void setConnectionEntry(ConnectionEntry entry) {
        try {
        	if (entry == null) {
        		this.databaseHandler = null;
        	} else {
        		this.databaseHandler = DatabaseHandler.getConnectedDatabaseHandler(entry);
        		if (!"false".equals(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_FETCH_DATA_BASE_TABLES_NAMES))){
        			retrieveTableAndAttributeNames();
        		}
        	}
        } catch (SQLException e) {
            SwingTools.showSimpleErrorMessage("db_connection_failed_url", e, entry.getURL());
            this.databaseHandler = null;
        }
    }

    public JPanel makeQueryBuilderPanel() {
        return makeQueryBuilderPanel(false);
    }

    public JPanel makeQueryBuilderPanel(boolean editOnly) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;

        if (!editOnly) {
            JPanel gridPanel = new JPanel(createGridLayout(1, 3));
            // table and attribute lists, where clause text area
            tableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            tableList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    updateAttributeNames();
                    updateSQLQuery();
                }
            });
            JScrollPane tablePane = new ExtendedJScrollPane(tableList);
            tablePane.setBorder(createTitledBorder("Tables"));
            gridPanel.add(tablePane);

            attributeList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    updateSQLQuery();
                }
            });
            JScrollPane attributePane = new ExtendedJScrollPane(attributeList);
            attributePane.setBorder(createTitledBorder("Attributes"));
            gridPanel.add(attributePane);

            whereTextArea.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {}
                @Override
                public void keyPressed(KeyEvent e) {}
                @Override
                public void keyReleased(KeyEvent e) {
                    updateSQLQuery();
                }
            });
            JScrollPane whereTextPane = new ExtendedJScrollPane(whereTextArea);
            whereTextPane.setBorder(createTitledBorder("Where Clause"));
            gridPanel.add(whereTextPane);

            c.weighty = 0.5;
            c.gridwidth = GridBagConstraints.REMAINDER;
            panel.add(gridPanel, c);
        }

        // SQL statement field
        c.weighty = 1.0d;
        sqlQueryTextArea.setBorder(createTitledBorder("SQL Query"));
        panel.add(new ExtendedJScrollPane(sqlQueryTextArea), c);

        return panel;
    }

    private void updateAttributeNames() {
        List<ColumnIdentifier> allColumnIdentifiers = new LinkedList<ColumnIdentifier>();
        Object[] selection = tableList.getSelectedValues();
        for (Object o : selection) {
        	TableName tableName = (TableName)o;
            List<ColumnIdentifier> attributeNames = this.attributeNameMap.get(tableName);
            if (attributeNames != null) {
                Iterator<ColumnIdentifier> i = attributeNames.iterator();
                while (i.hasNext()) {
                    ColumnIdentifier currentIdentifier = i.next();
                    allColumnIdentifiers.add(currentIdentifier);
                }
            }
        }
        attributeList.removeAll();
        ColumnIdentifier[] identifierArray = new ColumnIdentifier[allColumnIdentifiers.size()];
        allColumnIdentifiers.toArray(identifierArray);
        attributeList.setListData(identifierArray);
    }

    private void appendAttributeName(StringBuffer result, ColumnIdentifier identifier, boolean first, boolean singleTable) {
        if (!first) {
            result.append(", ");
        }
        if (singleTable) {
            result.append(identifier.getFullName(singleTable));
        } else {
            result.append(identifier.getFullName(singleTable) + " AS " + identifier.getAliasName(singleTable));
        }
    }

    private void updateSQLQuery() {
        fireStateChanged();

        Object[] tableSelection = tableList.getSelectedValues();
        if (tableSelection.length == 0) {
            sqlQueryTextArea.setText("");
            return;
        }

        boolean singleTable = tableSelection.length == 1;

        // SELECT
        StringBuffer result = new StringBuffer("SELECT ");
        Object[] attributeSelection = attributeList.getSelectedValues();
        if (singleTable && (attributeSelection.length == 0 || attributeSelection.length == attributeList.getModel().getSize())) {
            result.append("*");
        } else {
            if (attributeSelection.length == 0 || attributeSelection.length == attributeList.getModel().getSize()) {
                boolean first = true;
                for (int i = 0; i < attributeList.getModel().getSize(); i++) {
                    ColumnIdentifier identifier = (ColumnIdentifier) attributeList.getModel().getElementAt(i);
                    appendAttributeName(result, identifier, first, singleTable);
                    first = false;
                }
            } else {
                boolean first = true;
                for (Object o : attributeSelection) {
                    ColumnIdentifier identifier = (ColumnIdentifier)o;
                    appendAttributeName(result, identifier, first, singleTable);
                    first = false;
                }
            }
        }

        // FROM
        result.append("\nFROM ");
        boolean first = true;
        for (Object o : tableSelection) {
            if (first) {
                first = false;
            } else {
                result.append(", ");
            }
            TableName tableName = (TableName)o;
            result.append(databaseHandler.getStatementCreator().makeIdentifier(tableName));
        }

        // WHERE
        String whereText = whereTextArea.getText().trim();
        if (whereText.length() > 0) {
            result.append("\nWHERE " + whereText);
        }
        sqlQueryTextArea.setText(result.toString());
    }

    private void retrieveTableAndAttributeNames() throws SQLException {
        if (databaseHandler != null) {
            ProgressThread retrieveTablesThread = new ProgressThread("fetching_database_tables") {
                @Override
                public void run() {
                    getProgressListener().setTotal(100);
                    getProgressListener().setCompleted(10);
                    try {
                        // retrieve data
                        attributeNameMap.clear();
                        if (databaseHandler != null) {
                            Map<TableName, List<ColumnIdentifier>> newAttributeMap;
                            try {
                                newAttributeMap = databaseHandler.getAllTableMetaData(getProgressListener(), 10, 100, true);
                                attributeNameMap.putAll(newAttributeMap);
                            } catch (SQLException e) {
                                SwingTools.showSimpleErrorMessage("db_connection_failed_simple", e, e.getMessage());
                            }
                        }

                        // set table name list data
                        final TableName[] allNames = new TableName[attributeNameMap.size()];
                        attributeNameMap.keySet().toArray(allNames);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                tableList.removeAll();
                                tableList.setListData(allNames);
                            }
                        });
                    } finally {
                        getProgressListener().complete();
                        // disconnect
                        //	                    try {
                        //							databaseHandler.disconnect();
                        //						} catch (SQLException e) {
                        //							SwingTools.showSimpleErrorMessage("db_connection_failed_simple", e, e.getMessage());
                        //						}
                    }
                }
            };
            retrieveTablesThread.start();
        }
    }

    public void setQuery(String query) {
        sqlQueryTextArea.setText(query);
    }

    public String getQuery() {
        return sqlQueryTextArea.getText();
    }
}
