package com.rapidminer.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.rapidminer.example.test.ExampleTestSuite;
import com.rapidminer.operator.annotation.test.PolynomialFunctionTest;
import com.rapidminer.operator.io.test.CSVReaderTest;
import com.rapidminer.operator.io.test.DatabaseWriteTest;
import com.rapidminer.operator.performance.test.PerformanceTestSuite;

/**
 * 
 * @author Simon Fischer
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	ExampleTestSuite.class,
	PerformanceTestSuite.class,
	DatabaseWriteTest.class,
	CSVReaderTest.class,
	OperatorVersionTest.class,
	
	IterationArrayListTest.class,
	MathUtilsTest.class,
	PolynomialFunctionTest.class,
	SECDTest.class,
	
	//SampleTest.class
	})
public class AllTests {

}
