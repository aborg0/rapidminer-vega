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
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.BinominalAttribute;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.PolynomialExampleSetResourceConsumptionEstimator;
import com.rapidminer.operator.annotation.PolynomialFunction;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * Adds a value to a nominal attribute definition.
 * 
 * @author Peter B. Volk, Ingo Mierswa
 */
public class AddNominalValue extends AbstractDataProcessing {

	/** The parameter name for &quot;The name of the nominal attribute to which values should be added.&quot; */
	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	/** The parameter name for &quot;The value which should be added.&quot; */
	public static final String PARAMETER_NEW_VALUE = "new_value";

	public AddNominalValue(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(new AttributeSetPrecondition(getExampleSetInputPort(), AttributeSetPrecondition.getAttributesByParameter(this, PARAMETER_ATTRIBUTE_NAME), Ontology.NOMINAL));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		AttributeMetaData targetAttribute = metaData.getAttributeByName(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		if (targetAttribute != null && getParameterAsString(PARAMETER_NEW_VALUE) != null) {
			targetAttribute.getValueSet().add(getParameterAsString(PARAMETER_NEW_VALUE));
			targetAttribute.setValueSetRelation(targetAttribute.getValueSetRelation().merge(SetRelation.SUBSET));
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {		 
		Attribute attribute = exampleSet.getAttributes().get(getParameterAsString(PARAMETER_ATTRIBUTE_NAME));

		// some checks
		if (attribute == null) {
			throw new UserError(this, 111, getParameterAsString(PARAMETER_ATTRIBUTE_NAME));
		}
		if (!attribute.isNominal()) {
			throw new UserError(this, 119, new Object[] { attribute.getName(), this.getName() });
		}

		String newValue = getParameterAsString(PARAMETER_NEW_VALUE);

		if (attribute instanceof BinominalAttribute) {
			Attribute newAttribute = AttributeFactory.createAttribute(Ontology.NOMINAL);
			ExampleTable table = exampleSet.getExampleTable();
			table.addAttribute(newAttribute);
			exampleSet.getAttributes().addRegular(newAttribute);

			NominalMapping originalMapping = attribute.getMapping();
			NominalMapping newMapping = newAttribute.getMapping();
			for (int i = 0; i < originalMapping.size(); i++) {
				newMapping.mapString(originalMapping.mapIndex(i));
			}
			newAttribute.getMapping().mapString(newValue);

			for (Example example: exampleSet) {
				example.setValue(newAttribute, example.getValue(attribute));
			}

			exampleSet.getAttributes().remove(attribute);
			newAttribute.setName(attribute.getName());			
		}  else {
			attribute.getMapping().mapString(newValue);
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME, "The name of the nominal attribute to which values should be added.", getExampleSetInputPort(), false, Ontology.NOMINAL));
		types.add(new ParameterTypeString(PARAMETER_NEW_VALUE, "The value which should be added.", false));
		return types;
	}
	
	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		boolean isBinominal = true;
		try {
			isBinominal = getRequiredMetaData().getAttributeByName(getParameterAsString(PARAMETER_ATTRIBUTE_NAME)).isBinominal();
		} catch (UndefinedParameterError e) {
			return null;
		}
		if (isBinominal) {
			return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), AddNominalValue.class, null);
		} else {
			// constant value
			String[] timeConsumption = OperatorResourceConsumptionHandler.getTimeConsumption(AddNominalValue.class);
			String[] memoryConsumption = OperatorResourceConsumptionHandler.getMemoryConsumption(AddNominalValue.class);
			if (timeConsumption == null || memoryConsumption == null) {
				return null;
			}
			
			PolynomialFunction timeFunction = new PolynomialFunction(Double.parseDouble(timeConsumption[0]), 0, 0);
			PolynomialFunction memoryFunction = new PolynomialFunction(Double.parseDouble(memoryConsumption[0]), 0, 0);
			
			return new PolynomialExampleSetResourceConsumptionEstimator(getInputPort(), null, timeFunction, memoryFunction);
		}
	}
}
