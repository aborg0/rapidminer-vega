package com.rapidminer.operator.annotation;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;

/** Computes resource consumption based on an example set taken from a
 *  given port.
 * 
 * @author Simon Fischer
 *
 */
public abstract class ExampleSetResourceConsumptionEstimator implements ResourceConsumptionEstimator {

	private InputPort inputPort;


	public ExampleSetResourceConsumptionEstimator(InputPort inputPort) {
		super();
		this.inputPort = inputPort;
	}

	public abstract long estimateMemory(ExampleSetMetaData exampleSet);
	public abstract long estimateRuntime(ExampleSetMetaData exampleSet);	

	@Override
	public long estimateMemory() {
		final ExampleSetMetaData exampleSet = getExampleSet();
		if (exampleSet == null) {
			return -1;
		} else {
			return estimateMemory(exampleSet);
		}
	}

	@Override
	public long estimateRuntime() {
		final ExampleSetMetaData exampleSet = getExampleSet();
		if (exampleSet == null) {
			return -1;
		} else {
			return estimateRuntime(exampleSet);
		}
	}

	private ExampleSetMetaData getExampleSet() {
		final MetaData md = inputPort.getMetaData();
		if (md instanceof ExampleSetMetaData) {
			return (ExampleSetMetaData) md;
		} else {
			return null;
		}
	}
}
