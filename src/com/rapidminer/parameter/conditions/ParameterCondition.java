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
package com.rapidminer.parameter.conditions;

import com.rapidminer.parameter.ParameterHandler;

/**
 * The ParameterCondition interface can be used to define dependencies 
 * for parameter types, e.g. to show certain parameters only in cases
 * where another parameter has a specified value.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public abstract class ParameterCondition {

	protected ParameterHandler parameterHandler;

	protected String conditionParameter;

	protected boolean becomeMandatory;

	public ParameterCondition(ParameterHandler parameterHandler, String conditionParameter, boolean becomeMandatory) {
		this.parameterHandler = parameterHandler;
		this.conditionParameter = conditionParameter;
		this.becomeMandatory = becomeMandatory;
	}

	/**
	 * This returns true if the condition is met and if the ancestor type isn't hidden.
	 */
	final public boolean dependencyMet() {

		if (parameterHandler.getParameters().getParameterType(conditionParameter).isHidden())
			return false;
		return isConditionFullfilled();
	}

	/**
	 * Subclasses have to implement this method in order to return if the condition is fulfilled.
	 */
	public abstract boolean isConditionFullfilled();

	public boolean becomeMandatory() {
		return becomeMandatory;
	}
}
