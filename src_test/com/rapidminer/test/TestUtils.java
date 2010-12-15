package com.rapidminer.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.XMLException;

/**
 * 
 * @author Simon Fischer
 * 
 */
public class TestUtils {

	private static final String TEST_REPOSITORY_NAME = "_TestRepository";
	private static boolean initialized = false;
	private static String PROPERTY_TEST_RESOLVE_DIRECTORY = "rapidminer.test.resolvedir";;

	public static void initRapidMiner() {
		if (!initialized) {
			File testConfigFile = ParameterService.getUserConfigFile("test.properties");
			if (testConfigFile.exists()) {
				Properties testProps = new Properties();
				FileInputStream in;
				try {
					in = new FileInputStream(testConfigFile);
					testProps.load(in);
					in.close();
				} catch (IOException e) {
					throw new RuntimeException("Failed to read " + testConfigFile);
				}
				String resolvedir = testProps.getProperty(PROPERTY_TEST_RESOLVE_DIRECTORY);
				if (resolvedir != null) {
					System.setProperty(PROPERTY_TEST_RESOLVE_DIRECTORY, resolvedir);
				}
			}

			String resolvedir = System.getProperty(PROPERTY_TEST_RESOLVE_DIRECTORY);
			if (resolvedir == null) {
				throw new RuntimeException("In order to run tests, please define system property rapidminer.test.resolvedir in your run configuration to point to the test repository on rapid share (R&D/TestCases/) or create file " + testConfigFile + ".");
			}

			RapidMiner.setExecutionMode(ExecutionMode.TEST);
			RapidMiner.init();

			try {
				LocalRepository repos = new LocalRepository(TEST_REPOSITORY_NAME, new File(resolvedir + File.separatorChar + "Repository")) {
					@Override
					public boolean shouldSave() {
						return false;
					}
				};
				RepositoryManager.getInstance(null).addRepository(repos);
			} catch (RepositoryException e) {
				throw new RuntimeException("Failed to intialize test repository");
			}

			initialized = true;
		}
	}

	public static void assertEqualsNaN(String message, double expected, double actual) {
		if (Double.isNaN(expected)) {
			if (!Double.isNaN(actual)) {
				throw new AssertionFailedError(message + " expected: <" + expected + "> but was: <" + actual + ">");
			}
		} else {
			Assert.assertEquals(message, expected, actual, 0.000000001);
		}
	}

	public static void assertEquals(String message, AttributeRole r1, AttributeRole r2) {
		Assert.assertEquals(message + " (attribute role)", r1.getSpecialName(), r2.getSpecialName());
		Attribute a1 = r1.getAttribute();
		Attribute a2 = r2.getAttribute();
		assertEquals(message, a1, a2);
	}

	public static void assertEquals(String message, Attribute a1, Attribute a2) {
		Assert.assertEquals(message + " (attribute name)", a1.getName(), a2.getName());
		Assert.assertEquals(message + " (attribute type)", a1.getValueType(), a2.getValueType());
		Assert.assertEquals(message + " (attribute block type)", a1.getBlockType(), a2.getBlockType());
		Assert.assertEquals(message + " (default value)", a1.getDefault(), a2.getDefault());
		if (a1.isNominal()) {
			assertEquals(message + " (nominal mapping)", a1.getMapping(), a2.getMapping());
		}
	}

	public static void assertEquals(String message, NominalMapping mapping1, NominalMapping mapping2) {
		Assert.assertEquals(message + " (nominal mapping size)", mapping1.size(), mapping2.size());
		List<String> v1 = mapping1.getValues();
		List<String> v2 = mapping2.getValues();
		Assert.assertEquals(message + " (nominal values)", v1, v2);
		if (v1 != null) { // v2 also != null
			for (String value : v1) {
				Assert.assertEquals(message + " (index of nominal value '" + value + "')", mapping1.getIndex(value), mapping2.getIndex(value));
			}
		}
	}

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

	private static void assertEquals(String message, Example e1, Example e2, Iterator<Attribute> atts1, Iterator<Attribute> atts2, int row) {
		while (atts1.hasNext() && atts2.hasNext()) {
			Attribute a1 = atts1.next();
			Attribute a2 = atts2.next();
			if (!a1.getName().equals(a2.getName())) {
				// this should have been detected by previous checks already
				throw new AssertionFailedError("Attribute ordering does not match: " + a1.getName() + "," + a2.getName());
			}
			if (a1.isNominal()) {
				Assert.assertEquals(message + " (example " + (row + 1) + ", nominal attribute value " + a1.getName() + ")", e1.getNominalValue(a1), e2.getNominalValue(a2));
			} else {
				Assert.assertEquals(message + " (example " + (row + 1) + ", numerical attribute value " + a1.getName() + ")", e1.getValue(a1), e2.getValue(a2));
			}
		}
	}

	/** This method is sensitive to the attribute ordering. */
	public static void assertEquals(String message, Attributes attributes1, Attributes attributes2) {
		Assert.assertEquals(message + " (number of attributes)", attributes1.allSize(), attributes2.allSize());
		Iterator<AttributeRole> i = attributes1.allAttributeRoles();
		Iterator<AttributeRole> j = attributes1.allAttributeRoles();
		while (i.hasNext()) {
			AttributeRole r1 = i.next();
			AttributeRole r2 = j.next();
			assertEquals(message, r1, r2);
		}
	}

	public static IOContainer executeProcessFromTestRepository(String relativeRepositoryLocation) throws IOException, XMLException, OperatorException {
		ProcessLocation loc = new RepositoryProcessLocation(new RepositoryLocation(
				RepositoryLocation.REPOSITORY_PREFIX + TEST_REPOSITORY_NAME +
						relativeRepositoryLocation));
		Process process = loc.load(null);
		return process.run();
	}

	public static IOObject getIOObjectFromTestRepository(String relativeObjectLocation) throws MalformedRepositoryLocationException, RepositoryException {
		RepositoryLocation loc = new RepositoryLocation(
				RepositoryLocation.REPOSITORY_PREFIX + TEST_REPOSITORY_NAME +
						relativeObjectLocation);
		return ((IOObjectEntry) loc.locateEntry()).retrieveData(null);
	}

	public static void assertArrayEquals(Object[] expected, Object[] actual) {
		if (expected == null) {
			junit.framework.Assert.assertEquals((Object) null, actual);
			return;
		}
		if (actual == null) {
			throw new AssertionFailedError("Expected " + expected.toString() + " , but is null.");
		}
		junit.framework.Assert.assertEquals("Array length", expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			junit.framework.Assert.assertEquals(expected[i], actual[i]);
		}
	}
}
