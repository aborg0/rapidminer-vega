/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
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
package com.rapidminer.operator.meta;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.SimpleOperatorChain;
import com.rapidminer.operator.Value;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;

/**
 * <p>This operator performs the inner operators and delivers the result of the
 * inner operators. If any error occurs during this subprocess, this error
 * will be neglected and this operator simply will return no additional 
 * input.</p>
 * 
 * <p>Please use this operator with care since it will also cover errors which
 * are not expected by the analyst. In combination with a process branch, however,
 * it can be used to handle exceptions in the analysis process (i.e. expected errors).
 * </p>
 *   
 * @author Ingo Mierswa
 */
public class ExceptionHandling extends SimpleOperatorChain {
	
	public static final String PARAMETER_EXCEPTION_MACRO = "exception_macro";

	private boolean withoutError = true;
	private Exception exception;
	
	public ExceptionHandling(OperatorDescription description) {
		super(description);
		addValue(new Value("success", "Indicates whether the executionwas successful") {
			@Override
			public Object getValue() {
				return withoutError;
			}

			@Override
			public boolean isNominal() {
				return true;
			}			
		});
		addValue(new Value("exception", "The exception that occured during execution.") {
			@Override
			public Object getValue() {				
				return exception;
			}

			@Override
			public boolean isNominal() {
				return true;
			}			
		});
	}

	@Override
	public void doWork() throws OperatorException {
		withoutError = true;
		exception = null;
		try {			
			super.doWork();
		} catch (Exception e) {
			logWarning("Error occurred and will be neglected by " + getName() + ": " + e.getMessage());
			if (isParameterSet(PARAMETER_EXCEPTION_MACRO)) {
				getProcess().getMacroHandler().addMacro(getParameterAsString(PARAMETER_EXCEPTION_MACRO), e.getMessage());
			}
			withoutError = false;
			this.exception = e;
			for (OutputPort port : getOutputPorts().getAllPorts()) {
				port.deliver(null);
			}
		}
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeString(PARAMETER_EXCEPTION_MACRO, "The name of the macro a potentially occuring exception message will be stored in.", true));
		types.addAll(super.getParameterTypes());
		return types;
	}
}
