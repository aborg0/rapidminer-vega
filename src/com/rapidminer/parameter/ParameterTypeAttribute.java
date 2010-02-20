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
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.tools.Ontology;

/**
 * This attribute type supports the user by let him select an attribute name from a combo box 
 * of known attribute names. For long lists, auto completition and filtering of the drop down menu
 * eases the handling.
 * For knowing attribute names before process execution a valid meta data transformation must be performed. Otherwise
 * the user might type in the name, instead of choosing.
 * @author Sebastian Land
 */
public class ParameterTypeAttribute extends ParameterTypeString {

	private static final long serialVersionUID = -4177652183651031337L;

	private InputPort inPort;

	private int[] allowedValueTypes;

	public ParameterTypeAttribute(final String key, String description, InputPort inPort) {
		this(key, description, inPort, false);
	}

	public ParameterTypeAttribute(final String key, String description, InputPort inPort, int...valueTypes) {
		this(key, description, inPort, false, valueTypes);
	}

	public ParameterTypeAttribute(final String key, String description, InputPort inPort, boolean optional) {
		this(key, description, inPort, optional, Ontology.ATTRIBUTE_VALUE);
	}

	public ParameterTypeAttribute(final String key, String description, InputPort inPort, boolean optional, boolean expert) {
		this(key, description, inPort, optional, Ontology.ATTRIBUTE_VALUE);
		setExpert(expert);
	}


	public ParameterTypeAttribute(final String key, String description, InputPort inPort, boolean optional, boolean expert, int...valueTypes) {
		this(key, description, inPort, optional, Ontology.ATTRIBUTE_VALUE);
		setExpert(expert);
		allowedValueTypes = valueTypes;
	}


	public ParameterTypeAttribute(final String key, String description, InputPort inPort, boolean optional, int...valueTypes) {
		super(key, description, optional);
		this.inPort = inPort;
		allowedValueTypes = valueTypes;
	}

	public Vector<String> getAttributeNames() {
		Vector<String> names = new Vector<String>();
		if (inPort != null) {
			if (inPort.getMetaData() instanceof ExampleSetMetaData) {
				ExampleSetMetaData emd = (ExampleSetMetaData) inPort.getMetaData();
				for (AttributeMetaData amd : emd.getAllAttributes()) {
					if (!isFilteredOut(amd) && isOfAllowedType(amd.getValueType()))
						names.add(amd.getName());
				}
			} else if (inPort.getMetaData() instanceof ModelMetaData) {
				ModelMetaData mmd = (ModelMetaData) inPort.getMetaData();
				if (mmd != null) {
					ExampleSetMetaData emd = mmd.getTrainingSetMetaData();
					if (emd != null) {
						for (AttributeMetaData amd : emd.getAllAttributes()) {
							if (!isFilteredOut(amd) && isOfAllowedType(amd.getValueType()))
								names.add(amd.getName());
						}
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

	@Override
	public Object getDefaultValue() {
		return "";
	}

	/** This method might be overridden by subclasses in order to 
	 *  select attributes which are applicable
	 */
	protected boolean isFilteredOut(AttributeMetaData amd) {
		return false;
	};
	
	public InputPort getInputPort() {
		return inPort;
	}
}

