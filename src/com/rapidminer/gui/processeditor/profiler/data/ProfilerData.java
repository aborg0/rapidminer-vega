/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
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
package com.rapidminer.gui.processeditor.profiler.data;

/**
 * Stores the profiler data for an individual operator.
 * 
 * @author Marco Boeck
 */
public class ProfilerData {
	
	/** number of times operator has run */
	private long runCount;
	
	/** average amount of time operator used during execution (real time) */
	private double averageRunTimeReal;
	
	/** total amount of time in milliseconds operator used during execution. Needed to calculate average time (real time) */
	private double totalRunTimeReal;
	
	/** amount of time operator used during last execution (real time) in milliseconds */
	private double lastRunTimeReal;
	
	/** average amount of time operator used during execution (cpu time) */
	private double averageRunTimeCpu;
	
	/** total amount of time in nanoseconds operator used during execution. Needed to calculate average time (cpu time) */
	private double totalRunTimeCpu;
	
	/** amount of time operator used during last execution (cpu time) in nanoseconds */
	private double lastRunTimeCpu;
	
	
	/**
	 * Standard constructor, sets all values to 0.
	 */
	public ProfilerData() {
		runCount = 0;
		totalRunTimeReal = 0;
		averageRunTimeReal = 0;
		lastRunTimeReal = 0;
		averageRunTimeCpu = 0;
		totalRunTimeCpu = 0;
		lastRunTimeCpu = 0;
	}
	
	/**
	 * Returns true if the operator has been running at least once.
	 * @return true if the operator has been running at least once; false otherwise
	 */
	public boolean hasRun() {
		return (runCount > 0);
	}
	
	/**
	 * Increases runcount by one.
	 */
	public synchronized void incrementRunCount() {
		runCount++;
	}
	
	/**
	 * Gets the number of times the operator has run.
	 * @return the number of times the operator has run
	 */
	public long getRunCount() {
		return runCount;
	}
	
	/**
	 * Gets the average execution time of the operator (real time).
	 * @return the average execution time of operator in ms (real time)
	 */
	public double getAverageRunTimeReal() {
		return averageRunTimeReal;
	}
	
	/**
	 * Gets the average execution time of the operator in seconds (real time).
	 * @return the average execution time of operator in seconds (real time)
	 */
	public double getAverageRunTimeRealInSeconds() {
		double value = averageRunTimeReal;
		value /= 1000;
		return value;
	}
	
	/**
	 * Gets the average execution time of the operator (cpu time).
	 * @return the average execution time of operator in ns (cpu time)
	 */
	public double getAverageRunTimeCpu() {
		return averageRunTimeCpu;
	}
	
	/**
	 * Gets the average execution time of the operator in seconds (cpu time).
	 * @return the average execution time of operator in seconds (cpu time)
	 */
	public double getAverageRunTimeCpuInSeconds() {
		double value = averageRunTimeCpu;
		value /= 1000000000;
		return value;
	}
	
	/**
	 * Sets the amount of time the operator used during last execution. Also updates the average time. Both
	 * use real time.
	 * @param time the amount of time operator used during last execution in ms (real time)
	 */
	public synchronized void setLastRunTimeReal(long time) {
		if (time < 0) {
			throw new IllegalArgumentException("time must not be negative!");
		}
		lastRunTimeReal = time;
		totalRunTimeReal += time;
		averageRunTimeReal = totalRunTimeReal/runCount;
	}
	
	/**
	 * Sets the amount of time the operator used during last execution. Also updates the average time. Both
	 * use cpu time.
	 * @param time the amount of time operator used during last execution in ns (cpu time)
	 */
	public synchronized void setLastRunTimeCpu(long time) {
		if (time < 0) {
			throw new IllegalArgumentException("time must not be negative!");
		}
		lastRunTimeCpu = time;
		totalRunTimeCpu += time;
		averageRunTimeCpu = totalRunTimeCpu/runCount;
	}
	
	/**
	 * Gets the last execution time of the operator (real time).
	 * @return the last execution time of operator in ms (real time)
	 */
	public synchronized double getLastRunTimeReal() {
		return lastRunTimeReal;
	}
	
	/**
	 * Gets the last execution time of the operator in seconds (real time).
	 * @return the last execution time of operator in seconds (real time)
	 */
	public double getLastRunTimeRealInSeconds() {
		double value = lastRunTimeReal;
		value /= 1000;
		return value;
	}
	
	/**
	 * Gets the last execution time of the operator (cpu time).
	 * @return the last execution time of operator in ns (cpu time)
	 */
	public synchronized double getLastRunTimeCpu() {
		return lastRunTimeCpu;
	}
	
	/**
	 * Gets the last execution time of the operator in seconds (cpu time).
	 * @return the last execution time of operator in seconds (cpu time)
	 */
	public double getLastRunTimeCpuInSeconds() {
		double value = lastRunTimeCpu;
		value /= 1000000000;
		return value;
	}
	
	/**
	 * Gets the total execution time of the operator in NANOSECONDS (cpu time).
	 * @return total execution time of the operator in NANOSECONDS (cpu time)
	 */
	public double getTotalRunTimeCPU() {
		return totalRunTimeCpu;
	}
	
	/**
	 * Gets the total execution time of the operator in MILLISECONDS (real time).
	 * @return total execution time of the operator in MILLISECONDS (real time)
	 */
	public double getTotalRunTimeReal() {
		return totalRunTimeReal;
	}

}
