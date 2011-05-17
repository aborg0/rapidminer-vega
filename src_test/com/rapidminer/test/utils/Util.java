package com.rapidminer.test.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.Process;
import com.rapidminer.operator.IOObject;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;

public class Util {
	
	/**
	 * Token between the process name and the number for the expected results.
	 */
	public static final String EXPECTED_TOKEN = "-expected-port-";
	
	/**
	 * Returns all expected results for the specified process. These are all ioobjects which are directly
	 * contained in the folder of the process.
	 * 
	 * @throws RepositoryException
	 */
	public static List<IOObject> getExpectedResult( Process process ) throws RepositoryException {
			
		Map<Integer, IOObject> results = new HashMap<Integer, IOObject>();
		
		Folder folder = process.getRepositoryLocation().locateEntry().getContainingFolder();
		
		for( DataEntry entry : folder.getDataEntries() ) {
			if( entry instanceof IOObjectEntry ) {
				IOObjectEntry ioo = (IOObjectEntry) entry;
				String name = ioo.getLocation().getName();
				// All expected results begin with port and the number of the port
				String expectedPrefix = process.getRepositoryLocation().getName()+EXPECTED_TOKEN;
				if( name.startsWith(expectedPrefix) ) {
					String number = name.substring(expectedPrefix.length());
					try {
						int i = Integer.parseInt(number);
						results.put(i, ((IOObjectEntry) entry).retrieveData(null));
					} catch (NumberFormatException e) {
						// Can not parse so this is not a valid ioobject for the test and we will skip this
					}
				}
			}
		}
			
		List<Integer> keys = new ArrayList<Integer>(results.keySet());
		Collections.sort(keys);
		
		List<IOObject> sortedResults = new ArrayList<IOObject>();
		for( Integer key : keys ) {
			sortedResults.add(results.get(key));
		}
		return sortedResults;
	}
	
	/**
	 * Removes all stores expected results for the specified process.
	 * 
	 * @param process
	 * @throws RepositoryException
	 */
	public static void removeExpectedResults( Process process ) throws RepositoryException {
		Folder folder = process.getRepositoryLocation().locateEntry().getContainingFolder();
		
		Collection<IOObjectEntry> toDelete = new ArrayList<IOObjectEntry>();
		for( DataEntry entry : folder.getDataEntries() ) {
			if( entry instanceof IOObjectEntry ) {
				IOObjectEntry ioo = (IOObjectEntry) entry;
				String name = ioo.getLocation().getName();
				// All expected results begin with port and the number of the port
				String expectedPrefix = process.getRepositoryLocation().getName()+EXPECTED_TOKEN;
				if( name.startsWith(expectedPrefix) ) {
					toDelete.add(ioo);
				}
			}
		}
		
		for( IOObjectEntry entry : toDelete ) {
			entry.delete();
		}
	}
	
}
