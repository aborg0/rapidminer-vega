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
package com.rapidminer.operator.preprocessing.filter;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;

/**
 * This operator simply sets the value for the specified example and
 * attribute to the given value.
 *  
 * @author Ingo Mierswa
 */
public class SetData extends AbstractDataProcessing {

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_EXAMPLE_INDEX = "example_index";

	public static final String PARAMETER_COUNT_BACKWARDS = "count_backwards";

	public static final String PARAMETER_VALUE = "value";


	public SetData(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(this, PARAMETER_ATTRIBUTE_NAME)));		
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		AttributeMetaData targetAttribute = metaData.getAttributeByName(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		if (targetAttribute != null) {
			if (targetAttribute.isNominal()) {
				targetAttribute.getValueSet().add(getParameterAsString(PARAMETER_VALUE));
			} else {
				try {
					targetAttribute.getValueRange().add(Double.parseDouble(getParameterAsString(PARAMETER_VALUE)));
				} catch (NumberFormatException e) {}
			}
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {		
		String value = getParameterAsString(PARAMETER_VALUE);
		int exampleIndex = getParameterAsInt(PARAMETER_EXAMPLE_INDEX);

		if (exampleIndex == 0) {
			throw new UserError(this, 207, new Object[] { "0", PARAMETER_EXAMPLE_INDEX, "only positive or negative indices are allowed"});
		}

		if (getParameterAsBoolean(PARAMETER_COUNT_BACKWARDS)) {
			exampleIndex = exampleSet.size() - exampleIndex;
		} else {
			exampleIndex--;
		}

		if (exampleIndex >= exampleSet.size()) {
			throw new UserError(this, 110, exampleIndex);
		}

		Attribute attribute = exampleSet.getAttributes().get(getParameter(PARAMETER_ATTRIBUTE_NAME));
		if (attribute == null) {
			throw new UserError(this, 111, getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		}

		Example example = exampleSet.getExample(exampleIndex);

		if (attribute.isNominal()) {
			example.setValue(attribute, attribute.getMapping().mapString(value));
		} else {
			try {
				double doubleValue = Double.parseDouble(value);
				example.setValue(attribute, doubleValue);
			} catch (NumberFormatException e) {
				throw new UserError(this, 211, PARAMETER_VALUE, value);
			}
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME, "The name of the attribute for which the value should be set.", getExampleSetInputPort(), false));
		ParameterType type = new ParameterTypeInt(PARAMETER_EXAMPLE_INDEX, "The index of the example for which the value should be set. Counting starts at 1.", 1, Integer.MAX_VALUE, false);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_COUNT_BACKWARDS, "If checked, the last counting order is inverted and hence the last example is addressed by index 1, the before last by index 2 and so on.", false, true));
		types.add(new ParameterTypeString(PARAMETER_VALUE, "The value which should be set.", false, false));
		return types;
	}
}
