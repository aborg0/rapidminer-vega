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
package com.rapidminer.tools.math.similarity;

import java.io.Serializable;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.parameter.ParameterHandler;

/**
 * This interfaces defines the methods for all similarity measures. Classes implementing this 
 * interface are not allowed to have a constructor, instead should use the init method.
 * 
 * @author Sebastian Land
 */
public abstract class DistanceMeasure implements Serializable {

	private static final long serialVersionUID = 1290079829430640414L;

	/**
	 * Before using a similarity measure, it is needed to initialize. Subclasses might use initializing 
	 * for remembering the exampleset properties like attribute type or test if applicable to exampleSet at all.
	 * Please note that it might be necessary to also override the other init methods if this measure should make
	 * use of parameters or other IOObjects.
	 * 
	 * @param exampleSet the exampleset 
	 */
	public abstract void init(ExampleSet exampleSet) throws OperatorException;

	/**
	 * Before using a similarity measure, it is needed to initialize. Subclasses might use initializing 
	 * for remembering the exampleset properties like attribute type or test if applicable to exampleSet at all.
	 * This init method calls init(exampleSet) per default and ignores the parameterHandler and the ioContainer. Subclasses might use 
	 * the parameterHandler to evaluate parameter settings and the IOContainer to access other objects.
	 * 
	 * @param exampleSet the exampleset
	 * @param parameterHandler the handler to ask for parameter values 
	 */
	public void init(ExampleSet exampleSet, ParameterHandler parameterHandler) throws OperatorException {
		init(exampleSet);
	}


	/**
	 * This method does the calculation of the distance between two double arrays. The meanings of
	 * the double values might be remembered from the init method.
	 * @param value1
	 * @param value2
	 * @return the distance
	 */
	public abstract double calculateDistance(double[] value1, double[] value2);

	/**
	 * This method does the similarity of the distance between two double arrays. The meanings of
	 * the double values might be remembered from the init method.
	 * @param value1
	 * @param value2
	 * @return the distance
	 */
	public abstract double calculateSimilarity(double[] value1, double[] value2);

	/**
	 * This method returns a boolean whether this measure is a distance measure
	 * @return true if is distance
	 */
	public boolean isDistance() {
		return true;
	}

	/**
	 * This method returns a boolean whether this measure is a similarity measure
	 * @return true if is similarity
	 */
	public final boolean isSimilarity() {
		return !isDistance();
	}

	/**
	 * This is a convenient method for calculating the distance between examples.
	 * All attributes will be used to form a double array, used for the calculateDistance method. 
	 * @return the distance
	 */
	public double calculateDistance(Example firstExample, Example secondExample) {
		Attributes attributes = firstExample.getAttributes();
		double[] firstValues = new double[attributes.size()];
		double[] secondValues = new double[attributes.size()];

		int i = 0;
		for (Attribute attribute: attributes) {
			firstValues[i] = firstExample.getValue(attribute);
			secondValues[i] = secondExample.getValue(attribute);
			i++;
		}

		return calculateDistance(firstValues, secondValues);
	}

	/**
	 * This is a convenient method for calculating the distance between examples and double arrays.
	 * All attributes will be used to form a double array, used for the calculateDistance method. 
	 * @return the distance
	 */
	public final double calculateDistance(Example firstExample, double[] second) {
		Attributes attributes = firstExample.getAttributes();
		double[] firstValues = new double[attributes.size()];

		int i = 0;
		for (Attribute attribute: attributes) {
			firstValues[i] = firstExample.getValue(attribute);
			i++;
		}

		return calculateDistance(firstValues, second);
	}


	/**
	 * This is a convenient method for calculating the similarity between examples.
	 * All attributes will be used to form a double array, used for the calculateDistance method. 
	 * @return the distance
	 */
	public double calculateSimilarity(Example firstExample, Example secondExample) {
		Attributes attributes = firstExample.getAttributes();
		double[] firstValues = new double[attributes.size()];
		double[] secondValues = new double[attributes.size()];

		int i = 0;
		for (Attribute attribute: attributes) {
			firstValues[i] = firstExample.getValue(attribute);
			secondValues[i] = secondExample.getValue(attribute);
			i++;
		}

		return calculateSimilarity(firstValues, secondValues);
	}

	/**
	 * This is a convenient method for calculating the similarity between examples and a double array.
	 * All attributes will be used to form a double array, used for the calculateDistance method. 
	 * @return the distance
	 */
	public final double calculateSimilarity(Example firstExample, double[] second) {
		Attributes attributes = firstExample.getAttributes();
		double[] firstValues = new double[attributes.size()];

		int i = 0;
		for (Attribute attribute: attributes) {
			firstValues[i] = firstExample.getValue(attribute);
			i++;
		}

		return calculateSimilarity(firstValues, second);
	}

	/** If the computation of this distance measure depends on additional {@link IOObject}s,
	 *  this method can be overridden to install additional ports at the operator which uses
	 *  this distance measure. If this method is overridden, subclasses can make use of the data
	 *  received at the created ports in their {@link #init(ExampleSet, ParameterHandler)} method. 
	 *  <br/>
	 *  The default implementation does nothing.
	 * */
	public void installAdditionalPorts(InputPorts inputPorts, ParameterHandler parameterHandler) { }

	/** Undoes what {@link #installAdditionalPorts(InputPorts, ParameterHandler)} did. 
	 * @see #installAdditionalPorts(InputPorts, ParameterHandler) */
	public void uninstallAdditionalPorts(InputPorts inputPorts) { }
}
