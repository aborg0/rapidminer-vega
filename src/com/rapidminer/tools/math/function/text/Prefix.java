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
package com.rapidminer.tools.math.function.text;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Calculates the prefix of the given string and pushes it on the result stack. If the 
 * given string is too short, the complete string will be returned.
 * 
 * @author Ingo Mierswa
 */
public class Prefix extends PostfixMathCommand {

	public Prefix() {
		numberOfParameters = 2;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		if (stack.size() != 2)
			throw new ParseException("Needs three arguments: The string and the length");

		// initialize the result to the first argument
		Object lengthObject = stack.pop();
		Object textObject = stack.pop();
		if (!(textObject instanceof String) || !(lengthObject instanceof Number)) {
			throw new ParseException("Invalid argument type, must be (string, number)");
		}
		
		String text = (String) textObject;
		int length = Math.min(text.length(), ((Number) lengthObject).intValue());
		try {
			stack.push(text.substring(0, length));
		} catch (IndexOutOfBoundsException e) {
			stack.push(text);
		}
	}
}
