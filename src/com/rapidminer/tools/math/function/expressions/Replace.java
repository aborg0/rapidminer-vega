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
package com.rapidminer.tools.math.function.expressions;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Replaces substrings by matching targetSequence to given string.
 * @author Sebastian Land
 */
public class Replace extends PostfixMathCommand {

	public Replace() {
		numberOfParameters = 3;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		if (stack.size() < 3)
			throw new ParseException("Needs three arguments: The string, the target and the replacement string.");

		// initialize the result to the first argument
		Object byObject = stack.pop();
		Object whatObject = stack.pop();
		Object textObject = stack.pop();
		if (!(textObject instanceof String) || !(byObject instanceof String) || !(whatObject instanceof String)) {
			throw new ParseException(
					"Invalid argument type, must be (string, string, string)");
		}

		String by = (String) byObject;
		String what = (String) whatObject;
		String text = (String) textObject;

		if (what.length() == 0)
			throw new ParseException("The target String must contain text");
		
		stack.push(text.replace(what, by));
	}
}
