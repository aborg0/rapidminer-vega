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
package com.rapidminer.operator.preprocessing.normalization;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDReal;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.container.Range;


/**
 * This operator performs a normalization. This can be done between a user
 * defined minimum and maximum value or by a z-transformation, i.e. on mean 0
 * and variance 1. or by a proportional transformation as proportion of the total sum
 * of the respective attribute.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class Normalization extends PreprocessingOperator {

	public static final String[] NORMALIZATION_METHODS = new String[] {
		"Z-transformation",
		"range transformation",
		"proportion transformation"
	};

	public static final int METHOD_Z_TRANSFORMATION = 0;

	public static final int METHOD_RANGE_TRANSFORMATION = 1;

	public static final int METHOD_PROPORTION_TRANSFORMATION = 2;

	public static final String PARAMETER_NORMALIZATION_METHOD = "method";

	/** The parameter name for &quot;The minimum value after normalization&quot; */
	public static final String PARAMETER_MIN = "min";

	/** The parameter name for &quot;The maximum value after normalization&quot; */
	public static final String PARAMETER_MAX = "max";

	/** Creates a new Normalization operator. */
	public Normalization(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd) throws UndefinedParameterError {
		if (amd.isNumerical()) {
			amd.setType(Ontology.REAL);
			int method = getParameterAsInt(PARAMETER_NORMALIZATION_METHOD);
			switch (method) {
			case METHOD_Z_TRANSFORMATION:
				amd.setMean(new MDReal((double) 0));
				amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
				return Collections.singleton(amd);
			case METHOD_RANGE_TRANSFORMATION:
				double min = getParameterAsDouble(PARAMETER_MIN);
				double max = getParameterAsDouble(PARAMETER_MAX);
				amd.setMean(new MDReal());
				amd.setValueRange(new Range(min, max), SetRelation.EQUAL);
				return Collections.singleton(amd);
			case METHOD_PROPORTION_TRANSFORMATION:
				if (amd.getValueSetRelation() == SetRelation.EQUAL) {
					if (emd.getNumberOfExamples().isKnown())
						amd.setMean(new MDReal(1d / emd.getNumberOfExamples().getValue()));
					else
						amd.setMean(new MDReal());
					Range range = amd.getValueRange();
					if (range.getLower() < 0d)
						getExampleSetInputPort().addError(new SimpleMetaDataError(Severity.WARNING, getExampleSetInputPort(), "attribute_contains_negative_values", amd.getName(), NORMALIZATION_METHODS[2]));
				} else {
					// set to unknown
					amd.setMean(new MDReal());
					amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.UNKNOWN);
				}
				return Collections.singleton(amd);
			}
		}
		return Collections.singleton(amd);
	}

	/**
	 * Depending on the parameter value of &quot;standardize&quot; this method
	 * creates either a ZTransformationModel, MinMaxNormalizationModel or PercentageNormalizationModel.
	 */
	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {

		int method = getParameterAsInt(PARAMETER_NORMALIZATION_METHOD);
		if (method == METHOD_Z_TRANSFORMATION) {
			// Z-Transformation
			exampleSet.recalculateAllAttributeStatistics();
			HashMap<String, Tupel<Double, Double>> attributeMeanVarianceMap = new HashMap<String, Tupel<Double, Double>>(); 
			for (Attribute attribute : exampleSet.getAttributes()) {
				if (attribute.isNumerical()) {
					attributeMeanVarianceMap.put(attribute.getName(), new Tupel<Double, Double>( 
							exampleSet.getStatistics(attribute, Statistics.AVERAGE),
							exampleSet.getStatistics(attribute, Statistics.VARIANCE)));
				}
			}
			ZTransformationModel model = new ZTransformationModel(exampleSet, attributeMeanVarianceMap);
			return model;
		} else if (method == METHOD_RANGE_TRANSFORMATION){
			// Range Normalization
			double min = getParameterAsDouble(PARAMETER_MIN);
			double max = getParameterAsDouble(PARAMETER_MAX);
			if (max <= min)
				throw new UserError(this, 116, "max", "Must be greater than 'min'");

			// calculating attribute ranges
			HashMap<String, Tupel<Double, Double>> attributeRanges = new HashMap<String, Tupel<Double, Double>>();
			exampleSet.recalculateAllAttributeStatistics();
			for (Attribute attribute : exampleSet.getAttributes()) {
				if (attribute.isNumerical()) {
					attributeRanges.put(attribute.getName(), new Tupel<Double, Double>(exampleSet.getStatistics(attribute, Statistics.MINIMUM), exampleSet.getStatistics(attribute, Statistics.MAXIMUM)));
				}
			}
			return new MinMaxNormalizationModel(exampleSet, min, max, attributeRanges);
		} else {
			// Percentage Normalization
			// calculating attribute sums
			Attributes attributes = exampleSet.getAttributes();
			double[] attributeSum = new double[attributes.size()];
			for (Example example: exampleSet) {
				int i = 0;
				for (Attribute attribute: attributes) {
					if (attribute.isNumerical()) {
						attributeSum[i] += example.getValue(attribute);
					}
					i++;
				}
			}
			HashMap<String, Double> attributeSums = new HashMap<String, Double>();
			int i = 0;
			for (Attribute attribute : exampleSet.getAttributes()) {
				if (attribute.isNumerical()) {
					attributeSums.put(attribute.getName(), attributeSum[i]);
				}
				i++;
			}

			return new ProportionNormalizationModel(exampleSet, attributeSums); 
		}
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return AbstractNormalizationModel.class;
	}
	
	/** Returns a list with all parameter types of this model. */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_NORMALIZATION_METHOD, "Select the normalization method.", NORMALIZATION_METHODS, 0));
		ParameterType type = new ParameterTypeDouble(PARAMETER_MIN, "The minimum value after normalization", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_NORMALIZATION_METHOD, NORMALIZATION_METHODS, true, new int[] {1}));
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_MAX, "The maximum value after normalization", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_NORMALIZATION_METHOD, NORMALIZATION_METHODS, true, new int[] {1}));
		types.add(type);
		return types;
	}


	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NUMERICAL };
	}
}
