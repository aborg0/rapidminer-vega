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
package com.rapidminer.operator.meta;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.Process;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPortExtender;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.XMLException;


/** This operator can be used to embed a complete process definition into the current 
 *  process definition. 
 *  The process must have been written into a file before and will be loaded and 
 *  executed when the current process reaches this operator. Optionally, the input
 *  of this operator can be used as input for the embedded process. In both cases,
 *  the output of the process will be delivered as output of this operator. Please note
 *  that validation checks will not work for process containing an operator of this
 *  type since the check cannot be performed without actually loading the process.
 * 
 * @author Ingo Mierswa
 */
public class ProcessEmbeddingOperator extends Operator {

	private final InputPortExtender inputExtender = new InputPortExtender("input", getInputPorts());
	private final OutputPortExtender outputExtender = new OutputPortExtender("result", getOutputPorts());

	/** The parameter name for &quot;The process file which should be encapsulated by this operator&quot; */
	public static final String PARAMETER_PROCESS_FILE = "process_location";

	/** The parameter name for &quot;Indicates if the operator input should be used as input of the process&quot; */
	public static final String PARAMETER_USE_INPUT = "use_input";

	/** Determines whether meta data is propagated through the included process. */
	public static final String PARAMETER_PROPAGATE_METADATA_RECURSIVELY = "propagate_metadata_recursively";

	public static final String PARAMETER_MACROS = "macros";
	
	public static final String PARAMETER_MACRO_NAME = "macro_name";
	
	public static final String PARAMETER_MACRO_VALUE = "macro_value";
	
	
	private Process cachedProcess;
	
	private ProcessSetupError cachedError = null;

	public ProcessEmbeddingOperator(OperatorDescription description) {
		super(description);
		inputExtender.start();
		outputExtender.start();
		getParameters().addObserver(new Observer<String>() {
			@Override
			public void update(Observable<String> observable, String arg) {
				cachedProcess = null;
				cachedError = null;
			}
		}, false);

		getTransformer().addRule(new MDTransformationRule() {
			@Override
			public void transformMD() {
				if (getParameterAsBoolean(PARAMETER_PROPAGATE_METADATA_RECURSIVELY)) {
					if (cachedProcess != null) {
						ProcessRootOperator root = cachedProcess.getRootOperator();

						int requires = root.getSubprocess(0).getInnerSources().getNumberOfConnectedPorts();
						int gets = getInputPorts().getNumberOfConnectedPorts();						
						if (requires != gets) {
							getInputPorts().getPortByIndex(0).addError(new SimpleMetaDataError(Severity.ERROR, getInputPorts().getPortByIndex(0), "included_process_input_mismatch", requires, gets));
						}
						
						int delivers = root.getSubprocess(0).getInnerSinks().getNumberOfConnectedPorts();
						int consumes = getOutputPorts().getNumberOfConnectedPorts();
						if (delivers != consumes) {
							getInputPorts().getPortByIndex(0).addError(new SimpleMetaDataError(Severity.WARNING, getInputPorts().getPortByIndex(0), "included_process_output_mismatch", delivers, consumes));
						}						
						
						if (getParameterAsBoolean(PARAMETER_USE_INPUT)) {			
							root.deliverInputMD(inputExtender.getMetaData(false));
						}
						root.transformMetaData();
						List<MetaData> result = root.getResultMetaData();
						outputExtender.deliverMetaData(result);
					}
				}
			}
		});
	}

	@Override
	protected void performAdditionalChecks() {
		super.performAdditionalChecks();
		if (getParameterAsBoolean(PARAMETER_PROPAGATE_METADATA_RECURSIVELY)) {
			if (cachedProcess == null) {
				try {
					cachedProcess = loadIncludedProcess();
				} catch (Exception e) {
					cachedError = new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), "cannot_load_included_process", e.getMessage());
					addError(cachedError);
				}					
			} else {
				if (cachedError != null) {
					addError(cachedError);
				}
			}
		}
	}

	@Override
	public void doWork() throws OperatorException {
		Process process;
		try {
			process = loadIncludedProcess();
		} catch (RepositoryException e) {
			throw new UserError(this, e, 312, getParameterAsString(PARAMETER_PROCESS_FILE), e.getMessage());
		}

		// define macros
		Map<String, String> macroMap = new HashMap<String, String>();
		List<String[]> macros = getParameterList(PARAMETER_MACROS);
		if (macros != null) {
			for (String[] macroPair : macros) {
				String macroName  = macroPair[0];
				String macroValue = macroPair[1];
				
				macroMap.put(macroName, macroValue);
			}
		}
		
		// run process
		IOContainer result = null;
		if (getParameterAsBoolean(PARAMETER_USE_INPUT)) {			
			result = process.run(new IOContainer(inputExtender.getData(false)),LogService.UNKNOWN_LEVEL, true, macroMap);
		} else {
			result = process.run(new IOContainer(),LogService.UNKNOWN_LEVEL, true, macroMap);
		}
		
		outputExtender.deliver(Arrays.asList(result.getIOObjects()));
	}

	private Process loadIncludedProcess() throws UndefinedParameterError, UserError, RepositoryException {
		RepositoryLocation location = getParameterAsRepositoryLocation(PARAMETER_PROCESS_FILE);
		Entry entry = location.locateEntry();
		if (entry == null) {
			throw new RepositoryException("Entry '"+location+"' does not exist.");
		} else if (entry instanceof ProcessEntry) {
			Process process;
			try {
				process = new RepositoryProcessLocation(location).load(null);
				process.setRepositoryAccessor(getProcess().getRepositoryAccessor());
			} catch (IOException e) {
				throw new UserError(this, 302, location, e.getMessage());
			} catch (XMLException e) {
				throw new UserError(this, 401, e.getMessage());
			}
			return process;
		} else {
			throw new RepositoryException("Entry '"+location+"' is not a data entry, but "+entry.getType());
		}
//
//		String relativeProcessLocation  = getParameterAsString(PARAMETER_PROCESS_FILE);
//		RepositoryLocation resolvedLocation;
//		if ((getProcess() != null) && (getProcess().getRepositoryLocation() != null)) {
//			try {
//				resolvedLocation = new RepositoryLocation(getProcess().getRepositoryLocation().parent(), relativeProcessLocation);
//			} catch (MalformedRepositoryLocationException e) {
//				throw e.makeUserError(this);
//			}
//		} else {
//			getLogger().info("Process is not contained in a repository. Trying to resolve absolute location.");
//			try {
//				resolvedLocation = new RepositoryLocation(relativeProcessLocation);
//			} catch (MalformedRepositoryLocationException e) {
//				throw e.makeUserError(this);
//			}
//		}		
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		
		types.add(new ParameterTypeRepositoryLocation(PARAMETER_PROCESS_FILE, "The process location which should be encapsulated by this operator", false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_INPUT, "Indicates if the operator input should be used as input of the process", false));
		types.add(new ParameterTypeBoolean(PARAMETER_PROPAGATE_METADATA_RECURSIVELY, "Determines whether meta data is propagated through the included process.", false));
		
		types.add(new ParameterTypeList(PARAMETER_MACROS, "Defines macros for this sub-process.", new ParameterTypeString(PARAMETER_MACRO_NAME, "The name of the macro.", false), new ParameterTypeString(PARAMETER_MACRO_VALUE, "The value of the macro.", false), true));
		
		return types;
	}
}
