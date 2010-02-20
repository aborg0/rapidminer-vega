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
package com.rapidminer.io.process.rules;

import org.w3c.dom.Element;

import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.XMLException;

/**
 * This superclass of all parse rules retrieves the operatorType affected by this rule. Subclasses might access them by
 * using the operatorTypeName.
 * 
 * @author Sebastian Land
 * 
 */
public abstract class AbstractParseRule implements ParseRule {

	protected String operatorTypeName;

	public AbstractParseRule(String operatorTypeName, Element element) throws XMLException {
		this.operatorTypeName = operatorTypeName;
	}

	@Override
	public String apply(Operator operator, XMLImporter importer) {
		if (operator.getOperatorDescription().getKey().equals(operatorTypeName))
			return apply(operator, operatorTypeName, importer);
		return null;
	}

	protected abstract String apply(Operator operator, String operatorTypeName, XMLImporter importer);
}
