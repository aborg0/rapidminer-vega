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
package com.rapidminer.operator.learner.bayes;

import java.util.ArrayList;
import java.util.Collection;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.distribution.DiscreteDistribution;
import com.rapidminer.tools.math.distribution.Distribution;
import com.rapidminer.tools.math.distribution.NormalDistribution;


/**
 * DistributionModel is a model for learners which estimate distributions 
 * of attribute values from example sets like NaiveBayes.
 * 
 * Predictions are calculated as product of the conditional probabilities
 * for all attributes times the class probability.
 * 
 * The basic learning concept is to simply count occurrences of classes
 * and attribute values. This means no probabilities are calculated during
 * the learning step. This is only done before output. Optionally, this
 * calculation can apply a Laplace correction which means in particular
 * that zero probabilities are avoided which would hide information
 * in distributions of other attributes.
 * 
 * @author Tobias Malbrecht
 */
public class SimpleDistributionModel extends DistributionModel {

	private static final long serialVersionUID = -402827845291958569L;

	private static final String UNKNOWN_VALUE_NAME = "unknown";

	private static final int INDEX_COUNT = 0;

	private static final int INDEX_VALUE_SUM = 1;

	private static final int INDEX_SQUARED_VALUE_SUM = 2;

	private static final int INDEX_MISSING_WEIGHTS = 3;

	private static final int INDEX_MEAN = 0;

	private static final int INDEX_STANDARD_DEVIATION = 1;

	private static final int INDEX_LOG_FACTOR = 2;

	/** The number of classes. */
	private int numberOfClasses;

	/** The number of attributes. */
	private int numberOfAttributes;

	/** Flags indicating which attribute is nominal. */
	private boolean[] nominal;

	/** Class name (used for result displaying). */
	private String className;

	/** Class values (used for result displaying). */
	private String[] classValues;

	/** Attribute names (used for result displaying). */
	private String[] attributeNames;

	/** Nominal attribute values (used for result displaying). */
	private String[][] attributeValues;

	/** Total weight (or number) of examples used to build the model. */  
	private double totalWeight;

	/** Total weight of examples belonging to the separate classes. */
	private double[] classWeights;

	/** 
	 * Specifies the total weight of examples in which the different combinations
	 * of classes and (nominal) attribute values co-occur. In the case of numeric
	 * attributes the (weighted) sum and the (weighted) sum of the squared
	 * attribute values are stored which are needed to calculate the mean and the
	 * standard deviation/variance of the resulting (assumed) normal distribution.  
	 * 
	 * Array dimensions:
	 * 	 1st: attribtues
	 * 	 2nd: classes
	 * 	 3nd: nominal values or value sum (index=0) and squared value sum (index=1)
	 */
	private double[][][] weightSums;

	/** Class log (!) a-priori probabilites. */
	private double[] priors;

	/** 
	 * Specifies the a-postiori distributions. Contains the log (!) a-postiori
	 * probabilites that certain values occur given the class value for nominal
	 * values. Contains the means and standard deviations for numerical attributes.
	 * 
	 * Array dimensions:
	 * 	 1st: attribtues
	 * 	 2nd: classes
	 * 	 3nd: nominal values or mean (index=0) and standard deviation (index=1)
	 */
	private double[][][] distributionProperties;

	/**
	 * Captures if laplace correction should be applied when calculating
	 * probabilities.
	 */
	boolean laplaceCorrectionEnabled;

	/**
	 * Indicates if the model has recently been updated and the actual probabilites
	 * have to be calculated.
	 */
	private boolean modelRecentlyUpdated;

	public SimpleDistributionModel(ExampleSet exampleSet) {
		this(exampleSet, true);
	}

	public SimpleDistributionModel(ExampleSet exampleSet, boolean laplaceCorrectionEnabled) {
		super(exampleSet);
		this.laplaceCorrectionEnabled = laplaceCorrectionEnabled;
		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		numberOfClasses = labelAttribute.getMapping().size();
		numberOfAttributes = exampleSet.getAttributes().size();
		nominal = new boolean[numberOfAttributes];
		attributeNames = new String[numberOfAttributes];
		attributeValues = new String[numberOfAttributes][];
		className = labelAttribute.getName();
		classValues = new String[numberOfClasses];
		for (int i = 0; i < numberOfClasses; i++) {
			classValues[i] = labelAttribute.getMapping().mapIndex(i);
		}
		int attributeIndex = 0;
		weightSums = new double[numberOfAttributes][numberOfClasses][];
		distributionProperties = new double[numberOfAttributes][numberOfClasses][];
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributeNames[attributeIndex] = attribute.getName();
			if (attribute.isNominal()) {
				nominal[attributeIndex] = true;
				int mappingSize = attribute.getMapping().size() + 1;
				attributeValues[attributeIndex] = new String[mappingSize];
				for (int i = 0; i < mappingSize - 1; i++) {
					attributeValues[attributeIndex][i] = attribute.getMapping().mapIndex(i);
				}
				attributeValues[attributeIndex][mappingSize - 1] = UNKNOWN_VALUE_NAME;
				for (int i = 0; i < numberOfClasses; i++) {
					weightSums[attributeIndex][i] = new double[mappingSize];
					distributionProperties[attributeIndex][i] = new double[mappingSize];
				}
			} else {
				nominal[attributeIndex] = false;
				for (int i = 0; i < numberOfClasses; i++) {
					weightSums[attributeIndex][i] = new double[4];
					distributionProperties[attributeIndex][i] = new double[3];
				}
			}
			attributeIndex++;
		}

		//  initialization of total and a priori weight counters
		totalWeight = 0.0d;
		classWeights = new double[numberOfClasses];
		priors = new double[numberOfClasses];

		// update the model
		update(exampleSet);

		// calculate the probabilities
		updateDistributionProperties();
	}

	@Override
	public String[] getAttributeNames() {
		return this.attributeNames;
	}

	@Override
	public int getNumberOfAttributes() {
		return this.attributeNames.length;
	}

	/**
	 * Updates the model by counting the occurances of classes and attribute values
	 * in combination with the class values.
	 * 
	 * ATTENTION: only updates the weight counters, distribution properties are not
	 * updated, call updateDistributionProperties() to accomplish this task
	 */
	@Override
	public void update(ExampleSet exampleSet) {
		Attribute weightAttribute = exampleSet.getAttributes().getWeight();
		for (Example example : exampleSet) {
			double weight = weightAttribute == null ? 1.0d : example.getWeight();
			totalWeight += weight;
			double labelValue = example.getLabel();
			if (!Double.isNaN(labelValue)) {
				int classIndex = (int) example.getLabel();
				classWeights[classIndex] += weight;
				int attributeIndex = 0;
				for (Attribute attribute : exampleSet.getAttributes()) {
					double attributeValue = example.getValue(attribute);
					if (nominal[attributeIndex]) {
						if (!Double.isNaN(attributeValue)) {
							if ((int) attributeValue < weightSums[attributeIndex][classIndex].length - 1) {
								weightSums[attributeIndex][classIndex][(int) attributeValue] += weight;								
							} else {
								// extend weight array if attribute value is not in mapping
								for (int i = 0; i < numberOfClasses; i++) {
									double[] newWeightSums = new double[(int) attributeValue + 2];
									newWeightSums[newWeightSums.length - 1] = weightSums[attributeIndex][i][weightSums[attributeIndex][i].length - 1];
									for (int j = 0; j < weightSums[attributeIndex][i].length - 1; j++) {
										newWeightSums[j] = weightSums[attributeIndex][i][j];
									}
									weightSums[attributeIndex][i] = newWeightSums;
									distributionProperties[attributeIndex][i] = new double[(int) attributeValue + 2];
								}
								weightSums[attributeIndex][classIndex][(int) attributeValue] += weight;
								// recreate internal attribute value mapping
								attributeValues[attributeIndex] = new String[(int) attributeValue + 2];
								for (int i = 0; i < attributeValues[attributeIndex].length - 1; i++) {									
									attributeValues[attributeIndex][i] = attribute.getMapping().mapIndex(i);
								}
								attributeValues[attributeIndex][attributeValues[attributeIndex].length - 1] = UNKNOWN_VALUE_NAME;
							}
						} else {
							weightSums[attributeIndex][classIndex][weightSums[attributeIndex][classIndex].length - 1] += weight;
						}
					} else {
						if (!Double.isNaN(attributeValue)) {
							weightSums[attributeIndex][classIndex][INDEX_COUNT] += 1d;
							weightSums[attributeIndex][classIndex][INDEX_VALUE_SUM] += weight * attributeValue;
							weightSums[attributeIndex][classIndex][INDEX_SQUARED_VALUE_SUM]  += weight * attributeValue * attributeValue;
						} else {
							weightSums[attributeIndex][classIndex][INDEX_MISSING_WEIGHTS] += weight;						
						}
					}
					attributeIndex++;
				}
			}
		}
		modelRecentlyUpdated = true;
	}

	/**
	 * Updates the distribution properties by calculating the logged probabilities
	 * and distribution parameters on the basis of the weight counters. 
	 */
	private void updateDistributionProperties() {
		double f = laplaceCorrectionEnabled ? 1 / totalWeight : Double.MIN_VALUE;
		double logFactorCoefficient = Math.sqrt(2 * Math.PI);
		for (int i = 0; i < numberOfClasses; i++) {
			priors[i] = Math.log(classWeights[i] / totalWeight);
		}
		for (int i = 0; i < numberOfAttributes; i++) {
			if (nominal[i]) {
				for (int j = 0; j < numberOfClasses; j++) {
					for (int k = 0; k < weightSums[i][j].length; k++) {
						distributionProperties[i][j][k] = Math.log((weightSums[i][j][k] + f) / (classWeights[j] + f * weightSums[i][j].length)); 
					}					
				}
			} else {
				for (int j = 0; j < numberOfClasses; j++) {
					double classWeight = classWeights[j] - weightSums[i][j][INDEX_MISSING_WEIGHTS];
					distributionProperties[i][j][INDEX_MEAN] = weightSums[i][j][INDEX_VALUE_SUM] / classWeight;
					double standardDeviation = Math.sqrt((weightSums[i][j][INDEX_SQUARED_VALUE_SUM] - weightSums[i][j][INDEX_VALUE_SUM] * weightSums[i][j][INDEX_VALUE_SUM] / classWeight) / (((weightSums[i][j][INDEX_COUNT] - 1) / weightSums[i][j][INDEX_COUNT]) * classWeight));
					if (Double.isNaN(standardDeviation) || standardDeviation <= 1e-3) {
						standardDeviation = 1e-3;
					}
					distributionProperties[i][j][INDEX_STANDARD_DEVIATION] = standardDeviation;
					distributionProperties[i][j][INDEX_LOG_FACTOR] = Math.log(distributionProperties[i][j][INDEX_STANDARD_DEVIATION] * logFactorCoefficient);
				}
			}
		}
		modelRecentlyUpdated = false;
	}

	/**
	 * Perform predictions based on the distribution properties.
	 */
	public ExampleSet performPredictionOld(ExampleSet exampleSet, Attribute predictedLabel) {
		if (modelRecentlyUpdated) {
			updateDistributionProperties();
		}
		for (Example example : exampleSet) {
			double[] probabilities = new double[numberOfClasses];
			double maxLogProbability = Double.NEGATIVE_INFINITY;
			double probabilitySum = 0;
			int mostProbableClass = 0;
			for (int i = 0; i < numberOfClasses; i++) {
				double logProbability = priors[i];
				if (!Double.isNaN(logProbability)) {
					int j = 0;
					for (Attribute attribute : exampleSet.getAttributes()) {
						double value = example.getValue(attribute);
						if (nominal[j]) {
							if (!Double.isNaN(value) && (int) value < distributionProperties[j][i].length) {
								logProbability += distributionProperties[j][i][(int) value];
							} else {
								logProbability += distributionProperties[j][i][distributionProperties[j][i].length - 1];
							}
						} else {
							if (!Double.isNaN(value)) {
								double base = (value - distributionProperties[j][i][INDEX_MEAN]) / distributionProperties[j][i][INDEX_STANDARD_DEVIATION];
								logProbability -= distributionProperties[j][i][INDEX_LOG_FACTOR] + 0.5 * (base * base);
							}
						}
						j++;
					}
					if (logProbability > maxLogProbability) { 
						maxLogProbability = logProbability;
						mostProbableClass = i;
					}
					probabilities[i] = logProbability;
				} else {
					probabilities[i] = Double.NaN;
				}
			}
			for (int i = 0; i < numberOfClasses; i++) {
				if (!Double.isNaN(probabilities[i])) {
					probabilities[i] = Math.exp(probabilities[i] - maxLogProbability);
					probabilitySum += probabilities[i];
				} else {
					probabilities[i] = 0;
				}
			}

			if (maxLogProbability == Double.NEGATIVE_INFINITY) {
				example.setPredictedLabel(Double.NaN);
				for (int i = 0; i < numberOfClasses; i++) {
					example.setConfidence(classValues[i], Double.NaN);
				}
			} else {
				example.setPredictedLabel(mostProbableClass);
				for (int i = 0; i < numberOfClasses; i++) {
					example.setConfidence(classValues[i], probabilities[i] / probabilitySum);
				}
			}
		}
		return exampleSet;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) {
		if (modelRecentlyUpdated) {
			updateDistributionProperties();
		}
		double[] probabilities = new double[numberOfClasses];
		for (Example example : exampleSet) {
			double maxLogProbability = Double.NEGATIVE_INFINITY;
			double probabilitySum = 0;
			int mostProbableClass = 0;
			int j = 0;
			for (int i = 0; i < numberOfClasses; i++) {
				probabilities[i] = priors[i];
			}
			for (Attribute attribute : exampleSet.getAttributes()) {
				double value = example.getValue(attribute);
				if (nominal[j]) {
					if (!Double.isNaN(value)) {
						int intValue = (int) value;
						for (int i = 0; i < numberOfClasses; i++) {
							if (intValue < distributionProperties[j][i].length) {
								probabilities[i] += distributionProperties[j][i][intValue];
							}
						}
					} else {
						for (int i = 0; i < numberOfClasses; i++) {
							probabilities[i] += distributionProperties[j][i][distributionProperties[j][i].length - 1];
						}
					}
				} else {
					if (!Double.isNaN(value)) {
						for (int i = 0; i < numberOfClasses; i++) {
							double base = (value - distributionProperties[j][i][INDEX_MEAN]) / distributionProperties[j][i][INDEX_STANDARD_DEVIATION];
							probabilities[i] -= distributionProperties[j][i][INDEX_LOG_FACTOR] + 0.5 * base * base;
						}
					}
				}
				j++;
			}
			for (int i = 0; i < numberOfClasses; i++) {
				if (!Double.isNaN(probabilities[i]) && probabilities[i] > maxLogProbability) { 
					maxLogProbability = probabilities[i];
					mostProbableClass = i;
				}
			}
			for (int i = 0; i < numberOfClasses; i++) {
				if (!Double.isNaN(probabilities[i])) {
					probabilities[i] = Math.exp(probabilities[i] - maxLogProbability);
					probabilitySum += probabilities[i];
				} else {
					probabilities[i] = 0;
				}
			}

			if (maxLogProbability == Double.NEGATIVE_INFINITY) {
				example.setPredictedLabel(Double.NaN);
				for (int i = 0; i < numberOfClasses; i++) {
					example.setConfidence(classValues[i], Double.NaN);
				}
			} else {
				example.setPredictedLabel(mostProbableClass);
				for (int i = 0; i < numberOfClasses; i++) {
					example.setConfidence(classValues[i], probabilities[i] / probabilitySum);
				}
			}
		}
		return exampleSet;
	}

	public void setLaplaceCorrectionEnabled(boolean laplaceCorrectionEnabled) {
		this.laplaceCorrectionEnabled = laplaceCorrectionEnabled;
	}

	public boolean getLaplaceCorrectionEnabled() {
		return laplaceCorrectionEnabled;
	}

	@Override
	public double getLowerBound(int attributeIndex) {
		if (!nominal[attributeIndex]) {
			double lowerBound = Double.POSITIVE_INFINITY;
			for (int i = 0; i < numberOfClasses; i++) {
				double currentLowerBound = NormalDistribution.getLowerBound(distributionProperties[attributeIndex][i][INDEX_MEAN],
						distributionProperties[attributeIndex][i][INDEX_STANDARD_DEVIATION]);
				if (!Double.isNaN(currentLowerBound)) {
					lowerBound = Math.min(lowerBound, currentLowerBound);
				}
			}
			return lowerBound;
		} else {
			return Double.NaN;
		}
	}

	@Override
	public double getUpperBound(int attributeIndex) {
		if (!nominal[attributeIndex]) {
			double upperBound = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < numberOfClasses; i++) {
				double currentUpperBound = NormalDistribution.getUpperBound(distributionProperties[attributeIndex][i][INDEX_MEAN],
						distributionProperties[attributeIndex][i][INDEX_STANDARD_DEVIATION]);
				if (!Double.isNaN(currentUpperBound)) {
					upperBound = Math.max(upperBound, currentUpperBound);
				}
			}
			return upperBound;
		} else {
			return Double.NaN;
		}
	}

	@Override
	public boolean isDiscrete(int attributeIndex) {
		if (attributeIndex>= 0 && attributeIndex < nominal.length) {
			return nominal[attributeIndex];
		}
		return false;
	}

	@Override
	public Collection<Integer> getClassIndices() {
		Collection<Integer> classValueIndices = new ArrayList<Integer>(numberOfClasses);
		for (int i = 0; i < numberOfClasses; i++) {
			classValueIndices.add(i);
		}
		return classValueIndices;
	}

	@Override
	public int getNumberOfClasses() {
		return numberOfClasses;
	}

	@Override
	public String getClassName(int index) {
		return classValues[index];
	}

	@Override
	public Distribution getDistribution(int classIndex, int attributeIndex) {
		if (nominal[attributeIndex]) {
			double[] probabilities = new double[distributionProperties[attributeIndex][classIndex].length];
			for (int i = 0; i < probabilities.length; i++) {
				probabilities[i] = Math.exp(distributionProperties[attributeIndex][classIndex][i]);
			}
			return new DiscreteDistribution(attributeNames[attributeIndex], probabilities, attributeValues[attributeIndex]);
		} else {
			return new NormalDistribution(distributionProperties[attributeIndex][classIndex][INDEX_MEAN],
					distributionProperties[attributeIndex][classIndex][INDEX_STANDARD_DEVIATION]);
		}
	}

	@Override
	public String toString() {
		if (modelRecentlyUpdated) {
			updateDistributionProperties();
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append("Distribution model for label attribute " + className);
		buffer.append(Tools.getLineSeparators(2));
		for (int i = 0; i < numberOfClasses; i++) {
			String classTitle = "Class " + classValues[i] + " (" + Tools.formatNumber(Math.exp(priors[i])) + ")"; 
			buffer.append(Tools.getLineSeparator());
			buffer.append(classTitle);
			buffer.append(Tools.getLineSeparator());
			buffer.append(attributeNames.length + " distributions");
			buffer.append(Tools.getLineSeparator());
		}
		return buffer.toString();
	}
}
