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
import com.rapidminer.operator.learner.functions.neuralnet.SimpleNeuralNetLearner;
import com.rapidminer.test.TestContext;
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
		SKIP_CLASSES.add(SimpleNeuralNetLearner.class);
	}
	public static Test suite() {
		TestContext.get().initRapidMiner();
		TestSuite suite = new TestSuite("Learner test suite");
		for (String key : OperatorService.getOperatorKeys()) {
			if (key.startsWith("weka:")) {
				continue;
			}
			OperatorDescription opDesc = OperatorService.getOperatorDescription(key);
			if (Learner.class.isAssignableFrom(opDesc.getOperatorClass()) &&
					!OperatorChain.class.isAssignableFrom(opDesc.getOperatorClass()) &&
					!SKIP_CLASSES.contains(opDesc.getOperatorClass()) &&
					!opDesc.isDeprecated()) {
				suite.addTest(new LearnerTest(opDesc));
			}
		}
		return suite;
	}

}
