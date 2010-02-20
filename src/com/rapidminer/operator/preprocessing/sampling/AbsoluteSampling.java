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
package com.rapidminer.operator.preprocessing.sampling;

import java.util.Arrays;
import java.util.Collections;
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
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;


/**
 * Absolute sampling operator. This operator takes a random sample with the
 * given size. For example, if the sample size is set to 50, the result will
 * have exactly 50 examples randomly drawn from the complete data set. Please
 * note that this operator does not sample during a data scan but jumps to the
 * rows. It should therefore only be used in case of memory data management and
 * not, for example, for database or file management.
 * 
 * @author Ingo Mierswa
 */
public class AbsoluteSampling extends AbstractSamplingOperator {

	private class AbsoluteSamplingDataRowReader extends SamplingDataRowReader {

		private int toCome;
		private int toAccept;
		private final RandomGenerator random;
		protected AbsoluteSamplingDataRowReader(ExampleSet exampleSet, int target, RandomGenerator random) {
			super(exampleSet.iterator());
			this.toCome = exampleSet.size();
			this.toAccept = target;
			this.random = random;
		}
		
		@Override
		public boolean uses(Example example) {			
			boolean accept = (random.nextInt(toCome) + 1) <= toAccept;
			if (accept) {
				toAccept--;
			}
			toCome--;
			return accept;
		}		
	}
	
	/** The parameter name for &quot;The number of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	public AbsoluteSampling(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		int absoluteNumber = getParameterAsInt(PARAMETER_SAMPLE_SIZE);

		if (emd.getNumberOfExamples().isAtLeast(absoluteNumber) == MetaDataInfo.NO)
			getExampleSetInputPort().addError(
					new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), 
							Collections.singletonList(new ParameterSettingQuickFix(this, PARAMETER_SAMPLE_SIZE, emd.getNumberOfExamples().getValue().toString())),
							"exampleset.need_more_examples", absoluteNumber + ""));
		return new MDInteger(absoluteNumber);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {		
		int size = getParameterAsInt(PARAMETER_SAMPLE_SIZE);

		if (size > exampleSet.size()) {
			throw new UserError(this, 110, size);
		}

		// fill new table
//		List<Integer> indices = new ArrayList<Integer>(exampleSet.size());
//		for (int i = 0; i < exampleSet.size(); i++) {
//			indices.add(i);
//		}
//		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
//		List<DataRow> dataList = new LinkedList<DataRow>();
//		for (int i = 0; i < size; i++) {
//			int index = indices.remove(random.nextInt(indices.size()));
//			dataList.add(exampleSet.getExample(index).getDataRow());
//		}
		List<Attribute> attributes = Arrays.asList(exampleSet.getExampleTable().getAttributes());
//		ExampleTable exampleTable = new MemoryExampleTable(attributes, new ListDataRowReader(dataList.iterator()));
		ExampleTable exampleTable = new MemoryExampleTable(attributes, new AbsoluteSamplingDataRowReader(exampleSet, size, RandomGenerator.getRandomGenerator(this)));

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
		ParameterType type = new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The number of examples which should be sampled", 1, Integer.MAX_VALUE, 100);
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
