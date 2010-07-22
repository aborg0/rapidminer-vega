package com.rapidminer.operator.annotation;

/** Only highest order terms taken into account. Functions can be of the form
 * 
 *    c * log(n)^d1 * n^d2 * log(m)^*d3 * m^d4 
 */
public class PolynomialFunction {
		 
	private double coefficient;
	private double degreeExamples;
	private double degreeAttributes;
	private double logDegreeExamples;
	private double logDegreeAttributes;
		
	public PolynomialFunction(double coefficient, double degreeExamples, double degreeAttributes) {
		this(coefficient, degreeExamples, 0, degreeAttributes, 0);
	}
	
	public PolynomialFunction(double coefficient, 
			double degreeExamples, double logDegreeExamples,
			double degreeAttributes, double logDegreeAttributes) {
		super();
		this.coefficient = coefficient;
		this.degreeAttributes = degreeAttributes;
		this.degreeExamples = degreeExamples;
		this.logDegreeAttributes = logDegreeAttributes;
		this.logDegreeExamples = logDegreeExamples;
	}

	public static PolynomialFunction makeLinearFunction(double coefficient) {
		return new PolynomialFunction(coefficient, 1, 1);
	}
	
	public long evaluate(int numExamples, int numAttributes) {
		return (long) (coefficient * 
				Math.pow(numExamples, degreeExamples) *
				Math.pow(Math.log(numExamples), logDegreeExamples) *
				Math.pow(numAttributes, degreeAttributes) *
				Math.pow(Math.log(numAttributes), logDegreeAttributes));
	}
}