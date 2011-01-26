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
package com.rapidminer.operator.learner.functions.linear;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.functions.linear.LinearRegressionMethod.LinearRegressionResult;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.FDistribution;

/**
 * <p>
 * This operator calculates a linear regression model. It supports several different mechanisms for model selection: -
 * M5Prime using Akaike criterion for model selection. - A greedy implementation - A T-Test based selection - No
 * selection. Further selections can be added using the static method
 * 
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class LinearRegression extends AbstractLearner {

	/** The parameter name for &quot;The feature selection method used during regression.&quot; */
	public static final String PARAMETER_FEATURE_SELECTION = "feature_selection";

	/**
	 * The parameter name for &quot;Indicates if the algorithm should try to delete colinear features during the
	 * regression.&quot;
	 */
	public static final String PARAMETER_ELIMINATE_COLINEAR_FEATURES = "eliminate_colinear_features";

	public static final String PARAMETER_USE_BIAS = "use_bias";

	/**
	 * The parameter name for &quot;The minimum standardized coefficient for the removal of colinear feature
	 * elimination.&quot;
	 */
	public static final String PARAMETER_MIN_STANDARDIZED_COEFFICIENT = "min_standardized_coefficient";

	/** The parameter name for &quot;The ridge parameter used during ridge regression.&quot; */
	public static final String PARAMETER_RIDGE = "ridge";

	/** Attribute selection methods */
	public static final String[] FEATURE_SELECTION_METHODS = { "none", "M5 prime", "greedy" };

	public static final Map<String, Class<? extends LinearRegressionMethod>> SELECTION_METHODS = new LinkedHashMap<String, Class<? extends LinearRegressionMethod>>();

	static {
		SELECTION_METHODS.put("none", PlainLinearRegressionMethod.class);
		SELECTION_METHODS.put("M5 prime", M5PLinearRegressionMethod.class);
		SELECTION_METHODS.put("greedy", GreedyLinearRegressionMethod.class);
		SELECTION_METHODS.put("T-Test", TTestLinearRegressionMethod.class);
	}

	/** Attribute selection method: No attribute selection */
	public static final int NO_SELECTION = 0;

	/** Attribute selection method: M5 method */
	public static final int M5_PRIME = 1;

	/** Attribute selection method: Greedy method */
	public static final int GREEDY = 2;

	private OutputPort weightOutput = getOutputPorts().createPort("weights");

	public LinearRegression(OperatorDescription description) {
		super(description);

		getTransformer().addGenerationRule(weightOutput, AttributeWeights.class);
	}

	public Model learn(ExampleSet exampleSet) throws OperatorException {
		// initializing data and parameter values.
		Attribute label = exampleSet.getAttributes().getLabel();
		Attribute workingLabel = label;
		boolean cleanUpLabel = false;
		String firstClassName = null;
		String secondClassName = null;

		boolean useBias = getParameterAsBoolean(PARAMETER_USE_BIAS);
		boolean removeColinearAttributes = getParameterAsBoolean(PARAMETER_ELIMINATE_COLINEAR_FEATURES);
		double ridge = getParameterAsDouble(PARAMETER_RIDGE);
		double minStandardizedCoefficient = getParameterAsDouble(PARAMETER_MIN_STANDARDIZED_COEFFICIENT);

		// prepare for classification by translating into 0-1 coding.
		if (label.isNominal()) {
			if (label.getMapping().size() == 2) {
				firstClassName = label.getMapping().getNegativeString();
				secondClassName = label.getMapping().getPositiveString();

				int firstIndex = label.getMapping().getNegativeIndex();

				workingLabel = AttributeFactory.createAttribute("regression_label", Ontology.REAL);
				exampleSet.getExampleTable().addAttribute(workingLabel);

				for (Example example : exampleSet) {
					double index = example.getValue(label);
					if (index == firstIndex) {
						example.setValue(workingLabel, 0.0d);
					} else {
						example.setValue(workingLabel, 1.0d);
					}
				}

				exampleSet.getAttributes().setLabel(workingLabel);
				cleanUpLabel = true;
			}
		}

		// search all attributes and keep numerical
		int numberOfAttributes = exampleSet.getAttributes().size();
		boolean[] isUsedAttribute = new boolean[numberOfAttributes];
		int counter = 0;
		String[] attributeNames = new String[numberOfAttributes];
		for (Attribute attribute : exampleSet.getAttributes()) {
			isUsedAttribute[counter] = attribute.isNumerical();
			attributeNames[counter] = attribute.getName();
			counter++;
		}

		
		// compute and store statistics and turn off attributes with std. dev. = 0
		exampleSet.recalculateAllAttributeStatistics();
		double[] means = new double[numberOfAttributes];
		double[] standardDeviations = new double[numberOfAttributes];
		counter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (isUsedAttribute[counter]) {
				means[counter] = exampleSet.getStatistics(attribute, Statistics.AVERAGE_WEIGHTED);
				standardDeviations[counter] = Math.sqrt(exampleSet.getStatistics(attribute, Statistics.VARIANCE_WEIGHTED));
				if (standardDeviations[counter] == 0) {
					isUsedAttribute[counter] = false;
				}
			}
			counter++;
		}

		double labelMean = exampleSet.getStatistics(workingLabel, Statistics.AVERAGE_WEIGHTED);
		double labelStandardDeviation = Math.sqrt(exampleSet.getStatistics(workingLabel, Statistics.VARIANCE_WEIGHTED));

		int numberOfExamples = exampleSet.size();
		
		// determine the number of used attributes + 1
		int numberOfUsedAttributes = 1;
		for (int i = 0; i < isUsedAttribute.length; i++) {
			if (isUsedAttribute[i]) {
				numberOfUsedAttributes++;
			}
		}
		
		// perform a full regression and remove colinear attributes
		double[] coefficientsOnFullData;
		// TODO: Is this really sane?
		do {
			coefficientsOnFullData = performRegression(exampleSet, isUsedAttribute, means, labelMean, ridge);
		} while (removeColinearAttributes && deselectAttributeWithHighestCoefficient(isUsedAttribute, coefficientsOnFullData, standardDeviations, labelStandardDeviation, minStandardizedCoefficient));

		// calculate error on full data
		double errorOnFullData = getSquaredError(exampleSet, isUsedAttribute, coefficientsOnFullData, useBias);

		// apply attribute selection method
		String selectedMethod = getParameterAsString(PARAMETER_FEATURE_SELECTION);
		Class<? extends LinearRegressionMethod> methodClass = SELECTION_METHODS.get(selectedMethod);
		if (methodClass == null) {
			throw new UserError(this, 101);
		}
		LinearRegressionMethod method;
		try {
			method = methodClass.newInstance();
		} catch (InstantiationException e) {
			throw new UserError(this, 101);
		} catch (IllegalAccessException e) {
			throw new UserError(this, 101);
		}
		LinearRegressionResult result = method.applyMethod(this, useBias, ridge, exampleSet, isUsedAttribute, numberOfExamples, numberOfUsedAttributes, means, labelMean, standardDeviations, labelStandardDeviation, coefficientsOnFullData, errorOnFullData);
		
		// clean up eventually if was classification
		if (cleanUpLabel) {
			exampleSet.getAttributes().remove(workingLabel);
			exampleSet.getExampleTable().removeAttribute(workingLabel);
			exampleSet.getAttributes().setLabel(label);
		}

		// calculating statistics of the resulting model
		FDistribution fdistribution = new FDistribution(1, exampleSet.size() - result.coefficients.length);
		int length = result.coefficients.length;
		double[] standardErrors = new double[length - 1];
		double[] standardizedCoefficients = new double[length - 1];
		double[] tStatistics = new double[length - 1];
		double[] pValues = new double[length - 1];
		int index = 0;
		for (int i = 0; i < result.isUsedAttribute.length; i++) {
			if (result.isUsedAttribute[i]) {
				standardErrors[index] = Math.sqrt(result.error) / (standardDeviations[i] * (numberOfExamples - result.coefficients.length));
				standardizedCoefficients[index] = result.coefficients[index] * standardDeviations[i] / labelStandardDeviation;
				tStatistics[index] = result.coefficients[index] / standardErrors[index];
				double probability = fdistribution.getProbabilityForValue(tStatistics[index] * tStatistics[index]);
				pValues[index] = probability < 0 ? 1.0d : 1.0d - probability;
				index++;
			}
		}

		// delivering weights
		if (weightOutput.isConnected()) {
			AttributeWeights weights = new AttributeWeights(exampleSet);
			int selectedAttributes = 0;
			for (int i = 0; i < attributeNames.length; i++) {
				if (isUsedAttribute[i]) {
					weights.setWeight(attributeNames[i], coefficientsOnFullData[selectedAttributes]);
					selectedAttributes++;
				} else {
					weights.setWeight(attributeNames[i], 0);
				}
			}
			weightOutput.deliver(weights);
		}

		return new LinearRegressionModel(exampleSet, result.isUsedAttribute, result.coefficients, standardErrors, standardizedCoefficients, tStatistics, pValues, useBias, firstClassName, secondClassName);
	}



	/**
	 * This method removes the attribute with the highest standardized coefficient greater than the minimum coefficient
	 * parameter. Checks only those attributes which are currently selected. Returns true if an attribute was actually
	 * deselected and false otherwise.
	 */
	private boolean deselectAttributeWithHighestCoefficient(boolean[] selectedAttributes, double[] coefficients, double[] standardDeviations, double classStandardDeviation, double minStandardizedCoefficient) throws UndefinedParameterError {
		// double minCoefficient = getParameterAsDouble(PARAMETER_MIN_STANDARDIZED_COEFFICIENT);
		int attribute2Deselect = -1;
		int coefficientIndex = 0;
		for (int i = 0; i < selectedAttributes.length; i++) {
			if (selectedAttributes[i]) {
				double standardizedCoefficient = Math.abs(coefficients[coefficientIndex] * standardDeviations[i] / classStandardDeviation);
				if (standardizedCoefficient > minStandardizedCoefficient) {
					minStandardizedCoefficient = standardizedCoefficient;
					attribute2Deselect = i;
				}
				coefficientIndex++;
			}
		}
		if (attribute2Deselect >= 0) {
			selectedAttributes[attribute2Deselect] = false;
			return true;
		}
		return false;
	}

	/** Calculates the squared error of a regression model on the training data. */
	double getSquaredError(ExampleSet exampleSet, boolean[] selectedAttributes, double[] coefficients, boolean useIntercept) {
		double error = 0;
		Iterator<Example> i = exampleSet.iterator();
		while (i.hasNext()) {
			Example example = i.next();
			double prediction = regressionPrediction(example, selectedAttributes, coefficients, useIntercept);
			double diff = prediction - example.getLabel();
			error += diff * diff;
		}
		return error;
	}

	/** Calculates the prediction for the given example. */
	private double regressionPrediction(Example example, boolean[] selectedAttributes, double[] coefficients, boolean useIntercept) {
		double prediction = 0;
		int index = 0;
		int counter = 0;
		for (Attribute attribute : example.getAttributes()) {
			if (selectedAttributes[counter++]) {
				prediction += coefficients[index] * example.getValue(attribute);
				index++;
			}
		}

		if (useIntercept)
			prediction += coefficients[index];

		return prediction;
	}

	/**
	 * Calculate a linear regression only from the selected attributes. The method returns the calculated coefficients.
	 */
	double[] performRegression(ExampleSet exampleSet, boolean[] selectedAttributes, double[] means, double labelMean, double ridge) throws UndefinedParameterError {
		int currentlySelectedAttributes = 0;
		for (int i = 0; i < selectedAttributes.length; i++) {
			if (selectedAttributes[i]) {
				currentlySelectedAttributes++;
			}
		}

		Matrix independent = null, dependent = null;
		double[] weights = null;
		if (currentlySelectedAttributes > 0) {
			independent = new Matrix(exampleSet.size(), currentlySelectedAttributes);
			dependent = new Matrix(exampleSet.size(), 1);
			int exampleIndex = 0;
			Iterator<Example> i = exampleSet.iterator();
			weights = new double[exampleSet.size()];
			Attribute weightAttribute = exampleSet.getAttributes().getWeight();
			while (i.hasNext()) {
				Example example = i.next();
				int attributeIndex = 0;
				dependent.set(exampleIndex, 0, example.getLabel());
				int counter = 0;
				for (Attribute attribute : exampleSet.getAttributes()) {
					if (selectedAttributes[counter]) {
						double value = example.getValue(attribute) - means[counter];
						independent.set(exampleIndex, attributeIndex, value);
						attributeIndex++;
					}
					counter++;
				}
				if (weightAttribute != null)
					weights[exampleIndex] = example.getValue(weightAttribute);
				else
					weights[exampleIndex] = 1.0d;
				exampleIndex++;
			}
		}

		double[] coefficients = new double[currentlySelectedAttributes + 1];
		if (currentlySelectedAttributes > 0) {
			double[] coefficientsWithoutIntercept = com.rapidminer.tools.math.LinearRegression.performRegression(independent, dependent, weights, ridge);
			System.arraycopy(coefficientsWithoutIntercept, 0, coefficients, 0, currentlySelectedAttributes);
		}
		coefficients[currentlySelectedAttributes] = labelMean;

		int coefficientIndex = 0;
		for (int i = 0; i < selectedAttributes.length; i++) {
			if (selectedAttributes[i]) {
				coefficients[coefficients.length - 1] -= coefficients[coefficientIndex] * means[i];
				coefficientIndex++;
			}
		}

		return coefficients;
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return LinearRegressionModel.class;
	}

	public boolean supportsCapability(OperatorCapability lc) {
		if (lc.equals(OperatorCapability.NUMERICAL_ATTRIBUTES))
			return true;
		if (lc.equals(OperatorCapability.NUMERICAL_LABEL))
			return true;
		if (lc.equals(OperatorCapability.BINOMINAL_LABEL))
			return true;
		if (lc == OperatorCapability.WEIGHTED_EXAMPLES)
			return true;
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		String[] availableSelectionMethods = SELECTION_METHODS.keySet().toArray(new String[0]);
		types.add(new ParameterTypeCategory(PARAMETER_FEATURE_SELECTION, "The feature selection method used during regression.", availableSelectionMethods, M5_PRIME));

		// adding parameter of methods
		int i = 0;
		for (Entry<String, Class<? extends LinearRegressionMethod>> entry: SELECTION_METHODS.entrySet()) {
			try {
				LinearRegressionMethod method = entry.getValue().newInstance();
				for (ParameterType methodType: method.getParameterTypes()) {
					types.add(methodType);
					methodType.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_FEATURE_SELECTION, availableSelectionMethods, true, i));
				}
			} catch (InstantiationException e) { // can't do anything about this
			} catch (IllegalAccessException e) {
			}
			i++;
		}
		
		types.add(new ParameterTypeBoolean(PARAMETER_ELIMINATE_COLINEAR_FEATURES, "Indicates if the algorithm should try to delete colinear features during the regression.", true));
		ParameterType type = new ParameterTypeDouble(PARAMETER_MIN_STANDARDIZED_COEFFICIENT, "The minimum standardized coefficient for the removal of colinear feature elimination.", 0.0d, Double.POSITIVE_INFINITY, 1.5d);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_ELIMINATE_COLINEAR_FEATURES, true, true));
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_USE_BIAS, "Indicates if an intercept value should be calculated.", true));
		types.add(new ParameterTypeDouble(PARAMETER_RIDGE, "The ridge parameter used for ridge regression. A value of zero switches to ordinary least squares estimate.", 0.0d, Double.POSITIVE_INFINITY, 1.0E-8));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getExampleSetInputPort(), LinearRegression.class, null);
	}
}
