package com.rapidminer.test;

import junit.framework.TestCase;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.test.utils.RapidAssert;
import com.rapidminer.test.utils.Util;

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
		for (Operator op : process.getRootOperator().getAllInnerOperators()) {
			op.setBreakpoint(BreakpointListener.BREAKPOINT_AFTER, false);
			op.setBreakpoint(BreakpointListener.BREAKPOINT_BEFORE, false);
		}
		process.getRootOperator().setBreakpoint(BreakpointListener.BREAKPOINT_AFTER, false);
		process.getRootOperator().setBreakpoint(BreakpointListener.BREAKPOINT_BEFORE, false);

		IOContainer results = process.run();
		RapidAssert.assertEquals(Util.getExpectedResult(process), results.asList());
	}
	

	/**
	 * Display name for the test will be the process location.
	 */
	@Override
	public String getName() {
		return process.getRepositoryLocation().getAbsoluteLocation();
	}

}
