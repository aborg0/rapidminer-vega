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
package com.rapidminer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.container.Pair;

/** <p>The process context holds some data controlling the execution of a {@link Process}.
 *  This includes connections of the input and output ports of the root operator
 *  to repository locations as well as the definition of macros.
 *  </p>
 *  <p>
 *  The fact that this data is defined outside the process itself is particularly
 *  useful if this process is offered as a service, so it can be adapted easily. Furthermore,
 *  this saves the process designer from defining data reading and storing operators at the beginning
 *  and at the end of the process.</p>
 *  <p>
 *  Note: A ProcessContext is not necessarily associate with a {@link Process}. E.g., if a process
 *  is run remotely, it does not necessarily exist on the machine that prepares the context.
 *  </p>
 *  <p>
 *  Since this class acts merely as a data container, it has public getter and setter methods
 *  which return references to the actual data (as opposed to immutable views). In order to trigger
 *  an update, call a setter method rather than adding to the lists, which is invisible to the
 *  process context. 
 *  </p><p>
 *  The data is saved as strings rather than, e.g. using {@link RepositoryLocation}s.  
 *  </p>
 *  
 * @author Simon Fischer
 */
public class ProcessContext extends AbstractObservable<ProcessContext> {

	private List<String> inputRepositoryLocations = new LinkedList<String>();

	private List<String> outputRepositoryLocations= new LinkedList<String>();

	private List<Pair<String,String>> macros = new LinkedList<Pair<String,String>>();

	public ProcessContext() {

	}

	public List<String> getInputRepositoryLocations() {
		return Collections.unmodifiableList(inputRepositoryLocations);
	}

	public void setInputRepositoryLocations(List<String> inputRepositoryLocations) {
		if (inputRepositoryLocations.contains(null)) {
			throw new NullPointerException("Null elements not allowed");
		}
		this.inputRepositoryLocations = inputRepositoryLocations;
		fireUpdate(this);
	}

	public List<String> getOutputRepositoryLocations() {
		return Collections.unmodifiableList(outputRepositoryLocations);
	}

	public void setOutputRepositoryLocations(List<String> outputRepositoryLocations) {
		if (outputRepositoryLocations.contains(null)) {
			throw new NullPointerException("Null elements not allowed");
		}
		this.outputRepositoryLocations = outputRepositoryLocations;
		fireUpdate(this);
	}

	public List<Pair<String, String>> getMacros() {
		return macros;
	}

	public void addMacro(Pair<String, String> macro) {
		this.macros.add(macro);
	}
	
	public void setMacros(List<Pair<String, String>> macros) {
		this.macros = macros;
		fireUpdate(this);
	}

	public void setOutputRepositoryLocation(int index, String location) {
		if (location == null) {
			throw new NullPointerException("Null location not allowed");
		}
		while (outputRepositoryLocations.size() <= index) {
			outputRepositoryLocations.add("");			
		}		
		outputRepositoryLocations.set(index, location);
		fireUpdate();
	}

	public void setInputRepositoryLocation(int index, String location) {
		if (location == null) {
			throw new NullPointerException("Null location not allowed");
		}
		while (inputRepositoryLocations.size() <= index) {
			inputRepositoryLocations.add("");			
		}
		inputRepositoryLocations.set(index, location);
		fireUpdate();
	}

	public void removeOutputLocation(int rowIndex) {
		outputRepositoryLocations.remove(rowIndex);		
	}

	public void removeInputLocation(int rowIndex) {
		inputRepositoryLocations.remove(rowIndex);		
	}

	public void addOutputLocation(String location) {
		if (location == null) {
			throw new NullPointerException("Location must not be null");
		}
		outputRepositoryLocations.add(location);
	}

	public void addInputLocation(String location) {
		if (location == null) {
			throw new NullPointerException("Location must not be null");
		}
		inputRepositoryLocations.add(location);
	}
}
