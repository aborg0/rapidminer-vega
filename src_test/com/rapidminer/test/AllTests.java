package com.rapidminer.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.rapidminer.example.test.ExampleTestSuite;
import com.rapidminer.operator.annotation.test.PolynomialFunctionTest;
import com.rapidminer.operator.learner.test.LearnerTestSuite;
import com.rapidminer.operator.performance.test.PerformanceTestSuite;

/**
 * 
 * @author Simon Fischer
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({

	// Fast tests
	ExampleTestSuite.class,
	PerformanceTestSuite.class,

	PolynomialFunctionTest.class,
	
	EscapeTest.class,
	OperatorVersionTest.class,	
	IterationArrayListTest.class,
	MathUtilsTest.class,
	SECDTest.class,
	
	// Slow tests
	LearnerTestSuite.class,
	TestRepositorySuite.class
	
	
	// Database tests
	// Depends on servers being up, timeout takes a while
	// DatabaseWriteTest.class
	// SampleTest.class,
	
	// TODO MS CSV reader test not working
	//	CSVReaderTest.class//,
})
	
public class AllTests {}
