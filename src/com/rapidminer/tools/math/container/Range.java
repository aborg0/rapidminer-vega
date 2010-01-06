/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
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
package com.rapidminer.tools.math.container;

import java.io.Serializable;

import com.rapidminer.tools.Tools;
/**
 * @author Sebastian Land
 */
public class Range implements Serializable {

	private static final long serialVersionUID = 1L;

	private double lower;
	private double upper;

	public Range() {
		this.lower = Double.NaN;
		this.upper = Double.NaN;
	}

	public Range(double start, double end) {
		this.lower = start;
		this.upper = end;
	}

	public Range(Range valueRange) {
		this.lower = valueRange.getLower();
		this.upper = valueRange.getUpper();
	}

	/**
	 * This method increases the range size, if the value is not lying in between
	 */
	public void add(double value) {
		if (value < lower || Double.isNaN(lower))
			lower = value;
		if (value > upper || Double.isNaN(upper))
			upper = value;
	}

	public void union(Range range) {
		add(range.getLower());
		add(range.getUpper());
	}

	public boolean contains(double value) {
		return value > lower && value < upper;
	}
	
	@Override
	public String toString() {
		return "[" + Tools.formatIntegerIfPossible(lower) + " \u2013 " + Tools.formatIntegerIfPossible(upper) + "]";
	}

	public double getUpper() {
		return upper;
	}

	public double getLower() {
		return lower;
	}
	
	@Override
	public boolean equals(Object range) {
		if (range instanceof Range) {
			Range other = (Range) range;
			return upper == other.upper && lower == other.lower;
		}
		return false;
	}

	public boolean contains(Range range) {
		return (this.lower <= range.lower && this.upper >= range.upper);
	}

}
