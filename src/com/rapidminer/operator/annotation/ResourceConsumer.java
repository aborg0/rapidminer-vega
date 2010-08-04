package com.rapidminer.operator.annotation;

import com.rapidminer.operator.Operator;

/** An {@link Operator} that can estimate its resource consumption (CPU and memory)
 *  should implement this interface to describe its resource consumption.
 * 
 * @author Simon Fischer
 *
 */
public interface ResourceConsumer {

	public ResourceConsumptionEstimator getResourceConsumptionEstimator();
	
}
