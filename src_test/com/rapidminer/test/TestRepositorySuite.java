package com.rapidminer.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.rapidminer.Process;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryVisitor;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;

/**
 * <p>This test suite will add all process tests found in a specified test repository. You can 
 * specify the test repository by setting the following system properties for this repository:
 * 
 *  <ul>
 *  	<li>rapidminer.test.repository.url</li>
 *      <li>rapidminer.test.repository.location</li>
 *  	<li>rapidminer.test.repository.user</li>
 *  	<li>rapidminer.test.repository.password</li>
 *  <ul>
 *  </p>
 *  
 *  <p>Alternatively a file 'test.properties' with this properties can be saved in the home directory
 *  of RapidMiner.</p>
 *  
 *  <p>The alias for the repository will be 'junit'.</p>
 * 
 * @author Marcin Skirzynski
 *
 */
public class TestRepositorySuite extends TestCase {
	
	/**
	 * Creates and returns a suite with all process tests found in the specified repository or an empty
	 * suite if no repository was set.
	 * 
	 * @return			a test suite
	 * @throws RepositoryException
	 * @throws IOException
	 * @throws XMLException
	 */
	public static Test suite() throws RepositoryException, IOException, XMLException {

		final TestSuite suite = new TestSuite(TestRepositorySuite.class.toString());
		TestContext ctx = TestContext.get();
		
		ctx.initRapidMiner();
		if( !ctx.isRepositoryPresent() ) {
			LogService.getRoot().log(Level.WARNING, "No test repository specified -- adding no test from repository");
			return suite;
		}
		
		RepositoryLocation location = ctx.getRepositoryLocation();
		
		if( location==null )
			throw new RuntimeException("No repository location specified");
		
		
		RepositoryManager manager = RepositoryManager.getInstance(location.getAccessor());
		
		// Collecting entries to not throw the exceptions within the visitors method
		final Collection<ProcessEntry> entries = new ArrayList<ProcessEntry>();
		manager.walk(location.locateEntry(), new RepositoryVisitor<ProcessEntry>() {
			
			public boolean visit(ProcessEntry entry) {
				entries.add(entry);
				return false;
			}
			
		}, ProcessEntry.class);
		
		for( ProcessEntry entry : entries ) {
			Process process = new RepositoryProcessLocation(entry.getLocation()).load(null);
			suite.addTest(new ProcessTest(process));
		}
		
		return suite;
	}
	
}
