package com.rapidminer.test.utils;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.visualization.dependencies.NumericalMatrix;
import com.rapidminer.tools.math.Averagable;
import com.rapidminer.tools.math.AverageVector;

/**
 * Extension for JUnit's Assert for testing RapidMiner objects.
 * 
 * @author Simon Fischer, Marcin Skirzynski
 * 
 */
public class RapidAssert extends Assert {
	
	public static final double DELTA = 0.000000001; 

	
	/**
	 * Extends the Junit assertEquals method by additionally checking the doubles for NaN.
	 *  
	 * @param message		message to display if an error occurs
	 * @param expected		expected value
	 * @param actual		actual value
	 */
    public static void assertEqualsNaN(String message, double expected, double actual) {
        if (Double.isNaN(expected)) {
            if (!Double.isNaN(actual)) {
                throw new AssertionFailedError(message + " expected: <" + expected + "> but was: <" + actual + ">");
            }
        } else {
            assertEquals(message, expected, actual, DELTA);
        }
    }

    /**
     * Tests if the special names of the attribute roles are equal and the associated attributes themselves.
     * 
	 * @param message		message to display if an error occurs
	 * @param expected		expected value
	 * @param actual		actual value
     */
    public static void assertEquals(String message, AttributeRole expected, AttributeRole actual) {
        Assert.assertEquals(message + " (attribute role)", expected.getSpecialName(), actual.getSpecialName());
        Attribute a1 = expected.getAttribute();
        Attribute a2 = actual.getAttribute();
        assertEquals(message, a1, a2);
    }

    
    /**
     * Tests two attributes by using the name, type, block, type, default value and the nominal mapping
     * 
	 * @param message		message to display if an error occurs
	 * @param expected		expected value
	 * @param actual		actual value
     */
    public static void assertEquals(String message, Attribute expected, Attribute actual) {
        Assert.assertEquals(message + " (attribute name)", expected.getName(), actual.getName());
        Assert.assertEquals(message + " (attribute type)", expected.getValueType(), actual.getValueType());
        Assert.assertEquals(message + " (attribute block type)", expected.getBlockType(), actual.getBlockType());
        Assert.assertEquals(message + " (default value)", expected.getDefault(), actual.getDefault());
        if (expected.isNominal()) {
            assertEquals(message + " (nominal mapping)", expected.getMapping(), actual.getMapping());
        }
    }

    /**
     * Tests two nominal mappings for its size and values.
     * 
	 * @param message		message to display if an error occurs
	 * @param expected		expected value
	 * @param actual		actual value
     */
    public static void assertEquals(String message, NominalMapping expected, NominalMapping actual) {
        Assert.assertEquals(message + " (nominal mapping size)", expected.size(), actual.size());
        List<String> v1 = expected.getValues();
        List<String> v2 = actual.getValues();
        Assert.assertEquals(message + " (nominal values)", v1, v2);
        if (v1 != null) { // v2 also != null
            for (String value : v1) {
                Assert.assertEquals(message + " (index of nominal value '" + value + "')", expected.getIndex(value), actual.getIndex(value));
            }
        }
    }

    /**
     * Tests two example sets by iterating over all examples until the number of rows to consider are reached. If
     * this number is -1 there will be no limitation.
     * 
	 * @param message		message to display if an error occurs
	 * @param expected		expected value
	 * @param actual		actual value
     * @param numberOfRowsToConsider	number of examples to consider for the test. -1 means: No limit!
     */
    public static void assertEquals(String message, ExampleSet es1, ExampleSet es2, int numberOfRowsToConsider) {
        if (numberOfRowsToConsider == -1) {
            numberOfRowsToConsider = Integer.MAX_VALUE;
        }
        assertEquals(message, es1.getAttributes(), es2.getAttributes());
        Assert.assertEquals(message + " (number of examples)", es1.size(), es2.size());
        Iterator<Example> i1 = es1.iterator();
        Iterator<Example> i2 = es2.iterator();
        int row = 0;
        while (i1.hasNext() && i2.hasNext() && (row < numberOfRowsToConsider)) {
            assertEquals(message, i1.next(), i2.next(), es1.getAttributes().allAttributes(), es2.getAttributes().allAttributes(), row);
            row++;
        }
    }
    
    /**
     * Test two numerical matrices for equality. This contains tests about the number of columns and rows, as well as column&row names and if
     * the matrices are marked as symmetrical and if every value within the matrix is equal.
     *  
	 * @param message		message to display if an error occurs
	 * @param expected		expected matrix
	 * @param actual		actual matrix
     */
    public static void assertEquals(String message, NumericalMatrix expected, NumericalMatrix actual) {
    	
    	int expNrOfCols = expected.getNumberOfColumns();
    	int actNrOfCols = actual.getNumberOfColumns();
    	assertEquals(message + " (column number is not equal)", expNrOfCols, actNrOfCols);
    	
    	int expNrOfRows = expected.getNumberOfRows();
    	int actNrOfRows = actual.getNumberOfRows();
    	assertEquals(message + " (row number is not equal)", expNrOfRows, actNrOfRows);
    	
    	int cols = expNrOfCols; 
    	int rows = expNrOfRows;
    	
    	for( int col=0; col<cols; col++ ) {
    		String expectedColName = expected.getColumnName(col);
    		String actualColName = actual.getColumnName(col);
    		assertEquals(message + " (column name at index "+col+" is not equal)", expectedColName, actualColName );
    	}
    	
    	for( int row=0; row<rows; row++ ) {
    		String expectedRowName = expected.getRowName(row);
    		String actualRowName = actual.getRowName(row);
    		assertEquals(message + " (row name at index "+row+" is not equal)", expectedRowName, actualRowName );
    	}
    	
    	assertEquals(message + " (matrix symmetry is not equal)", expected.isSymmetrical(), actual.isSymmetrical());
    	
    	for( int row=0; row<rows; row++ ) {
    		for( int col=0; col<cols; col++ ) {
    			
    			double expectedVal = expected.getValue(row, col);
    			double actualVal = actual.getValue(row, col);
    			assertEquals(message + " (value at row "+row+" and column "+col+" is not equal)", expectedVal, actualVal );
    			
    		}
    	}
    	
    }
    
    /**
     * Tests the two average vectors for equality by testing the size and each averagable.
     * 
	 * @param message		message to display if an error occurs
	 * @param expected		expected vector
	 * @param actual		actual vector
     */
    public static void assertEquals(String message, AverageVector expected, AverageVector actual) {
    	int expSize = expected.getSize();
    	int actSize = actual.getSize();
    	assertEquals(message + " (size of the average vector is not equal)", expSize, actSize);
    	int size = expSize;

    	for( int i=0; i<size; i++ ) {
    		RapidAssert.assertEquals(message, expected.getAveragable(i), actual.getAveragable(i));
    	}
    }
    
    /**
     * Tests the two performance vectors for equality by testing the size, the criteria names, the main criterion and each criterion.
     * 
	 * @param message		message to display if an error occurs
	 * @param expected		expected vector
	 * @param actual		actual vector
     */
    public static void assertEquals(String message, PerformanceVector expected, PerformanceVector actual) {
    	int expSize = expected.getSize();
    	int actSize = actual.getSize();
    	assertEquals(message + " (size of the performance vector is not equal)", expSize, actSize);
    	int size = expSize;
    	
    	RapidAssert.assertArrayEquals(message, expected.getCriteriaNames(), actual.getCriteriaNames());
    	RapidAssert.assertEquals(message, expected.getMainCriterion(), actual.getMainCriterion());
    	
    	for( int i=0; i<size; i++ ) {
    		RapidAssert.assertEquals(message, expected.getCriterion(i), actual.getCriterion(i));
    	}
    }
    
    
    /**
     * Tests for equality by testing all averages, standard deviation and variances.
     * 
	 * @param message		message to display if an error occurs
	 * @param expected		expected averagable
	 * @param actual		actual averagable
     */
    public static void assertEquals(String message, Averagable expected, Averagable actual) {
    	
    	assertEquals(message + " (average is not equal)", expected.getAverage(), actual.getAverage());
    	assertEquals(message + " (makro average is not equal)", expected.getMakroAverage(), actual.getMakroAverage());
    	assertEquals(message + " (mikro average is not equal)", expected.getMikroAverage(), actual.getMikroAverage());
    	assertEquals(message + " (average count is not equal)", expected.getAverageCount(), actual.getAverageCount());
    	assertEquals(message + " (makro standard deviation is not equal)", expected.getMakroStandardDeviation(), actual.getMakroStandardDeviation());
    	assertEquals(message + " (mikro standard deviation is not equal)", expected.getMikroStandardDeviation(), actual.getMikroStandardDeviation());
    	assertEquals(message + " (standard deviation is not equal)", expected.getStandardDeviation(), actual.getStandardDeviation());
    	assertEquals(message + " (makro variance is not equal)", expected.getMakroVariance(), actual.getMakroVariance());
    	assertEquals(message + " (mikro variance is not equal)", expected.getMikroVariance(), actual.getMikroVariance());
    	assertEquals(message + " (variance is not equal)", expected.getVariance(), actual.getVariance());
    	
    }

    /**
	 * Tests for equality by testing all averages, standard deviation and variances, as well as the fitness, max fitness 
	 * and example count.
	 *  
	 * @param message		message to display if an error occurs
	 * @param expected		expected criterion
	 * @param actual		actual criterion
     */
    public static void assertEquals(String message, PerformanceCriterion expected, PerformanceCriterion actual) {
    	RapidAssert.assertEquals(message , (Averagable)expected, (Averagable)actual);
    	assertEquals(message + " (fitness is not equal)", expected.getFitness(), actual.getFitness());
    	assertEquals(message + " (max fitness is not equal)", expected.getMaxFitness(), actual.getMaxFitness());
    	assertEquals(message + " (example count is not equal)", expected.getExampleCount(), actual.getExampleCount());
    }
    
    /**
     * Tests the two examples by testing the value of the examples for every given attribute. 
     * This method is sensitive to the attribute ordering.
     * 
	 * @param message		message to display if an error occurs
	 * @param expected		expected value
	 * @param actual		actual value
     * @param expectedAttributesToConsider	an iterator for the attributes to consider for the expected example
     * @param actualAttributesToConsider	an iterator for the attributes to consider for the actual example
     * @param row			current row of the example set
     */
    private static void assertEquals(String message, Example expected, Example actual, 
    		Iterator<Attribute> expectedAttributesToConsider, Iterator<Attribute> actualAttributesToConsider, int row) {
        while (expectedAttributesToConsider.hasNext() && actualAttributesToConsider.hasNext()) {
            Attribute a1 = expectedAttributesToConsider.next();
            Attribute a2 = actualAttributesToConsider.next();
            if (!a1.getName().equals(a2.getName())) {
                // this should have been detected by previous checks already
                throw new AssertionFailedError("Attribute ordering does not match: " + a1.getName() + "," + a2.getName());
            }
            if (a1.isNominal()) {
                Assert.assertEquals(message + " (example " + (row + 1) + ", nominal attribute value " + a1.getName() + ")", expected.getNominalValue(a1), actual.getNominalValue(a2));
            } else {
                Assert.assertEquals(message + " (example " + (row + 1) + ", numerical attribute value " + a1.getName() + ")", expected.getValue(a1), actual.getValue(a2), DELTA);
            }
        }
    }

    /**
     * Tests if all attributes are equal. This method is sensitive to the attribute ordering.
     * 
	 * @param message		message to display if an error occurs
	 * @param expected		expected value
	 * @param actual		actual value
     */
    public static void assertEquals(String message, Attributes expected, Attributes actual) {
        Assert.assertEquals(message + " (number of attributes)", expected.allSize(), actual.allSize());
        Iterator<AttributeRole> i = expected.allAttributeRoles();
        Iterator<AttributeRole> j = expected.allAttributeRoles();
        while (i.hasNext()) {
            AttributeRole r1 = i.next();
            AttributeRole r2 = j.next();
            assertEquals(message, r1, r2);
        }
    }


    /**
     * Tests all objects in the array.
     * 
     * @param expected	array with expected objects
     * @param actual	array with actual objects
     */
    public static void assertArrayEquals(String message, Object[] expected, Object[] actual) {
        if (expected == null) {
            junit.framework.Assert.assertEquals((Object) null, actual);
            return;
        }
        if (actual == null) {
            throw new AssertionFailedError(message + " (expected " + expected.toString() + " , but is null)");
        }
        junit.framework.Assert.assertEquals(message + " (array length is not equal)", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            junit.framework.Assert.assertEquals(message, expected[i], actual[i]);
        }
    }
    
    /**
     * Tests all objects in the array.
     * 
	 * @param message		message to display if an error occurs
     * @param expected	array with expected objects
     * @param actual	array with actual objects
     */
    public static void assertArrayEquals(Object[] expected, Object[] actual) {
    	assertArrayEquals("", expected, actual);
    }
    
    /**
     * Tests if both list of ioobjects are equal.
     * 
	 * @param expected		expected value
	 * @param actual		actual value
     */
	public static void assertEquals( List<IOObject> expected, List<IOObject> actual ) {
		
		assertEquals("Number of connected output ports in the process is not equal with the number of ioobjects contained in the same folder with the format 'processname-expected-port-1', 'processname-expected-port-2', ...", 
				expected.size(), actual.size());
		
		Iterator<IOObject> expectedIter = expected.iterator();
		Iterator<IOObject> actualIter = actual.iterator();
		
		while( expectedIter.hasNext() && actualIter.hasNext() )  {
			IOObject expectedIOO = expectedIter.next();
			IOObject actualIOO = actualIter.next();

			if( expectedIOO instanceof ExampleSet && actualIOO instanceof ExampleSet )
				RapidAssert.assertEquals("ExampleSets are not equal", (ExampleSet)expectedIOO, (ExampleSet)actualIOO, -1);
			
			if( expectedIOO instanceof NumericalMatrix && actualIOO instanceof NumericalMatrix )
				RapidAssert.assertEquals("Numerical matrices are not equal", (NumericalMatrix) expectedIOO, (NumericalMatrix) actualIOO);
			
			if( expectedIOO instanceof PerformanceVector && actualIOO instanceof PerformanceVector )
				RapidAssert.assertEquals("Performance vectors are not equal", (PerformanceVector) expectedIOO, (PerformanceVector) actualIOO);
			else if( expectedIOO instanceof AverageVector && actualIOO instanceof AverageVector ) 
				RapidAssert.assertEquals("Average vectors are not equals", (AverageVector) expectedIOO, (AverageVector) actualIOO);
				

		}
		
	}
	
}


