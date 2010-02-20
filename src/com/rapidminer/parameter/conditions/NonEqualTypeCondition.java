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
import com.rapidminer.parameter.UndefinedParameterError;

/**
 * This condition checks if a type parameter (category) has NOT a certain value.
 * 
 * @author Ingo Mierswa
 */
public class NonEqualTypeCondition extends ParameterCondition {

	private int[] types;
	private String[] options;

	public NonEqualTypeCondition(ParameterHandler handler, String conditionParameter, String[] options, boolean becomeMandatory, int... types) {
		super(handler, conditionParameter, becomeMandatory);
		this.types = types;
		this.options = options;
	}

	@Override
	public boolean isConditionFullfilled() {
		boolean equals = false;
		int isType;
		try {
			isType = parameterHandler.getParameterAsInt(conditionParameter);
		} catch (UndefinedParameterError e) {
			return false;
		} 
		for (int type : types) {
			if (isType == type) {
				equals = true;
				break;
			}
		}
		return !equals;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (types.length > 1) {
			builder.append(conditionParameter.replace('_',' ') + " \u2284 {");
			for (int i = 0; i < types.length; i++) {
				builder.append(options[types[i]]);
				if (i + 1 < types.length)
					builder.append(", ");
			}
			builder.append("}");
		} else
			if (types.length > 0)
				builder.append(conditionParameter.replace('_',' ') + " \u2260 " + options[types[0]]);
		return builder.toString();
	}
}
