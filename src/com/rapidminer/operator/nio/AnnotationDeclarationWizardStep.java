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
import java.awt.Component;
import java.util.Collections;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.rapidminer.RapidMiner;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.viewer.DataTableColumnEditTable;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.container.Pair;

/**
 * This Wizard Step might be used to select several rows as annotation rows having special meaning.
 * 
 * @author Sebastian Land
 */
public class AnnotationDeclarationWizardStep extends WizardStep {

	private JPanel panel = new JPanel(new BorderLayout());
	private DataResultSetTranslator translator = null;
	private DataResultSetTranslationConfiguration config;
	private ExampleSet exampleSet;
	private DataResultSet resultSet;
	private DataTableColumnEditTable table;
	private int maxRows;
	private DataResultSetFactory dataResultSetFactory;

	public AnnotationDeclarationWizardStep(DataResultSetFactory dataResultSetFactory ) {
		super("importwizard.annotations");
		this.dataResultSetFactory = dataResultSetFactory;

		try {
			maxRows = Integer.parseInt(RapidMiner.getRapidMinerPropertyValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS));
		} catch (NumberFormatException e) {
			maxRows = 100;
		}
		TableCellRenderer renderer = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override
			public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				if (value == null) {
					value = "";
				}
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, maxRows, column);
			};
		};
		Pair<TableCellRenderer, TableCellEditor> pair = new Pair<TableCellRenderer, TableCellEditor>(renderer, new AnnotationCellEditor());
		table = new DataTableColumnEditTable(null, 
				Collections.singletonList("Annotations"), 
				Collections.singletonList(pair), true, false, false);

		panel.add(new ExtendedJScrollPane(table), BorderLayout.CENTER);
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.FORWARD) {
			new ProgressThread("guessing_value_types") {
				@Override
				public void run() {
					getProgressListener().setTotal(100);
					getProgressListener().setCompleted(10);

					try {
						if (translator != null) {
							translator.close();
						}
						resultSet = dataResultSetFactory.makeDataResultSet(null);
						translator = new DataResultSetTranslator(resultSet);
						getProgressListener().setCompleted(30);
						
						config = new DataResultSetTranslationConfiguration(resultSet);
						getProgressListener().setCompleted(40);					
						config = translator.guessValueTypes(config, resultSet, maxRows, getProgressListener());
						getProgressListener().setCompleted(60);
						exampleSet = translator.read(config, true, maxRows, getProgressListener());
						getProgressListener().setCompleted(80);

						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								table.setDataTable(new DataTableExampleSetAdapter(exampleSet, null));
							}
						});
					} catch (OperatorException e) {					
						// TODO: Show error dialog
						e.printStackTrace();
					} finally {
						getProgressListener().complete();
					}
				}
			}.start();
		}
		return true;
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.BACKWARD || direction == WizardStepDirection.FINISH) {
			if (translator != null) {
				try {

					translator.close();
				} catch (OperatorException e) {
					// TODO: Show error dialog
					e.printStackTrace();
				}
			}
		} else if (direction == WizardStepDirection.FORWARD) {
			if (exampleSet == null || table == null)
				return false;

			// modify configuration according to done annotations
			TreeMap<Integer, String> annotationsMap = new TreeMap<Integer, String>();

			Object[] annotations = table.getEnteredValues(0);
			for (int i = 0; i < annotations.length; i++) {
				if (annotations[i] != null) {
					annotationsMap.put(i, annotations[i].toString());
				}
			}
			config.setAnnotationsMap(annotationsMap);
		}
		return true;
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}

	@Override
	protected boolean canProceed() {
		return true;
	}

	@Override
	protected JComponent getComponent() {
		return panel;
	}

	public DataResultSetTranslationConfiguration getConfiguration() {
		return config;
	}

	public ExampleSet getParsedExampleSet() {
		return exampleSet;
	}

}
