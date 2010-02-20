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
package com.rapidminer.operator;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.UndefinedParameterError;


/** Abstract superclass of all operators modifying an example set, i.e. accepting
 *  an {@link ExampleSet} as input and delivering an {@link ExampleSet} as output.
 *  The behaviour is delegated from the {@link #doWork()} method to
 *  {@link #apply(ExampleSet)}.
 * 
 * @author Simon Fischer
 */
public abstract class AbstractExampleSetProcessing extends Operator {

	private final InputPort exampleSetInput = getInputPorts().createPort("example set input");
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set output");
	private final OutputPort originalOutput = getOutputPorts().createPort("original");

	public AbstractExampleSetProcessing(OperatorDescription description) {
		super(description);
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, getRequiredMetaData()));
		getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetOutput, false) {
			@Override
			public MetaData modifyMetaData(MetaData metaData) {
				if (metaData instanceof ExampleSetMetaData) {
					try {
						return AbstractExampleSetProcessing.this.modifyMetaData((ExampleSetMetaData)metaData);
					} catch (UndefinedParameterError e) {
						return metaData;
					}
				} else {
					return metaData;
				}
			}
		});
		getTransformer().addPassThroughRule(exampleSetInput, originalOutput);
	}

	/** Returns the example set input port, e.g. for adding errors. */
	protected final InputPort getInputPort() {
		return exampleSetInput;
	}

	/** Subclasses might override this method to define the meta data transformation performed 
	 *  by this operator. 
	 * @throws UndefinedParameterError */
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		return metaData;		
	}

	/** Subclasses my override this method to define more precisely the meta data expected
	 *  by this operator. */
	protected ExampleSetMetaData getRequiredMetaData() {
		return new ExampleSetMetaData();
	}


	/** Delegate for the apply method. The given ExampleSet is already a clone of the
	 *  input example set so that changing this examples set does not affect the original one. 
	 *  Subclasses should avoid cloning again unnecessarily. */
	public abstract ExampleSet apply(ExampleSet exampleSet) throws OperatorException;

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == originalOutput) {
			return getParameterAsBoolean("keep_example_set");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public final void doWork() throws OperatorException {
		ExampleSet inputExampleSet = exampleSetInput.getData();
		ExampleSet result = apply((ExampleSet) inputExampleSet.clone());
		originalOutput.deliver(inputExampleSet);
		exampleSetOutput.deliver(result);		
	}


	public InputPort getExampleSetInputPort() {
		return exampleSetInput;
	}

	public OutputPort getExampleSetOutputPort() {
		return exampleSetOutput;
	}
}
