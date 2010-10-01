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

import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.viewer.DataTableColumnEditTable;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.DataResultSetTranslationConfiguration;
import com.rapidminer.operator.nio.model.DataResultSetTranslator;
import com.rapidminer.operator.nio.model.WizardState;
import com.rapidminer.tools.container.Pair;

/**
 * This Wizard Step might be used to select several rows as annotation rows having special meaning.
 * 
 * @author Sebastian Land
 */
public class AnnotationDeclarationWizardStep extends WizardStep {

	private JPanel panel = new JPanel(new BorderLayout());
	
	private final WizardState state;
	
	private DataTableColumnEditTable table;

	public AnnotationDeclarationWizardStep(WizardState state) {
		super("importwizard.annotations");
		this.state = state;

		TableCellRenderer renderer = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override
			public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				if (value == null) {
					value = "";
				}
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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
					// TODO: We don't want an example set here. No example set needed for annotations
					//       We already have a tabnle model: The ExcelTableModel which can be used here.
					// TODO: This code is duplicated in MetaDataDeclarationWizardStep where it belongs
					getProgressListener().setTotal(100);
					getProgressListener().setCompleted(10);

					try {
						if (state.getTranslator() != null) {
							state.getTranslator().close();
						}
						DataResultSet resultSet = state.getDataResultSetFactory().makeDataResultSet(null);
						state.setTranslator(new DataResultSetTranslator(state.getOperator(), resultSet));
						getProgressListener().setCompleted(30);
						
						state.setTranslationConfiguration(new DataResultSetTranslationConfiguration(resultSet));
						getProgressListener().setCompleted(40);					
						state.getTranslator().guessValueTypes(state.getTranslationConfiguration(), resultSet, state.getNumberOfPreviewRows(), getProgressListener());
						getProgressListener().setCompleted(60);
						final ExampleSet exampleSet = state.readNow(true, getProgressListener());
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
			if (state.getTranslator() != null) {
				try {
					state.getTranslator().close();
				} catch (OperatorException e) {
					// TODO: Show error dialog
					e.printStackTrace();
				}
			}
		} else if (direction == WizardStepDirection.FORWARD) {
//			if (exampleSet == null || table == null)
//				return false;

			// modify configuration according to done annotations
			TreeMap<Integer, String> annotationsMap = new TreeMap<Integer, String>();

			Object[] annotations = table.getEnteredValues(0);
			for (int i = 0; i < annotations.length; i++) {
				if (annotations[i] != null) {
					annotationsMap.put(i, annotations[i].toString());
				}
			}
			state.getTranslationConfiguration().setAnnotationsMap(annotationsMap);
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
}
