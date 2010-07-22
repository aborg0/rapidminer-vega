package com.rapidminer.operator.annotation;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.AttributeSubsetSelector;

/** Computes resource consumption based on an example set taken from a
 *  given port.
 * 
 * @author Simon Fischer
 *
 */
public abstract class ExampleSetResourceConsumptionEstimator implements ResourceConsumptionEstimator {

	private InputPort inputPort;

	private AttributeSubsetSelector selector;


	public ExampleSetResourceConsumptionEstimator(InputPort inputPort, AttributeSubsetSelector selector) {
		super();
		this.inputPort = inputPort;
		this.selector = selector;
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

	protected ExampleSetMetaData getExampleSet() {
		final MetaData md = inputPort.getMetaData();
		if (md instanceof ExampleSetMetaData) {
			if (selector != null) {
				return selector.getMetaDataSubset((ExampleSetMetaData) md, false);
			} else {
				return (ExampleSetMetaData) md;
			}
		} else {
			return null;
		}
	}
}
