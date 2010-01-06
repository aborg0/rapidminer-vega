/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
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

import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ports.CollectingPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
/**
 * 
 * @author Sebastian Land
 */
public abstract class AbstractIteratingOperatorChain extends OperatorChain {

	private final PortPairExtender inputPortPairExtender = new PortPairExtender("input", getInputPorts(), getSubprocess(0).getInnerSources());
	private final CollectingPortPairExtender outExtender = new CollectingPortPairExtender("output", getSubprocess(0).getInnerSinks(), getOutputPorts());

	private int currentIteration = 0;
	
	public AbstractIteratingOperatorChain(OperatorDescription description) {
		super(description, "Iteration");

		inputPortPairExtender.start();
		outExtender.start();

		getTransformer().addRule(inputPortPairExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(outExtender.makePassThroughRule());
		
		addValue(new ValueDouble("iteration", "The iteration currently performed by this looping operator.") {
			@Override
			public double getDoubleValue() {
				return currentIteration;
			}
		});
	}
	
	@Override
	public void doWork() throws OperatorException {		
		outExtender.reset();
		this.currentIteration = 0;
		while (!shouldStop(getSubprocess(0).getInnerSinks().createIOContainer(false))) {
			getLogger().fine("Starting iteration "+(currentIteration+1));
			inputPortPairExtender.passDataThrough();
			getSubprocess(0).execute();			
			outExtender.collect();
            inApplyLoop();
            getLogger().fine("Completed iteration "+(currentIteration+1));
            currentIteration++;            
		}        
	}

	protected int getIteration() {
		return currentIteration;
	}
	
	abstract boolean shouldStop(IOContainer iterationResults) throws OperatorException;

}
