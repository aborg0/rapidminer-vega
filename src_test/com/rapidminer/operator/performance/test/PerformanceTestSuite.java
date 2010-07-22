package com.rapidminer.operator.performance.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ClassificationCriterionTest.class,
	EstimatedCriterionTest.class,
	MeasuredCriterionTest.class
})
public class PerformanceTestSuite {

}
