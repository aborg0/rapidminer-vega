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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.Ontology;

/**
 * Replaces missing values in examples. If a value is missing, it is replaced by one of the functions
 * &quot;minimum&quot;, &quot;maximum&quot;, &quot;average&quot;, and &quot;none&quot;, which is applied to the non
 * missing attribute values of the example set. &quot;none&quot; means, that the value is not replaced. The function can
 * be selected using the parameter list <code>columns</code>. If an attribute's name appears in this list as a key, the
 * value is used as the function name. If the attribute's name is not in the list, the function specified by the
 * <code>default</code> parameter is used. For nominal attributes the mode is used for the average, i.e. the nominal
 * value which occurs most often in the data. For nominal attributes and replacement type zero the first nominal value
 * defined for this attribute is used. The replenishment &quot;value&quot; indicates that the user defined parameter
 * should be used for the replacement.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class MissingValueReplenishment extends ValueReplenishment {

	/** The parameter name for &quot;This value is used for some of the replenishment types.&quot; */
	public static final String PARAMETER_REPLENISHMENT_VALUE = "replenishment_value";

	private static final int NONE = 0;

	private static final int MINIMUM = 1;

	private static final int MAXIMUM = 2;

	private static final int AVERAGE = 3;

	private static final int ZERO = 4;

	private static final int VALUE = 5;

	private static final String[] REPLENISHMENT_NAMES = { "none", "minimum", "maximum", "average", "zero", "value" };

	public MissingValueReplenishment(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd) throws UndefinedParameterError {
		amd.setNumberOfMissingValues(new MDInteger(0));
		return Collections.singletonList(amd);
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.VALUE_TYPE };
	}

	@Override
	public String[] getFunctionNames() {
		return REPLENISHMENT_NAMES;
	}

	@Override
	public int getDefaultFunction() {
		return AVERAGE;
	}

	@Override
	public int getDefaultColumnFunction() {
		return AVERAGE;
	}

	@Override
	public double getReplacedValue() {
		return Double.NaN;
	}

	@Override
	public double getReplenishmentValue(int functionIndex, ExampleSet exampleSet, Attribute attribute) throws UserError {
		switch (functionIndex) {
		case NONE:
			return Double.NaN;
		case MINIMUM:
			return exampleSet.getStatistics(attribute, Statistics.MINIMUM);
		case MAXIMUM:
			return exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
		case AVERAGE:
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
				return Double.NaN;
			} else if (attribute.isNominal()) {
				return exampleSet.getStatistics(attribute, Statistics.MODE);
			} else {
				return exampleSet.getStatistics(attribute, Statistics.AVERAGE);
			}
		case ZERO:
			return 0.0d;
		case VALUE:
			String valueString = getParameterAsString(PARAMETER_REPLENISHMENT_VALUE);
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
				String formatString = null;
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE)) {
					formatString = "MM/dd/yyyy";
				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.TIME)) {
					formatString = "hh.mm a";
				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
					formatString = "MM/dd/yyyy hh.mm a";
				}
				SimpleDateFormat dateFormat = new SimpleDateFormat(formatString, Locale.US);
				try {
					Date date = dateFormat.parse(valueString);
					return date.getTime();
				} catch (ParseException e) {
					throw new UserError(this, 218, PARAMETER_REPLENISHMENT_VALUE, valueString);
				}
			} else if (attribute.isNominal()) {
				return attribute.getMapping().mapString(valueString);
			} else {
				try {
					return Double.parseDouble(valueString);
				} catch (NumberFormatException e) {
					throw new UserError(this, 211, PARAMETER_REPLENISHMENT_VALUE, valueString);
				}
			}
		default:
			throw new RuntimeException("Illegal value functionIndex: " + functionIndex);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeString type = new ParameterTypeString(PARAMETER_REPLENISHMENT_VALUE, "This value is used for some of the replenishment types.", true, false);
		type.registerDependencyCondition(new ParameterCondition(this, PARAMETER_DEFAULT, true) {
			@Override
			public boolean isConditionFullfilled() {
				// check if any of the options is set to value
				try {
					if (getParameterAsInt(PARAMETER_DEFAULT) == VALUE)
						return true;
					List<String[]> pairs = getParameterList(PARAMETER_COLUMNS);
					if (pairs != null) {
						for (String[] pair : pairs) {
							if (pair[1].equals("value") || pair[1].equals(""+VALUE))
								return true;
						}
					}
				} catch (UndefinedParameterError e) {
				}
				return false;
			}
		});
		types.add(type);
		return types;
	}

}
