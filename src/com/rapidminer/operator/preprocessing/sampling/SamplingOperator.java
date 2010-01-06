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
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.RandomGenerator;


/**
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class SamplingOperator extends AbstractSamplingOperator {
	
	private class ProbabilitySamplingDataRowReader extends SamplingDataRowReader {
		
		private final RandomGenerator random;
		
		private double fraction;
		
		private ProbabilitySamplingDataRowReader(ExampleSet exampleSet, double fraction, RandomGenerator random) {
			super(exampleSet.iterator());
			this.random = random;
		}
		@Override
		public boolean uses(Example example) {
			return random.nextDouble() < fraction;
		}		
	}

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
	
	public static final String PARAMETER_SAMPLE = "sample";
	
	public static final String[] SAMPLE_MODES = { "absolute" , "relative" , "probability" };
	
	public static final int SAMPLE_ABSOLUTE = 0;
	
	public static final int SAMPLE_RELATIVE = 1;
	
	public static final int SAMPLE_PROBABILITY = 2;
	
	/** The parameter name for &quot;The number of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";
	
	/** The parameter name for &quot;The fraction of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";
	
	public static final String PARAMETER_SAMPLE_PROBABILITY = "sample_probability";

	public SamplingOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		switch (getParameterAsInt(PARAMETER_SAMPLE)) {
		case SAMPLE_ABSOLUTE:
			int absoluteNumber = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
			if (emd.getNumberOfExamples().isAtLeast(absoluteNumber) == MetaDataInfo.NO) {
				getExampleSetInputPort().addError(
						new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), 
								Collections.singletonList(new ParameterSettingQuickFix(this, PARAMETER_SAMPLE_SIZE, emd.getNumberOfExamples().getValue().toString())),
								"exampleset.need_more_examples", absoluteNumber + ""));
			}
			return new MDInteger(absoluteNumber);
		case SAMPLE_RELATIVE:
			if (emd.getNumberOfExamples().isKnown()) {
				return new MDInteger((int) ((getParameterAsDouble(PARAMETER_SAMPLE_RATIO) * emd.getNumberOfExamples().getValue())));
			}
			return new MDInteger();
		case SAMPLE_PROBABILITY:
		default:
			return new MDInteger();
		}
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		SamplingDataRowReader sampleReader = null;
		switch (getParameterAsInt(PARAMETER_SAMPLE)) {
		case SAMPLE_RELATIVE:
			sampleReader = new AbsoluteSamplingDataRowReader(exampleSet, (int) (exampleSet.size() * getParameterAsDouble(PARAMETER_SAMPLE_RATIO)), RandomGenerator.getRandomGenerator(this));
			break;
		case SAMPLE_ABSOLUTE:
			int size = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
			if (size > exampleSet.size()) {
				throw new UserError(this, 110, size);
			}
			sampleReader = new AbsoluteSamplingDataRowReader(exampleSet, size, RandomGenerator.getRandomGenerator(this));
			break;
		case SAMPLE_PROBABILITY:
		default:
			sampleReader = new ProbabilitySamplingDataRowReader(exampleSet, 1, RandomGenerator.getRandomGenerator(this));
			break;
		}
		
		// fill new table
		List<Attribute> attributes = Arrays.asList(exampleSet.getExampleTable().getAttributes());
		ExampleTable exampleTable = new MemoryExampleTable(attributes, sampleReader);

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
		ParameterType type = new ParameterTypeCategory(PARAMETER_SAMPLE, "Determines how the amount of data is specified.", SAMPLE_MODES, SAMPLE_ABSOLUTE);  
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The number of examples which should be sampled", 1, Integer.MAX_VALUE, 100);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_ABSOLUTE));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO, "The fraction of examples which should be sampled", 0.0d, 1.0d, 0.1d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_RELATIVE));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_SAMPLE_PROBABILITY, "The sample probability for each example.", 0.0d, 1.0d, 0.1d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_PROBABILITY));
		type.setExpert(false);
		types.add(type);
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}
}
