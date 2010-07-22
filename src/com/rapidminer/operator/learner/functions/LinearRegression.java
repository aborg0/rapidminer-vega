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
package com.rapidminer.operator.learner.functions;

import java.util.Iterator;
import java.util.List;

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
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.FDistribution;

/**
 * <p>
 * This operator calculates a linear regression model. It uses the Akaike criterion for model selection.
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
		Attribute label = exampleSet.getAttributes().getLabel();
		Attribute workingLabel = label;
		boolean cleanUpLabel = false;
		String firstClassName = null;
		String secondClassName = null;

		boolean useBias = getParameterAsBoolean(PARAMETER_USE_BIAS);
		boolean removeColinearAttributes = getParameterAsBoolean(PARAMETER_ELIMINATE_COLINEAR_FEATURES);
		double ridge = getParameterAsDouble(PARAMETER_RIDGE);
		double minStandardizedCoefficient = getParameterAsDouble(PARAMETER_MIN_STANDARDIZED_COEFFICIENT);

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

		// start with all attributes
		int numberOfAttributes = exampleSet.getAttributes().size();
		boolean[] attributeSelection = new boolean[numberOfAttributes];
		int counter = 0;
		String[] attributeNames = new String[numberOfAttributes];
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributeSelection[counter] = attribute.isNumerical();
			attributeNames[counter] = attribute.getName();
			counter++;
		}

		// compute and store statistics and turn off attributes with std. dev. = 0
		exampleSet.recalculateAllAttributeStatistics();
		double[] means = new double[numberOfAttributes];
		double[] standardDeviations = new double[numberOfAttributes];
		counter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attributeSelection[counter]) {
				means[counter] = exampleSet.getStatistics(attribute, Statistics.AVERAGE_WEIGHTED);
				standardDeviations[counter] = Math.sqrt(exampleSet.getStatistics(attribute, Statistics.VARIANCE_WEIGHTED));
				if (standardDeviations[counter] == 0) {
					attributeSelection[counter] = false;
				}
			}
			counter++;
		}

		double labelMean = exampleSet.getStatistics(workingLabel, Statistics.AVERAGE_WEIGHTED);
		double classStandardDeviation = Math.sqrt(exampleSet.getStatistics(workingLabel, Statistics.VARIANCE_WEIGHTED));

		int numberOfExamples = exampleSet.size();
		double[] coefficients;

		// perform a regression and remove colinear attributes
		do {
			coefficients = performRegression(exampleSet, attributeSelection, means, labelMean, ridge);
		} while (removeColinearAttributes && deselectAttributeWithHighestCoefficient(attributeSelection, coefficients, standardDeviations, classStandardDeviation, minStandardizedCoefficient));

		// determine the current number of attributes + 1
		int currentlySelectedAttributes = 1;
		for (int i = 0; i < attributeSelection.length; i++) {
			if (attributeSelection[i]) {
				currentlySelectedAttributes++;
			}
		}

		double error = getSquaredError(exampleSet, attributeSelection, coefficients, useBias);
		double akaike = (numberOfExamples - currentlySelectedAttributes) + 2 * currentlySelectedAttributes;

		boolean improved;
		int currentNumberOfAttributes = currentlySelectedAttributes;
		double currentError = Double.NaN;
		switch (getParameterAsInt(PARAMETER_FEATURE_SELECTION)) {
		case GREEDY:
			do {
				boolean[] currentlySelected = attributeSelection.clone();
				improved = false;
				currentNumberOfAttributes--;
				for (int i = 0; i < attributeSelection.length; i++) {
					if (currentlySelected[i]) {
						// calculate the akaike value without this attribute
						currentlySelected[i] = false;
						double[] currentCoeffs = performRegression(exampleSet, currentlySelected, means, labelMean, ridge);
						currentError = getSquaredError(exampleSet, currentlySelected, currentCoeffs, useBias);
						double currentAkaike = currentError / error * (numberOfExamples - currentlySelectedAttributes) + 2 * currentNumberOfAttributes;

						// if the value is improved compared to the current best
						if (currentAkaike < akaike) {
							improved = true;
							akaike = currentAkaike;
							System.arraycopy(currentlySelected, 0, attributeSelection, 0, attributeSelection.length);
							coefficients = currentCoeffs;
						}
						currentlySelected[i] = true;
					}
				}
			} while (improved);
			break;
		case M5_PRIME:
			// attribute removal as in M5 prime
			do {
				improved = false;
				currentNumberOfAttributes--;

				// find the attribute with the smallest standardized coefficient
				double minStadardizedCoefficient = 0;
				int attribute2Deselect = -1;
				int coefficientIndex = 0;
				for (int i = 0; i < attributeSelection.length; i++) {
					if (attributeSelection[i]) {
						double standardizedCoefficient = Math.abs(coefficients[coefficientIndex] * standardDeviations[i] / classStandardDeviation);
						if ((coefficientIndex == 0) || (standardizedCoefficient < minStadardizedCoefficient)) {
							minStadardizedCoefficient = standardizedCoefficient;
							attribute2Deselect = i;
						}
						coefficientIndex++;
					}
				}

				// check if removing this attribute improves Akaike
				if (attribute2Deselect >= 0) {
					attributeSelection[attribute2Deselect] = false;
					double[] currentCoefficients = performRegression(exampleSet, attributeSelection, means, labelMean, ridge);
					currentError = getSquaredError(exampleSet, attributeSelection, currentCoefficients, useBias);
					double currentAkaike = currentError / error * (numberOfExamples - currentlySelectedAttributes) + 2 * currentNumberOfAttributes;

					if (currentAkaike < akaike) {
						improved = true;
						akaike = currentAkaike;
						coefficients = currentCoefficients;
					} else {
						attributeSelection[attribute2Deselect] = true;
					}
				}
			} while (improved);
			break;
		case NO_SELECTION:
			currentError = error;
			break;
		}

		// clean up?
		if (cleanUpLabel) {
			exampleSet.getAttributes().remove(workingLabel);
			exampleSet.getExampleTable().removeAttribute(workingLabel);
			exampleSet.getAttributes().setLabel(label);
		}

		FDistribution fdistribution = new FDistribution(1, exampleSet.size() - coefficients.length);
		int length = coefficients.length;
		double[] standardErrors = new double[length - 1];
		double[] standardizedCoefficients = new double[length - 1];
		double[] tStatistics = new double[length - 1];
		double[] pValues = new double[length - 1];
		int index = 0;
		for (int i = 0; i < attributeSelection.length; i++) {
			if (attributeSelection[i]) {
				standardErrors[index] = Math.sqrt(currentError) / (standardDeviations[i] * (exampleSet.size() - coefficients.length));
				standardizedCoefficients[index] = coefficients[index] * standardDeviations[i] / classStandardDeviation;
				tStatistics[index] = coefficients[index] / standardErrors[index];
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
				if (attributeSelection[i]) {
					weights.setWeight(attributeNames[i], coefficients[selectedAttributes]);
					selectedAttributes++;
				} else {
					weights.setWeight(attributeNames[i], 0);
				}
			}
			weightOutput.deliver(weights);
		}

		return new LinearRegressionModel(exampleSet, attributeSelection, coefficients, standardErrors, standardizedCoefficients, tStatistics, pValues, useBias, firstClassName, secondClassName);
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
	private double getSquaredError(ExampleSet exampleSet, boolean[] selectedAttributes, double[] coefficients, boolean useIntercept) {
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
	private double[] performRegression(ExampleSet exampleSet, boolean[] selectedAttributes, double[] means, double labelMean, double ridge) throws UndefinedParameterError {
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
		types.add(new ParameterTypeCategory(PARAMETER_FEATURE_SELECTION, "The feature selection method used during regression.", FEATURE_SELECTION_METHODS, M5_PRIME));
		types.add(new ParameterTypeBoolean(PARAMETER_ELIMINATE_COLINEAR_FEATURES, "Indicates if the algorithm should try to delete colinear features during the regression.", true));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_BIAS, "Indicates if an intercept value should be calculated.", true));
		types.add(new ParameterTypeDouble(PARAMETER_MIN_STANDARDIZED_COEFFICIENT, "The minimum standardized coefficient for the removal of colinear feature elimination.", 0.0d, Double.POSITIVE_INFINITY, 1.5d));
		types.add(new ParameterTypeDouble(PARAMETER_RIDGE, "The ridge parameter used during ridge regression.", 0.0d, Double.POSITIVE_INFINITY, 1.0E-8));
		return types;
	}
}
