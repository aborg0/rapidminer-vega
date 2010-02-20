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
package com.rapidminer.tools.math.function.aggregation;

import java.util.Map;
import java.util.TreeMap;

import com.rapidminer.example.Attribute;


/**
 * Calculates the (weighted) median of some values.
 * 
 * @author Tobias Malbrecht
 *
 */
public class MedianFunction extends AbstractAggregationFunction {
	
	private TreeMap<Double, Double> valueWeightMap;
	
	private double totalWeight;
	
	public MedianFunction() {
		this(DEFAULT_IGNORE_MISSINGS);
	}
	
	public MedianFunction(Boolean ignoreMissings) {
		super(ignoreMissings);
		valueWeightMap = new TreeMap<Double, Double>();
	}
	
	public String getName() { return "median"; }
    
	@Override
	protected void reset() {
		foundMissing = false;
		totalWeight = 0;
		if (valueWeightMap != null) {
			valueWeightMap.clear();
		}
	}
	
	public void update(double value, double weight) {
		if (Double.isNaN(value)) {
			foundMissing = true;
			return;
		}
		Double totalValueWeight = valueWeightMap.get(value);
		if (totalValueWeight != null) {
			totalValueWeight += weight;
		} else {
			totalValueWeight = new Double(weight);
		}
		valueWeightMap.put(value, totalValueWeight);
		totalWeight += weight;
	}
	
	public void update(double value) {
		if (Double.isNaN(value)) {
			foundMissing = true;
			return;
		}
		Double totalValueWeight = valueWeightMap.get(value);
		if (totalValueWeight != null) {
			totalValueWeight++;
		} else {
			totalValueWeight = new Double(1);
		}
		valueWeightMap.put(value, totalValueWeight);
		totalWeight++;
	}
	
	public double getValue() {
		if (foundMissing && !ignoreMissings) {
			return Double.NaN;
		}
		double valueWeightSum = 0;
		double lastValue = Double.NaN;
		double lastWeight = Double.NaN;
		// TODO: check weighted median calculation
		for (Map.Entry<Double, Double> entry : valueWeightMap.entrySet()) {
			if (!Double.isNaN(lastValue) && !Double.isNaN(lastWeight)) {
				double thisWeight = entry.getValue().doubleValue();
				return (lastValue * lastWeight + entry.getKey().doubleValue() * thisWeight) / (lastWeight + thisWeight);
			}
			valueWeightSum += entry.getValue().doubleValue();
			if (valueWeightSum > totalWeight / 2) {
				return entry.getKey().doubleValue();
			}
			if (valueWeightSum == totalWeight / 2) {
				lastWeight = entry.getValue().doubleValue();
				lastValue = entry.getKey().doubleValue();
			}
		}
		return Double.NaN;
	}
	
    public boolean supportsAttribute(Attribute attribute) {
        return attribute.isNumerical();
    }    
}
