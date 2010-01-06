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
package com.rapidminer.gui.properties;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Properties;

import com.rapidminer.RapidMiner;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;


/**
 * The property table for general RapidMiner settings (in contrast to operator
 * parameters). The settings can be applied only for the current session or
 * saved for future sessions.
 * 
 * @author Ingo Mierswa
 *          ingomierswa Exp $
 */
public class SettingsPropertyTable extends PropertyTable {

	private static final long serialVersionUID = 649188519589057381L;

	private transient ParameterType[] parameterTypes;
     
	public SettingsPropertyTable() {
		this(RapidMiner.getRapidMinerProperties());
	}

	public SettingsPropertyTable(Collection<ParameterType> allProperties) {
		super(new String[] { "Property", "Value" });

		parameterTypes = new ParameterType[allProperties.size()];
		allProperties.toArray(parameterTypes);

		updateTableData(parameterTypes.length);
		for (int i = 0; i < parameterTypes.length; i++) {
			ParameterType type = parameterTypes[i];
			String key = parameterTypes[i].getKey();
			getModel().setValueAt(key, i, 0);
            // important: here System.getProperty() to use the settings applied in old sessions!
            Object property = System.getProperty(key);
			if (property == null)
				property = type.getDefaultValue();
			getModel().setValueAt(property, i, 1);
		}
		updateEditorsAndRenderers(this);
	}
    
	@Override
	protected Object readResolve() {
		return this;
	}
	
	@Override
	public ParameterType getParameterType(int row) {
		return parameterTypes[row];
	}

	@Override
	public Operator getOperator(int row) {
		return null;
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return (col == 1);
	}
	
	/** Returns the value of the property with the given index as string. */
	private String getPropertyValue(int index) {
		Object value = getModel().getValueAt(index, 1);
		if (value == null)
			return null;
		return value.toString();
	}

	/** Applies the properties without saving them. */ 
	public void applyProperties() {
		applyProperties(System.getProperties());
	}
	
	public void applyProperties(Properties properties) {
		for (int i = 0; i < parameterTypes.length; i++) {
			String value = getPropertyValue(i);
			if (value != null) {				
				properties.setProperty(parameterTypes[i].getKey(), value);
			} else {
				properties.remove(parameterTypes[i].getKey());
			}
		}
	}

	/**
	 * Applies and write the properties in the system dependent config file in
	 * the user directory.
	 */
	public void writeProperties(PrintWriter out) throws IOException {		
		for (int i = 0; i < parameterTypes.length; i++) {
			String key = parameterTypes[i].getKey();
			String value = getPropertyValue(i);
			if (value != null) {
				System.setProperty(key, value);
				out.println(key + " = " + value);
			}
		}
	}	
}
