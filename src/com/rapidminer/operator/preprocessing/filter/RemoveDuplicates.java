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
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;

/**
 * This operator removed duplicate examples from an example set by comparing all examples
 * with each other on basis of the specified attributes.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class RemoveDuplicates extends AbstractDataProcessing {

	private AttributeSubsetSelector subsetSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());

	public RemoveDuplicates(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		metaData.getNumberOfExamples().reduceByUnknownAmount();
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {		 		
		// partition: 0 select, 1 deselect
		int[] partition = new int[exampleSet.size()];
		Set<Attribute> compareAttributes = subsetSelector.getAttributeSubset(exampleSet, false);

		// if set is empty: Nothing can be done!
		if (compareAttributes.isEmpty())
			throw new UserError(this, 153, 1, 0);
		
		for (int i = 0; i < exampleSet.size(); i++) {
			Example example = exampleSet.getExample(i);
			for (int j = i + 1; j < exampleSet.size(); j++) {
				Example compExample = exampleSet.getExample(j);
				if (partition[j] == 0) {
					boolean equal = true;
					for (Attribute attribute : compareAttributes) {
						if (attribute.isNominal()) {
							String firstValue  = example.getNominalValue(attribute);
							String secondValue = compExample.getNominalValue(attribute);
							if (!firstValue.equals(secondValue)) {
								equal = false;
								break;
							}
						} else {
							if (example.getValue(attribute) != compExample.getValue(attribute)) {
								equal = false;
								break;
							}
						}
					}

					if (equal) {
						partition[j] = 1;
					}
				}
			}
		}

		SplittedExampleSet result = new SplittedExampleSet(exampleSet, new Partition(partition, 2));
		result.selectSingleSubset(0);

		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(subsetSelector.getParameterTypes());

		return types;
	}
}
