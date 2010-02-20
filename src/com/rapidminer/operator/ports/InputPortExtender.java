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
package com.rapidminer.operator.ports;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.CollectionPrecondition;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.FlatteningPassThroughRule;
import com.rapidminer.operator.ports.metadata.InputMissingMetaDataError;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.ManyToOnePassThroughRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.metadata.Precondition;

/**
 * @author Simon Fischer
 */
public class InputPortExtender extends SinglePortExtender<InputPort> {

	private MetaData desiredMetaData;
	private boolean firstIsMandatory;
	
	public InputPortExtender(String name, Ports<InputPort> ports) {
		super(name, ports);		
	}
	
	public InputPortExtender(String name, Ports<InputPort> ports, MetaData desiredMetaData, boolean firstIsMandatory) {
		this(name, ports);
		this.desiredMetaData = desiredMetaData;
		this.firstIsMandatory = firstIsMandatory;
	}

	@Override
	protected InputPort createPort() {
		InputPort port = super.createPort();
		Precondition precondition = makePrecondition(port);
		if (precondition != null) {			
			port.addPrecondition(new CollectionPrecondition(precondition));
		}
		return port;
	}

	/** Returns a list of non-null data of all input ports. 
	 * @param unfold If true, collections are added as individual objects rather than as a collection. The unfolding is done recursively.  
	 * */
	@SuppressWarnings("unchecked")
	public <T extends IOObject> List<T> getData(boolean unfold) {
		List<T> results = new LinkedList<T>();		
		for (InputPort port : getManagedPorts()) {
			IOObject data = port.getAnyDataOrNull();
			if (data != null) {
				if (unfold && (data instanceof IOObjectCollection)) {
					unfold((IOObjectCollection)data, results);
				} else {
					results.add((T)data);
				}
			}
		}
		return results;
	}

	@SuppressWarnings("unchecked")
	private <T> void unfold(IOObjectCollection<IOObject> data, List<T> results) {
		for (IOObject obj : data.getObjects()) {
			if (obj instanceof IOObjectCollection) {
				unfold((IOObjectCollection)obj, results);
			} else {
				results.add((T)obj);
			}
		}
	}

	/** Returns a list of non-null meta data of all input ports. 
	 */
	public List<MetaData> getMetaData(boolean unfold) {
		List<MetaData> results = new LinkedList<MetaData>();		
		for (InputPort port : getManagedPorts()) {
			MetaData data = port.getMetaData();
			if (data != null) {
				if (unfold && data instanceof CollectionMetaData )
					results.add(((CollectionMetaData) data).getElementMetaDataRecursive());
				else
					results.add(data);
			}
		}
		return results;
	}

	protected Precondition makePrecondition(final InputPort port) {
		if (desiredMetaData != null) {
			return new Precondition() {
				@Override
				public void assumeSatisfied() {
					if (!getManagedPorts().isEmpty())
						getManagedPorts().iterator().next().receiveMD(desiredMetaData);
				}

				@Override
				public void check(MetaData metaData) {
					if (!getManagedPorts().isEmpty()) {
						if (getManagedPorts().iterator().next() == port) {
							if (metaData == null && firstIsMandatory) {
								port.addError(new InputMissingMetaDataError(port, desiredMetaData.getObjectClass(), null));
							}
						}
						if (metaData != null) {
							if (! desiredMetaData.isCompatible(metaData, CompatibilityLevel.VERSION_5)) {
								Collection<MetaDataError> errors = desiredMetaData.getErrorsForInput(port, metaData, CompatibilityLevel.VERSION_5);
								for (MetaDataError error : errors) {				
									port.addError(error);
								}
							}
						}
					}
				}

				@Override
				public String getDescription() {
					return "requires " + ((firstIsMandatory)? " at least one " : "") + desiredMetaData.getDescription();
				}

				@Override
				public MetaData getExpectedMetaData() {
					return desiredMetaData;
				}

				@Override
				public boolean isCompatible(MetaData input, CompatibilityLevel level) {
					return desiredMetaData.isCompatible(input, level);
				}
				
			};
		}
		return null;
	}

	public MDTransformationRule makePassThroughRule(OutputPort outputPort) {
		return new ManyToOnePassThroughRule(getManagedPorts(), outputPort);		
	}

	public MDTransformationRule makeFlatteningPassThroughRule(OutputPort outputPort) {
		return new FlatteningPassThroughRule(getManagedPorts(), outputPort);		
	}


}
