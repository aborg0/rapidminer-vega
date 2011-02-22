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
package com.rapidminer.operator.preprocessing.join;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetUnionRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

/**
 * This operator gets two example sets and adds new features to each of both example sets
 * so that both example sets consist of the same set of features. This set is the union
 * or the superset of both original feature sets. The values of the new features are set
 * to missing. This operator only works on the regular attributes and will not change, add,
 * or otherwise modify the existing special attributes.
 * 
 * @author Ingo Mierswa
 */
public class ExampleSetSuperset extends Operator {

	private InputPort exampleSet1Input = getInputPorts().createPort("example set 1", ExampleSet.class);
	private InputPort exampleSet2Input = getInputPorts().createPort("example set 2", ExampleSet.class);
	private OutputPort supersetOutput1 = getOutputPorts().createPort("superset 1");
	private OutputPort supersetOutput2 = getOutputPorts().createPort("superset 2");

	public ExampleSetSuperset(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new ExampleSetUnionRule(exampleSet1Input, exampleSet2Input, supersetOutput1, null) {
			@Override
			protected void transformAddedAttributeMD(ExampleSetMetaData emd, AttributeMetaData newAttribute) {
				newAttribute.setValueSetRelation(SetRelation.UNKNOWN);
				newAttribute.setNumberOfMissingValues(emd.getNumberOfExamples());
			}
		});
		getTransformer().addRule(new ExampleSetUnionRule(exampleSet2Input, exampleSet1Input, supersetOutput2, null) {
			@Override
			protected void transformAddedAttributeMD(ExampleSetMetaData emd, AttributeMetaData newAttribute) {
				newAttribute.setValueSetRelation(SetRelation.UNKNOWN);
				newAttribute.setNumberOfMissingValues(emd.getNumberOfExamples());
			}
		});
	}

	public void superset(ExampleSet exampleSet1, ExampleSet exampleSet2) {		
		// determine attributes missing in ES 1
		List<Attribute> newAttributesForES1 = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet2.getAttributes()) {
			if (exampleSet1.getAttributes().get(attribute.getName()) == null) {
				newAttributesForES1.add(AttributeFactory.createAttribute(attribute.getName(), attribute.getValueType()));
			}
		}

		// determine attributes missing in ES 2
		List<Attribute> newAttributesForES2 = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet1.getAttributes()) {
			if (exampleSet2.getAttributes().get(attribute.getName()) == null) {
				newAttributesForES2.add(AttributeFactory.createAttribute(attribute.getName(), attribute.getValueType()));
			}
		}		

		// add new attributes to ES 1
		for (Attribute attribute : newAttributesForES1) {
			exampleSet1.getExampleTable().addAttribute(attribute);
			exampleSet1.getAttributes().addRegular(attribute);
		}

		// add new attributes to ES 2
		for (Attribute attribute : newAttributesForES2) {
			exampleSet2.getExampleTable().addAttribute(attribute);
			exampleSet2.getAttributes().addRegular(attribute);
		}


		// set all values to missing for ES 1
		for (Example example : exampleSet1) {
			for (Attribute attribute : newAttributesForES1) {
				example.setValue(attribute, Double.NaN);
			}
		}

		// set all values to missing for ES 2
		for (Example example : exampleSet2) {
			for (Attribute attribute : newAttributesForES2) {
				example.setValue(attribute, Double.NaN);
			}
		}
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet1 = exampleSet1Input.getData();
		ExampleSet exampleSet2 = exampleSet2Input.getData();
		superset(exampleSet1, exampleSet2);
		supersetOutput1.deliver(exampleSet1);
		supersetOutput2.deliver(exampleSet2);
	}
	
	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0), ExampleSetSuperset.class, null);
	}
}
