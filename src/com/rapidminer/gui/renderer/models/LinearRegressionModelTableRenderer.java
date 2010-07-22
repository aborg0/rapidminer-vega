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
package com.rapidminer.gui.renderer.models;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.functions.LinearRegressionModel;

/**
 * Renderer for the linear regression model.
 * 
 * @author Simon Fischer
 */
public class LinearRegressionModelTableRenderer extends AbstractTableModelTableRenderer {

	private static class LinearRegressionModelTableModel extends AbstractTableModel {

		private static final long serialVersionUID = -2112928170124291591L;

		private final LinearRegressionModel model;
		
		public LinearRegressionModelTableModel(LinearRegressionModel model) {
			this.model = model;
		}

		@Override
		public int getColumnCount() {
			return 6;
		}
		
		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return "Attribute";
			case 1:
				return "Coefficient";
			case 2:
				return "Std. Error";
			case 3:
				return "Std. Coefficient";
			case 4:
				return "t-Stat";
			case 5:
				return "Significance";
			}
			return null;
		}

		@Override
		public int getRowCount() {
			return model.getCoefficients().length - (model.usesIntercept() ? 0 : 1);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (model.usesIntercept() && rowIndex == model.getCoefficients().length - 1) {
					return "(Intercept)";
				} else {
					return model.getSelectedAttributeNames()[rowIndex];
				}
			case 1:
				return model.getCoefficients()[rowIndex];
			case 2:
				if (model.usesIntercept() && rowIndex == model.getCoefficients().length - 1) {
					return "";
				} else {
					return model.getStandardErrors()[rowIndex];
				}
			case 3:
				if (model.usesIntercept() && rowIndex == model.getCoefficients().length - 1) {
					return "";
				} else {
					return model.getStandardizedCoefficients()[rowIndex];
				}
			case 4:
				if (model.usesIntercept() && rowIndex == model.getCoefficients().length - 1) {
					return "";
				} else {
					return model.getTStats()[rowIndex];
				}
			case 5:
				if (model.usesIntercept() && rowIndex == model.getCoefficients().length - 1) {
					return "";
				} else {
					return model.getProbabilities()[rowIndex];
				}
			}
			return null;
		}
	}

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer, boolean isReporting) {
		return new LinearRegressionModelTableModel((LinearRegressionModel) renderable);
	}
}
