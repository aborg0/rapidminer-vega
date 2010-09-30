package com.rapidminer.operator.annotation;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.tools.AttributeSubsetSelector;

/** Evaluates resource consumption based on a simple polynomial function. 
 * 
 * @author Simon Fischer
 *
 */
public class PolynomialExampleSetResourceConsumptionEstimator extends ExampleSetResourceConsumptionEstimator {

	private final PolynomialFunction cpuFunction;
	private final PolynomialFunction memoryFunction;

	public PolynomialExampleSetResourceConsumptionEstimator(InputPort in, AttributeSubsetSelector selector,
			PolynomialFunction cpuFunction, PolynomialFunction memoryFunction) {
		super(in, selector);
		this.cpuFunction = cpuFunction;
		this.memoryFunction = memoryFunction;
	}

	protected int getNumberOfRelevantAttributes(ExampleSetMetaData emd) {
		return emd.getNumberOfRegularAttributes();	
	}
	
	@Override
	public long estimateMemory(ExampleSetMetaData exampleSet) {
		final MDInteger numEx = exampleSet.getNumberOfExamples();
		if (numEx == null) {
			return -1;
		} else if (numEx.getNumber() == 0) {
			return -1;
		}
		final int numAtt = getNumberOfRelevantAttributes(exampleSet);
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
	
	@Override
	public PolynomialFunction getCpuFunction() {
		return cpuFunction;
	}
	
	@Override
	public PolynomialFunction getMemoryFunction() {
		return memoryFunction;
	}
}
