package com.rapidminer.operator.annotation;

import java.text.DecimalFormat;
import java.text.NumberFormat;

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
	
	@Override
	public String toString() {
		if (coefficient == 0.0f || coefficient == -0.0f) {
			return "n/a";
		}
		NumberFormat formatter = new DecimalFormat("#.##"); 
		StringBuffer resourceString = new StringBuffer();
		resourceString.append("f() = ");
		resourceString.append(formatter.format(coefficient));
		if (degreeExamples > 0 || degreeAttributes > 0) {
			resourceString.append(" * (");
		}
		if (degreeExamples > 0) {
			if (logDegreeExamples>0) {
				resourceString.append("log");
				resourceString.append(formatter.format(logDegreeExamples));
				if (degreeExamples > 1) {
					resourceString.append("(examples^");
					resourceString.append(formatter.format(degreeExamples));
				} else {
					resourceString.append("(examples");
				}
				resourceString.append(')');
			} else {
				if (degreeExamples > 1) {
					resourceString.append("examples^");
					resourceString.append(formatter.format(degreeExamples));
				} else {
					resourceString.append("examples");
				}
			}
			if (degreeAttributes > 0) {
				resourceString.append(" * ");
			}
		}
		if (degreeAttributes > 0) {
			if (logDegreeAttributes>0) {
				resourceString.append("log");
				resourceString.append(formatter.format(logDegreeAttributes));
				if (degreeAttributes > 1) {
					resourceString.append("(attributes^");
					resourceString.append(formatter.format(degreeAttributes));
				} else {
					resourceString.append("(attributes");
				}
				resourceString.append(')');
			} else {
				if (degreeAttributes > 1) {
					resourceString.append("attributes^");
					resourceString.append(formatter.format(degreeAttributes));
				} else {
					resourceString.append("attributes");
				}
			}
		}
		if (degreeExamples > 0 || degreeAttributes > 0) {
			resourceString.append(')');
		}
		return resourceString.toString();
	}
}