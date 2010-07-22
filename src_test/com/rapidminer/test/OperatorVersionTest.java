package com.rapidminer.test;

import static junit.framework.Assert.*;
import org.junit.Test;

import com.rapidminer.operator.OperatorVersion;

/**
 * 
 * @author Simon Fischer
 *
 */
public class OperatorVersionTest {

	@Test
	public void testParse() {
		OperatorVersion reference = new OperatorVersion(5,1,2);
		assertEquals(reference, new OperatorVersion("5.1.2"));		
	}

	@Test
	public void testBeta() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertEquals(reference, new OperatorVersion("5.1.2beta"));		
	}
	
	@Test
	public void testZero() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertEquals(reference, new OperatorVersion("5.1.002"));
		assertEquals(reference, new OperatorVersion("5.01.2"));
		assertEquals(reference, new OperatorVersion("05.1.2"));
	}
	
	@Test
	public void testCompare1() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertTrue(reference.compareTo(new OperatorVersion("5.1.0")) > 0);
		assertTrue(reference.compareTo(new OperatorVersion("5.0.3")) > 0);
		assertTrue(reference.compareTo(new OperatorVersion("4.9.0")) > 0);
	}
	
	@Test
	public void testCompare2() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertTrue(reference.compareTo(new OperatorVersion("5.1.2")) == 0);
		assertTrue(reference.compareTo(new OperatorVersion("5.1.3")) < 0);
		assertTrue(reference.compareTo(new OperatorVersion("5.2.2")) < 0);
		assertTrue(reference.compareTo(new OperatorVersion("6.0.0")) < 0);
	}
	
	@Test
	public void testComparator1() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertTrue(OperatorVersion.COMPARATOR.compare(reference, new OperatorVersion("5.1.0")) > 0);
		assertTrue(OperatorVersion.COMPARATOR.compare(reference, new OperatorVersion("5.0.3")) > 0);
		assertTrue(OperatorVersion.COMPARATOR.compare(reference, new OperatorVersion("4.9.0")) > 0);
	}
	
	@Test
	public void testComparator2() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertTrue(OperatorVersion.COMPARATOR.compare(reference, new OperatorVersion("5.1.2")) == 0);
		assertTrue(OperatorVersion.COMPARATOR.compare(reference, new OperatorVersion("5.1.3")) < 0);
		assertTrue(OperatorVersion.COMPARATOR.compare(reference, new OperatorVersion("5.2.2")) < 0);
		assertTrue(OperatorVersion.COMPARATOR.compare(reference, new OperatorVersion("6.0.0")) < 0);
	}
}
