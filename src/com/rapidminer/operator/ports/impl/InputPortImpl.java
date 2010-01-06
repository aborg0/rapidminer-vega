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
package com.rapidminer.operator.ports.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;

/**  
 *  
 * @author Simon Fischer
 *
 */
public class InputPortImpl extends AbstractPort implements InputPort {

	private final Collection<Precondition> preconditions = new LinkedList<Precondition>();

	private MetaData metaData;
	
	/** The port to which this port is connected. */
	private OutputPort sourceOutputPort;

	/** Use the factory method {@link InputPorts#createPort()} to create InputPorts. */
	protected InputPortImpl(Ports<? extends Port> owner, String name, boolean simulatesStack) {
		super(owner, name, simulatesStack);		
	}

	@Override
	public void clear(int clearFlags) {		
		super.clear(clearFlags);		
		if ((clearFlags & CLEAR_METADATA) > 0) {
			this.metaData = null;
		}
	}

	@Override
	public void receive(IOObject object) {		
		setData(object);
	}

	@Override
	public void receiveMD(MetaData metaData) {
		assert(this.metaData != null);
		this.metaData = metaData;	
	}

	@Override
	public MetaData getMetaData() {
		return metaData;
	}

	void connect(OutputPort outputPort) {
		this.sourceOutputPort = outputPort;
		fireUpdate(this);
	}

	@Override
	public OutputPort getSource() {		
		return sourceOutputPort;
	}



	@Override
	public void addPrecondition(Precondition precondition) {
		preconditions.add(precondition);

	}

	@Override
	public void checkPreconditions() {
		MetaData metaData = getMetaData();
		for (Precondition precondition : preconditions) {
			try {
				precondition.check(metaData);
			} catch (Exception e) {
				getPorts().getOwner().getOperator().getLogger().log(Level.WARNING, "Error checking preconditions at "+getSpec()+": "+e, e);
				this.addError(new SimpleMetaDataError(Severity.WARNING, this, "exception_checking_precondition", e.toString()));
			}
		}		
	}

	@Override
	public String getPreconditionDescription() {
		StringBuilder buf = new StringBuilder();
		buf.append(getName());
		buf.append(": ");
		for (Precondition precondition : preconditions) {
			buf.append(precondition.getDescription());
		}
		return buf.toString();
	}

	@Override
	public boolean isInputCompatible(MetaData input, CompatibilityLevel level) {
		for (Precondition precondition : preconditions) {
			if (!precondition.isCompatible(input, level)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getDescription() {
		StringBuilder b = new StringBuilder();
		boolean first = true;
		for (Precondition precondition : preconditions) {
			if (!first) {
				b.append(", ");				
			} else {
				first = false;
			}
			b.append(precondition.getDescription());
		}
		return b.toString();
	}

	@Override
	public boolean isConnected() {
		return (sourceOutputPort != null);
	}

	@Override
	public Collection<Precondition> getAllPreconditions() {
		return Collections.unmodifiableCollection(preconditions);		
	}
}
