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
package com.rapidminer.operator.learner.functions.linear;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.math.FDistribution;

/**
 * This implements an attribute selection method for linear regression that is based on 
 * a T-Test. It will filter out all attributes whose coefficient is not significantly different
 * from 0.
 * 
 * @author Sebastian Land
 *
 */
public class TTestLinearRegressionMethod implements LinearRegressionMethod {

	public static final String PARAMETER_TYPE_CONFIDENCE_VALUE = "alpha";
	
	@Override
	public LinearRegressionResult applyMethod(LinearRegression regression, boolean useBias, double ridge, ExampleSet exampleSet, boolean[] isUsedAttribute, int numberOfExamples, int numberOfUsedAttributes, double[] means, double labelMean, double[] standardDeviations, double labelStandardDeviation, double[] coefficientsOnFullData, double errorOnFullData) throws UndefinedParameterError {
		double alpha = regression.getParameterAsDouble(PARAMETER_TYPE_CONFIDENCE_VALUE); 
		
		FDistribution fdistribution = new FDistribution(1, exampleSet.size() - coefficientsOnFullData.length);
		int length = coefficientsOnFullData.length;
		double[] standardErrors = new double[length - 1];
		double[] standardizedCoefficients = new double[length - 1];
		double[] tStatistics = new double[length - 1];
		int index = 0;
		for (int i = 0; i < isUsedAttribute.length; i++) {
			if (isUsedAttribute[i]) {
				standardErrors[index] = Math.sqrt(errorOnFullData) / (standardDeviations[i] * (numberOfExamples - coefficientsOnFullData.length));
				standardizedCoefficients[index] = coefficientsOnFullData[index] * standardDeviations[i] / labelStandardDeviation;
				tStatistics[index] = coefficientsOnFullData[index] / standardErrors[index];
				double probability = fdistribution.getProbabilityForValue(tStatistics[index] * tStatistics[index]);
				if ((probability < 0 ? 1.0d : 1.0d - probability) > alpha) {
					isUsedAttribute[i] = false;
				}
				index++;
			}
		}

		LinearRegressionResult result = new LinearRegressionResult();
		result.isUsedAttribute = isUsedAttribute;
		result.coefficients = regression.performRegression(exampleSet, isUsedAttribute, means, labelMean, ridge);
		result.error = regression.getSquaredError(exampleSet, isUsedAttribute, result.coefficients, useBias);
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		LinkedList<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeDouble(PARAMETER_TYPE_CONFIDENCE_VALUE, "This is the significance level of the t-test.", 0, 1, 0.05));
		return types;
	}

}
