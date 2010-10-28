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
package com.rapidminer.tools.math.function.date;

import java.util.Date;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Determines if the first Date is strictly later than the second Date.
 * 
 * @author Marco Boeck
 */
public class DateAfter extends PostfixMathCommand {
	
	public DateAfter() {
		numberOfParameters = 2;
	}
	
	/**
	 * Creates the boolean result.
	 * True if the first date is strictly later than the second date; false otherwise (includes same date).
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);
		
		Object dateObjectTwo = stack.pop();
		Object dateObjectOne = stack.pop();
		if (!(dateObjectOne instanceof Double) && !(dateObjectTwo instanceof Double)) {
			throw new ParseException("Invalid argument type for 'date_before', must both be a date (double)");
		}
		double dateDoubleOne = (Double)dateObjectOne;
		double dateDoubleTwo = (Double)dateObjectTwo;
		Date dateOne = new Date((long)dateDoubleOne);
		Date dateTwo = new Date((long)dateDoubleTwo);
		boolean result = dateOne.after(dateTwo);
		stack.push(result);
	}
}
