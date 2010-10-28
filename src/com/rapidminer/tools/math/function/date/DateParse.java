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

import java.text.DateFormat;
import java.util.Date;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Parses a Date object from either a long value or a string using the default locale.
 * 
 * @author Marco Boeck
 */
public class DateParse extends PostfixMathCommand {
	
	public DateParse() {
		numberOfParameters = 1;
	}
	
	/**
	 * Create the resulting Date object.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack
		
		Object dateObject = stack.pop();
		if (dateObject instanceof Double) {
			try {
				dateObject = (long)(double)(Double)dateObject;
			} catch (ClassCastException e) {
				throw new ParseException("Invalid second argument for 'date_parse', cannot convert to Date");
			}
		}
		if (!(dateObject instanceof String) && !(dateObject instanceof Long)) {
			throw new ParseException("Invalid argument type for 'date_parse', second argument must be (string) or (long)");
		}
		Date date;
		if (dateObject instanceof String) {
			String dateString = (String)dateObject;
			try {
				date = DateFormat.getDateInstance(DateFormat.SHORT).parse(dateString);
				stack.push(date);
			} catch (java.text.ParseException e) {
				throw new ParseException("Bad string argument for 'date_parse' (" + e.getMessage() + ")");
			}
		} else if (dateObject instanceof Long) {
			long dateLong = (Long)dateObject;
			date = new Date(dateLong);
			stack.push(date);
		}
	}
}
