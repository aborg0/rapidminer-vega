package com.rapidminer.operator.learner.test;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.Learner;
import com.rapidminer.operator.learner.functions.SeeminglyUnrelatedRegressionOperator;
import com.rapidminer.test.TestUtils;
import com.rapidminer.tools.OperatorService;

/** Creates all learners using the {@link OperatorService} and constructs input example sets
 *  according to their capabilities to check whether they operate without throwing exceptions /
 *  throwing the correct exceptions.
 *   
 * */
public class LearnerTestSuite extends TestCase {
	
	private static final Set<Class> SKIP_CLASSES = new HashSet<Class>();
	static {
		SKIP_CLASSES.add(SeeminglyUnrelatedRegressionOperator.class);
	}
	public static Test suite() {
		TestUtils.initRapidMiner();
		TestSuite suite = new TestSuite("Learner test suite");
		for (String key : OperatorService.getOperatorKeys()) {
			OperatorDescription opDesc = OperatorService.getOperatorDescription(key);
			if (Learner.class.isAssignableFrom(opDesc.getOperatorClass()) &&
					!OperatorChain.class.isAssignableFrom(opDesc.getOperatorClass()) &&
					!SKIP_CLASSES.contains(opDesc.getOperatorClass())) {
				suite.addTest(new LearnerTest(opDesc));
			}
		}
		return suite;
	}

}
