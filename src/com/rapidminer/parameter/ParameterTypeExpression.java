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
package com.rapidminer.parameter;

import com.rapidminer.operator.ports.InputPort;

/**
 * This attribute type supports the user by letting him define an expression with a
 * user interface known from calculators.
 * 
 * For knowing attribute names before process execution a valid meta data transformation must be performed. Otherwise
 * the user might type in the name, instead of choosing.
 * 
 * @author Ingo Mierswa
 */
public class ParameterTypeExpression extends ParameterTypeString {
	
	private static final long serialVersionUID = -1938925853519339382L;

	private transient InputPort inPort;

	public ParameterTypeExpression(final String key, String description, InputPort inPort) {
		this(key, description, inPort, false);
	}

	public ParameterTypeExpression(final String key, String description, InputPort inPort, boolean optional, boolean expert) {
		this(key, description, inPort, optional);
		setExpert(expert);
	}

	public ParameterTypeExpression(final String key, String description, InputPort inPort, boolean optional) {
		super(key, description, optional);
		this.inPort = inPort;
	}

	@Override
	public Object getDefaultValue() {
		return "";
	}
	
	public InputPort getInputPort() {
		return inPort;
	}
}
