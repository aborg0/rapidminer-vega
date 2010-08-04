package com.rapidminer.operator.annotation;

import com.rapidminer.operator.Operator;

/** Estimates the resource consumption (CPU time and memory usage) 
 *  of an {@link Operator} based on its current input. The methods
 *  in this interface do not take any arguments. Instead, they are
 *  backed by an operator and consider its current input.
 *  
 * @author Simon Fischer
 *
 */
public interface ResourceConsumptionEstimator {

	/** Returns the estimated number of CPU-cycles. 
	 *  If, for any reason, computation is impossible, -1 should be returned. */
	public long estimateRuntime();
	
	/** Returns the estimated number of bytes required when executing this operator.
	*  If, for any reason, computation is impossible, -1 should be returned. */
	public long estimateMemoryConsumption();
	
}
