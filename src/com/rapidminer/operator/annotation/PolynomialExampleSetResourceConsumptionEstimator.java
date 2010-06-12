package com.rapidminer.operator.annotation;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;

/** Evaluates resource consumption based on a simple polynomial function. 
 * 
 * @author Simon Fischer
 *
 */
public class PolynomialExampleSetResourceConsumptionEstimator extends ExampleSetResourceConsumptionEstimator {

	/** Only highest order terms taken into account. Functions can be of the form
	 * 
	 *    c * log(n)^d1 * n^d2 * log(m)^*d3 * m^d4 
	 */
	public static class PolynomialFunction {
		private double coefficient;
		private double degreeExamples;
		private double degreeAttributes;
		private double logDegreeExamples;
		private double logDegreeAttributes;
		
		private PolynomialFunction(double coefficient, double degreeExamples, double degreeAttributes) {
			this(coefficient, degreeExamples, 0, degreeAttributes, 0);
		}
		
		private PolynomialFunction(double coefficient, 
				double degreeExamples, double logDegreeExamples,
				double degreeAttributes, double logDegreeAttributes) {
			super();
			this.coefficient = coefficient;
			this.degreeAttributes = degreeAttributes;
			this.degreeExamples = degreeExamples;
			this.logDegreeAttributes = logDegreeAttributes;
			this.logDegreeExamples = logDegreeExamples;
		}

		public long evaluate(int numExamples, int numAttributes) {
			return (long) (coefficient * 
					Math.pow(numExamples, degreeExamples) *
					Math.pow(Math.log(numExamples), logDegreeExamples) *
					Math.pow(numAttributes, degreeAttributes) *
					Math.pow(Math.log(numAttributes), logDegreeAttributes));
		}
	}
	
	private final PolynomialFunction cpuFunction;
	private final PolynomialFunction memoryFunction;
	
	private PolynomialExampleSetResourceConsumptionEstimator(InputPort in, PolynomialFunction cpuFunction, PolynomialFunction memoryFunction) {
		super(in);
		this.cpuFunction = cpuFunction;
		this.memoryFunction = memoryFunction;
	}

	@Override
	public long estimateMemory(ExampleSetMetaData exampleSet) {
		final MDInteger numEx = exampleSet.getNumberOfExamples();
		if (numEx == null) {
			return -1;
		} else if (numEx.getNumber() == 0) {
			return -1;
		}
		final int numAtt = exampleSet.getNumberOfRegularAttributes();
		return cpuFunction.evaluate(numEx.getNumber(), numAtt);
	}

	@Override
	public long estimateRuntime(ExampleSetMetaData exampleSet) {
		final MDInteger numEx = exampleSet.getNumberOfExamples();
		if (numEx == null) {
			return -1;
		} else if (numEx.getNumber() == 0) {
			return -1;
		}
		final int numAtt = exampleSet.getNumberOfRegularAttributes();
		return memoryFunction.evaluate(numEx.getNumber(), numAtt);
	}		
}
