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
package com.rapidminer.gui.processeditor.profiler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.ExampleSetFactory;
import com.rapidminer.repository.RepositoryLocation;


/**
 * Handles the data of the profiler during runtime.
 * 
 * @author Marco Boeck
 *
 */
public class ProfilerDataManager {
	
	/** the map holding all relevant data for the current process during runtime */
	private Map<String, ProfilerData> dataMap;
	
	/** the map holding all data maps for all processes during runtime */
	private Map<ProcessLocation, Map<String, ProfilerData>> processMap;
	
	/** the map holding all RepositoryLocations (if specified) for all processes during runtime */
	private Map<ProcessLocation, RepositoryLocation> processMergeLocationMap;
	
	/** the instance of the class */
	private static ProfilerDataManager pdm;
	
	private static final Logger LOGGER = Logger.getLogger(ProfilerDataManager.class.getName());
	
	
	/**
	 * Private constructor.
	 */
	private ProfilerDataManager() {
		processMap = new HashMap<ProcessLocation, Map<String, ProfilerData>>();
		dataMap = new HashMap<String, ProfilerData>();
		processMergeLocationMap = new HashMap<ProcessLocation, RepositoryLocation>();
	}
	
	/**
	 * Singleton access.
	 * @return the ProfilerDataManager instance
	 */
	public static synchronized ProfilerDataManager getInstance() {
		if (pdm == null) {
			pdm = new ProfilerDataManager();
		}
		return pdm;
	}
	
	/**
	 * Gets the data to the specified operator.
	 * @param name the name of the operator
	 * @return the map containing the operator data
	 */
	public ProfilerData getOperatorData(String name) {
		return dataMap.get(name);
	}
	
	/**
	 * Gets if there is already data for the specified operator.
	 * @param name the name of the operator
	 * @return true if the specified operator has already been added; false otherwise
	 */
	public boolean isOperatorDataAvailable(String name) {
		return dataMap.get(name) != null;
	}
	
	/**
	 * Needs to be called if the currently active process has changed.
	 * @param process the new process
	 */
	public void setProcess(Process process) {
		if (!processMap.containsKey(process.getProcessLocation())) {
			dataMap = new HashMap<String, ProfilerData>();
			processMap.put(process.getProcessLocation(), dataMap);
		} else {
			dataMap = processMap.get(process.getProcessLocation());
		}
	}
	
	/**
	 * Adds the specified operator data to the manager.
	 * @param process the current process.
	 * @param name the name of the operator
	 * @param data the data for the operator
	 * @return the ProfilerData now associated with the operator
	 */
	public ProfilerData addOperatorData(Process process, String name, ProfilerData data) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}
		if (name == null) {
			throw new IllegalArgumentException("name must not be null!");
		}
		if (data == null) {
			throw new IllegalArgumentException("data must not be null!");
		}
		if (isOperatorDataAvailable(name)) {
			LOGGER.warning("Operator data for operator " + name + " already existing!");
			return getOperatorData(name);
		}
		dataMap.put(name, data);
		return data;
	}
	
	/**
	 * Sets the RepositoryLocation into which to merge (if auto merge has been toggled).
	 * @param process the process
	 * @param location the RepositoryLocation for the process
	 */
	public void setMergeLocation(Process process, RepositoryLocation location) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}
		if (location == null) {
			throw new IllegalArgumentException("location must not be null!");
		}
		processMergeLocationMap.put(process.getProcessLocation(), location);
	}
	
	/**
	 * Gets the RepositoryLocation into which to merge (if auto merge has been toggled).
	 * @param process the process of which the RepositoryLocation should be returned
	 * @return the RepositoryLocation into which to merge for the given process
	 */
	public RepositoryLocation getMergeLocation(Process process) {
		return processMergeLocationMap.get(process.getProcessLocation());
	}
	
	/**
	 * Resets all gathered data.
	 */
	public void resetAllData() {
		processMap.clear();
		dataMap.clear();
	}
	
	/**
	 * Resets the gathered data of the current process.
	 */
	public void resetCurrentData() {
		dataMap.clear();
	}

	/**
	 * Gets the profiling data as an ExampleSet so it can be saved.
	 * @return the profiling data as an ExampleSet
	 */
	public ExampleSet getProfilingDataAsExampleSet() {
		// second size argument is the number of variables in the ProfilerData class + 1 (operator name)
		Object[][] data = new Object[dataMap.size()][8];
		int i = 0;
		for (String key : dataMap.keySet()) {
			ProfilerData profilerData = dataMap.get(key);
			
			data[i][0] = key;
			data[i][1] = profilerData.getRunCount();
			data[i][2] = profilerData.getLastRunTimeReal();
			data[i][3] = profilerData.getLastRunTimeCpu();
			data[i][4] = profilerData.getAverageRunTimeReal();
			data[i][5] = profilerData.getAverageRunTimeCpu();
			data[i][6] = profilerData.getTotalRunTimeReal();
			data[i][7] = profilerData.getTotalRunTimeCPU();
			
			i++;
		}
		
		ExampleSet example = ExampleSetFactory.createExampleSet(data);
		for (Attribute att : example.getAttributes()) {
			if (att.getName().equals("att1")) {
				att.setName("Operator Name");
				continue;
			}
			if (att.getName().equals("att2")) {
				att.setName("Number of executions");
				continue;
			}
			if (att.getName().equals("att3")) {
				att.setName("Last execution time in ms (real time)");
				continue;
			}
			if (att.getName().equals("att4")) {
				att.setName("Last execution time in ns (cpu time)");
				continue;
			}
			if (att.getName().equals("att5")) {
				att.setName("Average execution time in ms (real time)");
				continue;
			}
			if (att.getName().equals("att6")) {
				att.setName("Average execution time in ns (cpu time)");
				continue;
			}
			if (att.getName().equals("att7")) {
				att.setName("Total execution time in ms (real time)");
				continue;
			}
			if (att.getName().equals("att8")) {
				att.setName("Total execution time in ns (cpu time)");
				continue;
			}
		}
		
		return example;
	}
	
	/**
	 * Merges the current profiling data with an existing ExampleSet so it can be saved and used for e.g. statistical
	 * analysis.
	 * @return the merged profiling data as an ExampleSet
	 */
	public ExampleSet getProfilingDataAsMergedExampleSet(ExampleSet existingExample) {
		// second size argument is the number of variables in the ProfilerData class + 1 (operator name)
		int dataMapSize = dataMap.size() + (existingExample != null ? existingExample.size() : 0);
		if (existingExample != null) {
			// reduce Array size by one for each match which will be merged later on
			for (String key : dataMap.keySet()) {
				for (int j=0; j<existingExample.size(); j++) {
					Example example = existingExample.getExample(j);
					Attribute att = example.getAttributes().get("Operator Name");
					if (example.getNominalValue(att).equals(key)) {
						dataMapSize--;
						break;
					}
				}
			}
		}
		
		List<Example> usedExampleList = new ArrayList<Example>();
		Object[][] data = new Object[dataMapSize][8];
		int i = 0;
		for (String key : dataMap.keySet()) {
			boolean found = false;
			ProfilerData profilerData = dataMap.get(key);
			// example is null if new ExampleSet has been chosen in den wizard
			if (existingExample != null) {
				for (int j=0; j<existingExample.size(); j++) {
					Example example = existingExample.getExample(j);
					Attribute att = example.getAttributes().get("Operator Name");
					if (example.getNominalValue(att).equals(key)) {
						found = true;
						usedExampleList.add(example);
						double newRunCount = example.getValue(example.getAttributes().get("Number of executions")) + profilerData.getRunCount();
						
						data[i][0] = key;
						data[i][1] = newRunCount;
						data[i][2] = profilerData.getLastRunTimeReal() != 0 ? profilerData.getLastRunTimeReal() : example.getValue(example.getAttributes().get("Last execution time in ms (real time)"));
						data[i][3] = profilerData.getLastRunTimeCpu() != 0 ? profilerData.getLastRunTimeCpu() : example.getValue(example.getAttributes().get("Last execution time in ns (cpu time)"));
						data[i][4] = (example.getValue(example.getAttributes().get("Total execution time in ms (real time)")) + profilerData.getTotalRunTimeReal())/newRunCount;
						data[i][5] = (example.getValue(example.getAttributes().get("Total execution time in ns (cpu time)")) + profilerData.getTotalRunTimeCPU())/newRunCount;
						data[i][6] = example.getValue(example.getAttributes().get("Total execution time in ms (real time)")) + profilerData.getTotalRunTimeReal();
						data[i][7] = example.getValue(example.getAttributes().get("Total execution time in ns (cpu time)")) + profilerData.getTotalRunTimeCPU();
						
						break;
					}
				}
			}
			
			// create new data if it doesn't exist yet
			if (!found) {
				data[i][0] = key;
				data[i][1] = profilerData.getRunCount();
				data[i][2] = profilerData.getLastRunTimeReal();
				data[i][3] = profilerData.getLastRunTimeCpu();
				data[i][4] = profilerData.getAverageRunTimeReal();
				data[i][5] = profilerData.getAverageRunTimeCpu();
				data[i][6] = profilerData.getTotalRunTimeReal();
				data[i][7] = profilerData.getTotalRunTimeCPU();
			}
			i++;
		}
		// add values from existing ExampleSet again which have not been merged
		if (existingExample != null) {
			for (Example example : existingExample) {
				// check for used examples
				boolean used = false;
				for (Example usedExample : usedExampleList) {
					if (example.getDataRow().equals(usedExample.getDataRow())) {
						used = true;
						break;
					}
				}
				if (used) {
					continue;
				}
				data[i][0] = example.getNominalValue(example.getAttributes().get("Operator Name"));
				data[i][1] = example.getValue(example.getAttributes().get("Number of executions"));
				data[i][2] = example.getValue(example.getAttributes().get("Last execution time in ms (real time)"));
				data[i][3] = example.getValue(example.getAttributes().get("Last execution time in ns (cpu time)"));
				data[i][4] = example.getValue(example.getAttributes().get("Average execution time in ms (real time)"));
				data[i][5] = example.getValue(example.getAttributes().get("Average execution time in ns (cpu time)"));
				data[i][6] = example.getValue(example.getAttributes().get("Total execution time in ms (real time)"));
				data[i][7] = example.getValue(example.getAttributes().get("Total execution time in ns (cpu time)"));
				
				i++;
			}
		}
		
		ExampleSet example = ExampleSetFactory.createExampleSet(data);
		for (Attribute att : example.getAttributes()) {
			if (att.getName().equals("att1")) {
				att.setName("Operator Name");
				continue;
			}
			if (att.getName().equals("att2")) {
				att.setName("Number of executions");
				continue;
			}
			if (att.getName().equals("att3")) {
				att.setName("Last execution time in ms (real time)");
				continue;
			}
			if (att.getName().equals("att4")) {
				att.setName("Last execution time in ns (cpu time)");
				continue;
			}
			if (att.getName().equals("att5")) {
				att.setName("Average execution time in ms (real time)");
				continue;
			}
			if (att.getName().equals("att6")) {
				att.setName("Average execution time in ns (cpu time)");
				continue;
			}
			if (att.getName().equals("att7")) {
				att.setName("Total execution time in ms (real time)");
				continue;
			}
			if (att.getName().equals("att8")) {
				att.setName("Total execution time in ns (cpu time)");
				continue;
			}
		}
		
		return example;
	}
}
