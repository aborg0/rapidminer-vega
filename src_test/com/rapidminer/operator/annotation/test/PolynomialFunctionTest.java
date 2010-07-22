package com.rapidminer.operator.annotation.test;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import com.rapidminer.operator.annotation.PolynomialFunction;

public class PolynomialFunctionTest {

	@Test
	public void testLinearFunction() {
		PolynomialFunction f = PolynomialFunction.makeLinearFunction(2);
		assertEquals(70, f.evaluate(5, 7));
	}
	
	@Test
	public void testPolynomialFunction() {
		PolynomialFunction f = new PolynomialFunction(10, 2, 3);
		assertEquals(85750, f.evaluate(5, 7));
	}

	@Test
	public void testPolyPlusLogFunction() {
		PolynomialFunction f = new PolynomialFunction(10, 2, 1, 3, 1);
		// 10 * 5^2*ln(5) * 7^3*ln(7) * 10
		assertEquals((long)268553.69946285250055529643, f.evaluate(5, 7));
	}

}
