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
package com.rapidminer.parameter;

/**
 * A parameter type for double values. Operators ask for the double value with
 * {@link com.rapidminer.operator.Operator#getParameterAsDouble(String)}. For
 * infinite ranges Double.POSITIVE_INFINITY and Double.NEGATIVE_INFINITY should
 * be used.
 * 
 * @author Ingo Mierswa, Simon Fischer
 *          Exp $
 */
public class ParameterTypeDouble extends ParameterTypeNumber {

	private static final long serialVersionUID = 2455026868706964187L;

	private double defaultValue = Double.NaN;

	private double min = Double.NEGATIVE_INFINITY;

	private double max = Double.POSITIVE_INFINITY;

	private boolean noDefault = true;

	private boolean optional = true;

	public ParameterTypeDouble(String key, String description, double min, double max) {
		this(key, description, min, max, Double.NaN);
		this.noDefault = true;
		this.optional = false;
	}

	public ParameterTypeDouble(String key, String description, double min, double max, boolean optional) {
		this(key, description, min, max, Double.NaN);
		this.noDefault = true;
		this.optional = optional;
		if (!optional)
			setExpert(false);

	}

	public ParameterTypeDouble(String key, String description, double min, double max, double defaultValue) {
		super(key, description);
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
		this.optional = true;
		setExpert(false);
	}


	public ParameterTypeDouble(String key, String description, double min, double max, double defaultValue, boolean expert) {
		super(key, description);
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
		this.optional = true;
		setExpert(expert);
	}
	@Override
	public double getMinValue() {
		return min;
	}

	@Override
	public double getMaxValue() {
		return max;
	}

	@Override
	public boolean isOptional() {
		return super.isOptional() && optional;
	}

	@Override
	public Object getDefaultValue() {
		if (Double.isNaN(defaultValue)) {
			return null;
		} else {
			return Double.valueOf(defaultValue);
		}
	}

	@Override
	public void setDefaultValue(Object object) {
		this.defaultValue = (Double)object;
	}

	/** Returns true. */
	@Override
	public boolean isNumerical() { return true; }

	@Override
	public String getRange() {
		String range = "real; ";
		if (min == Double.NEGATIVE_INFINITY)
			range += "-\u221E";
		else
			range += min;
		range += "-";
		if (max == Double.POSITIVE_INFINITY)
			range += "+\u221E";
		else
			range += max;
		if (!noDefault)
			range += "; default: " + defaultValue;
		return range;
	}
}
