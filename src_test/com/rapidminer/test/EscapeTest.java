package com.rapidminer.test;

import static com.rapidminer.test.utils.RapidAssert.assertArrayEquals;
import static junit.framework.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;

/** Tests {@link Tools#escape(String, char, char[])} and {@link Tools#unescape(String, char, char[], char)}.
 * 
 * @author Simon Fischer
 *
 */
public class EscapeTest {

	@Test
	public void testEscape() {
		assertEquals("test\\\\tost", Tools.escape("test\\tost", '\\', new char[0]));
		assertEquals("test\\\ntost", Tools.escape("test\ntost", '\\', new char[] {'\n'}));		
		assertEquals("one\\.two\\.three\\.\\.five", Tools.escape("one.two.three..five", '\\', new char[] {'.'}));
	}
	
	@Test
	public void testUnescape() {
		List<String> result = new LinkedList<String>();
		result.add("test\\tost");
		assertEquals(result, Tools.unescape("test\\\\tost", '\\', new char[] {'\\'}, (char)-1));
		
		result = new LinkedList<String>();
		result.add("line1");
		result.add("line.two");
		result.add("");
		result.add("last.line");
		assertEquals(result, Tools.unescape("line1.line\\.two..last\\.line", '\\', new char[0], '.'));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testException() {
		Tools.unescape("illegal\\escape character", '\\', new char[] {'a', 'b'}, (char)-1);
	}
	
	@Test
	public void testParameterTypeTuple() {
		//final char internalSeparator = Parameters.PAIR_SEPARATOR;
		final char internalSeparator = '.';
		assertArrayEquals(new String[] { "fi"+internalSeparator+"rst", "sec"+internalSeparator+"ond" }, ParameterTypeTupel.transformString2Tupel("fi\\"+internalSeparator+"rst"+internalSeparator+"sec\\"+internalSeparator+"ond"));
		
		assertEquals("fi\\"+internalSeparator+"rst"+internalSeparator+"sec\\"+internalSeparator+"ond", ParameterTypeTupel.transformTupel2String("fi"+internalSeparator+"rst", "sec"+internalSeparator+"ond"));
		assertEquals("fi\\"+internalSeparator+"rst"+internalSeparator+"sec\\"+internalSeparator+"ond", ParameterTypeTupel.transformTupel2String(new String[] { "fi"+internalSeparator+"rst", "sec"+internalSeparator+"ond" }));
		assertEquals("fi\\"+internalSeparator+"rst"+internalSeparator+"sec\\"+internalSeparator+"ond", ParameterTypeTupel.transformTupel2String(new Pair<String,String>("fi"+internalSeparator+"rst", "sec"+internalSeparator+"ond")));
	}
	
	@Test
	public void testParameterTypeEnumeration() {
		//final char internalRecordSeparator = Parameters.RECORD_SEPARATOR;
		final char internalRecordSeparator = ',';
		assertArrayEquals(
				new String[] { 
						"fi"+internalRecordSeparator+"rst",
						"sec"+internalRecordSeparator+"ond",
						"third"+internalRecordSeparator+"" }, 
				ParameterTypeEnumeration.transformString2Enumeration("fi\\"+internalRecordSeparator+"rst"+internalRecordSeparator+"sec\\"+internalRecordSeparator+"ond"+internalRecordSeparator+"third\\"+internalRecordSeparator+""));
		List<String> enumeration = new LinkedList<String>();
		enumeration.add("fi"+internalRecordSeparator+"rst");
		enumeration.add("sec"+internalRecordSeparator+"ond");
		enumeration.add("third"+internalRecordSeparator+"");
		assertEquals("fi\\"+internalRecordSeparator+"rst"+internalRecordSeparator+"sec\\"+internalRecordSeparator+"ond"+internalRecordSeparator+"third\\"+internalRecordSeparator+"", ParameterTypeEnumeration.transformEnumeration2String(enumeration));
	}
}
