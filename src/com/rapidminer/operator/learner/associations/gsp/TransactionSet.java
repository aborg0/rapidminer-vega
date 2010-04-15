/**
 * 
 */
package com.rapidminer.operator.learner.associations.gsp;

/**
 * This class holds informations about the start and end time of a set of transactions.
 * Please note, that the set itself isn't saved, and isn't needed.
 * @author Sebastian Land
 */
public class TransactionSet {

	private double startTime = Double.POSITIVE_INFINITY;
	private double endTime = Double.NEGATIVE_INFINITY;
	
	public double getEndTime() {
		return endTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public void addTimeOfTransaction(double time) {
		if (time > this.endTime)
			this.endTime = time;
		if (time < this.startTime)
			this.startTime = time;
	}
	
	@Override
	public String toString() {
		return startTime + " - " + endTime;
	}

	public void reset() { 
		startTime = Double.POSITIVE_INFINITY;
		endTime = Double.NEGATIVE_INFINITY;
	}                         
}
