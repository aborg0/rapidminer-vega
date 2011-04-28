package com.rapidminer.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import com.rapidminer.Process;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;

/**
 * <p>Tests a single process by doing a run of the process and testing if the output
 * ports return the expected {@link IOObject}.</p>
 * 
 * <p>Expected results will be fetched as follows: All {@link IOObject}s in the same repository
 * location which follow the name scheme "processname-expected-port-01", "processname-expected-port-02", ...
 * will be used and mapped to the output port. The number at the end of the entry will determine
 * the specific port.</p>  
 *  
 * @author Marcin Skirzynski
 *
 */
public class ProcessTest extends TestCase {
	
	/**
	 * Token between the process name and the number for the expected results.
	 */
	public static final String EXPECTED_TOKEN = "-expected-port-";
	
	/**
	 * The process to test
	 */
	private final Process process;

	/**
	 * Creates a test case for the specified process
	 * 
	 * @param process	the process to test
	 */
	public ProcessTest( Process process ) {
		super("testProcess");
		this.process = process;
	}
	
	/**
	 * Initializes RapidMiner if not done yet.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TestContext.get().initRapidMiner();
	}
	
	/**
	 * Runs the process and compares the actual results with the expected.
	 *  
	 * @throws OperatorException
	 * @throws RepositoryException
	 */
	public void testProcess() throws OperatorException, RepositoryException {
		IOContainer results = process.run();
		List<IOObject> expectedResults = getExpectedResult(process);
		
		if( results.size()!=expectedResults.size() ) {
			throw new AssertionFailedError("Number of connected output ports in the process is not equal with the number of ioobjects contained in the same folder with the format 'processname-expected-port-1', 'processname-expected-port-2', ...");
		}
		
		for( int i=0; i<results.size(); i++ ) {
			IOObject actual = results.getElementAt(i);
			IOObject expected = expectedResults.get(i); 
			
			if( expected instanceof ExampleSet && actual instanceof ExampleSet )
				TestUtils.assertEquals("ExampleSets are not equal", (ExampleSet)expected, (ExampleSet)actual, -1);

		}
	}
	
	/**
	 * Returns all expected results for the specified process. These are all ioobjects which are directly
	 * contained in the folder of the process.
	 * 
	 * @throws RepositoryException
	 */
	private List<IOObject> getExpectedResult( Process process ) throws RepositoryException {
			
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
	 * Display name for the test will be the process location.
	 */
	@Override
	public String getName() {
		return process.getRepositoryLocation().getAbsoluteLocation();
	}

}
