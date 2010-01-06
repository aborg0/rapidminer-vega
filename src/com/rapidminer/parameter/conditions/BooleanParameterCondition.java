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
package com.rapidminer.parameter.conditions;

import com.rapidminer.parameter.ParameterHandler;

/**
 * This condition checks if a boolean parameter has a certain value.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class BooleanParameterCondition extends ParameterCondition {

	private boolean conditionValue;

	public BooleanParameterCondition(ParameterHandler parameterHandler, String conditionParameter, boolean becomeMandatory, boolean conditionValue) {
		super(parameterHandler, conditionParameter, becomeMandatory);
		this.conditionValue = conditionValue;
	}

	@Override
	public boolean isConditionFullfilled() {
		return (parameterHandler.getParameterAsBoolean(conditionParameter) == conditionValue);
	}	

	@Override
	public String toString() {
		return conditionParameter.replace('_',' ') + " = " + conditionValue;
	}
}
