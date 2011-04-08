package com.rapidminer.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.rapidminer.example.test.ExampleTestSuite;
import com.rapidminer.operator.annotation.test.PolynomialFunctionTest;
import com.rapidminer.operator.io.test.DatabaseWriteTest;
import com.rapidminer.operator.learner.test.LearnerTestSuite;
import com.rapidminer.operator.performance.test.PerformanceTestSuite;

/**
 * 
 * @author Simon Fischer
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({

	// We start with the fast tests
	ExampleTestSuite.class,
	PerformanceTestSuite.class,
	EscapeTest.class,
	OperatorVersionTest.class,	
	IterationArrayListTest.class,
	MathUtilsTest.class,
	PolynomialFunctionTest.class,
	SECDTest.class,
	
	// TODO MS CSV reader test not working
//	CSVReaderTest.class//,
	
//	// Slow, instantiates all learners
	LearnerTestSuite.class,
	
	// Depends on servers being up, timeout takes a while
	DatabaseWriteTest.class
	//SampleTest.class,
	})
public class AllTests {

}
