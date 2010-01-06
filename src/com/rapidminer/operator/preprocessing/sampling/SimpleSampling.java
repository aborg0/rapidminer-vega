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
package com.rapidminer.operator.preprocessing.sampling;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SimpleExampleSet;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;


/**
 * Simple sampling operator. This operator performs a random sampling of a given
 * fraction. For example, if the input example set contains 5000 examples and
 * the sample ratio is set to 0.1, the result will have approximately 500
 * examples.
 * 
 * @author Ingo Mierswa
 */
public class SimpleSampling extends AbstractSamplingOperator {

	private class SimpleSamplingDataRowReader extends SamplingDataRowReader {
		private final RandomGenerator random;
		private SimpleSamplingDataRowReader(ExampleSet exampleSet, RandomGenerator random) {
			super(exampleSet.iterator());
			this.random = random;
		}
		@Override
		public boolean uses(Example example) {
			return random.nextDouble() < fraction;
		}		
	}

	/** The parameter name for &quot;The fraction of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	private double fraction = 0.1d;

	public SimpleSampling(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		if (emd.getNumberOfExamples().isKnown())
			return new MDInteger((int) ((getParameterAsDouble(PARAMETER_SAMPLE_RATIO) * emd.getNumberOfExamples().getValue())));
		return new MDInteger();
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {		 
		this.fraction = getParameterAsDouble(PARAMETER_SAMPLE_RATIO);

		// fill new table
		//		List<DataRow> dataList = new LinkedList<DataRow>();
		//		Iterator<Example> reader = exampleSet.iterator();
		//		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		//		while (reader.hasNext()) {
		//			Example example = reader.next();
		//			if (accept(example, random)) {
		//				dataList.add(example.getDataRow());
		//			}
		//			checkForStop();
		//		}

		List<Attribute> attributes = Arrays.asList(exampleSet.getExampleTable().getAttributes());
		//ExampleTable exampleTable = new MemoryExampleTable(attributes, new ListDataRowReader(dataList.iterator()));
		ExampleTable exampleTable = new MemoryExampleTable(attributes, new SimpleSamplingDataRowReader(exampleSet, RandomGenerator.getRandomGenerator(this)));

		// regular attributes
		List<Attribute> regularAttributes = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			regularAttributes.add(attribute);
		}

		// special attributes
		ExampleSet result = new SimpleExampleSet(exampleTable, regularAttributes);
		Iterator<AttributeRole> special = exampleSet.getAttributes().specialAttributes();
		while (special.hasNext()) {
			AttributeRole role = special.next();
			result.getAttributes().setSpecialAttribute(role.getAttribute(), role.getSpecialName());
		}

		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO, "The fraction of examples which should be sampled", 0.0d, 1.0d, 0.1d);
		type.setExpert(false);
		types.add(type);
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}
}
