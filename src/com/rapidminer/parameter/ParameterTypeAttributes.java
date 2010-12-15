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
package com.rapidminer.parameter;

import java.util.Vector;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.Ontology;


/**
 * A parameter type for regular expressions.
 * 
 * @author Tobias Malbrecht
 */
public class ParameterTypeAttributes extends ParameterTypeString {

	private static final long serialVersionUID = -4177652183651031337L;

	private transient InputPort inPort;

	private int[] allowedValueTypes;

	public ParameterTypeAttributes(final String key, String description, InputPort inPort) {
		this(key, description, inPort, true, Ontology.ATTRIBUTE_VALUE);
	}

	public ParameterTypeAttributes(final String key, String description, InputPort inPort, int...valueTypes) {
		this(key, description, inPort, true, valueTypes);
	}

	public ParameterTypeAttributes(final String key, String description, InputPort inPort, boolean optional) {
		this(key, description, inPort, optional, Ontology.ATTRIBUTE_VALUE);
	}

	public ParameterTypeAttributes(final String key, String description, InputPort inPort, boolean optional, int...valueTypes) {
		super(key, description, optional);
		this.inPort = inPort;
		this.allowedValueTypes = valueTypes;
	}

	public ParameterTypeAttributes(final String key, String description, InputPort inPort, boolean optional, boolean expert) {
		this(key, description, inPort, optional);
		setExpert(expert);

	}

	public Vector<String> getAttributeNames() {
		Vector<String> names = new Vector<String>();
		if (inPort != null) {
			if (inPort.getMetaData() instanceof ExampleSetMetaData) {
				ExampleSetMetaData emd = (ExampleSetMetaData) inPort.getMetaData();
				for (AttributeMetaData amd : emd.getAllAttributes()) {
					if (isOfAllowedType(amd.getValueType())) {
						names.add(amd.getName());
					}
				}
			}
		}
		return names;
	}

	private boolean isOfAllowedType(int valueType) {
		boolean isAllowed = false;
		for (int type: allowedValueTypes) {
			isAllowed |= Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, type);
		}
		return isAllowed;
	}
}

