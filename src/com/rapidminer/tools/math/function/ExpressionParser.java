/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.math.function;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.nfunk.jep.JEP;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;
import org.nfunk.jep.type.Complex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.MacroHandler;
import com.rapidminer.Process;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.generator.GenerationException;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.filter.ChangeAttributeName;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.function.expressions.Average;
import com.rapidminer.tools.math.function.expressions.Constant;
import com.rapidminer.tools.math.function.expressions.LogarithmDualis;
import com.rapidminer.tools.math.function.expressions.Maximum;
import com.rapidminer.tools.math.function.expressions.Minimum;
import com.rapidminer.tools.math.function.expressions.ParameterValue;
import com.rapidminer.tools.math.function.expressions.Signum;
import com.rapidminer.tools.math.function.text.CharAt;
import com.rapidminer.tools.math.function.text.Compare;
import com.rapidminer.tools.math.function.text.Concat;
import com.rapidminer.tools.math.function.text.Contains;
import com.rapidminer.tools.math.function.text.EndsWith;
import com.rapidminer.tools.math.function.text.Equals;
import com.rapidminer.tools.math.function.text.IndexOf;
import com.rapidminer.tools.math.function.text.Length;
import com.rapidminer.tools.math.function.text.LowerCase;
import com.rapidminer.tools.math.function.text.Matches;
import com.rapidminer.tools.math.function.text.ParseNumber;
import com.rapidminer.tools.math.function.text.Prefix;
import com.rapidminer.tools.math.function.text.Replace;
import com.rapidminer.tools.math.function.text.ReplaceRegex;
import com.rapidminer.tools.math.function.text.StartsWith;
import com.rapidminer.tools.math.function.text.Substring;
import com.rapidminer.tools.math.function.text.Suffix;
import com.rapidminer.tools.math.function.text.Trim;
import com.rapidminer.tools.math.function.text.UpperCase;

/**
 * <p>This class can be used as expression parser in order to generate new attributes.
 * The parser constructs new attributes from the attributes of the input
 * example set.</p>
 * 
 * <p>The following <em>operators</em> are supported:
 * <ul>
 * <li>Addition: +</li>
 * <li>Subtraction: -</li>
 * <li>Multiplication: *</li>
 * <li>Division: /</li>
 * <li>Power: ^</li>
 * <li>Modulus: %</li> 
 * <li>Less Than: &lt;</li>
 * <li>Greater Than: &gt;</li>
 * <li>Less or Equal: &lt;=</li>
 * <li>More or Equal: &gt;=</li>
 * <li>Equal: ==</li>
 * <li>Not Equal: !=</li>
 * <li>Boolean Not: !</li>
 * <li>Boolean And: &&</li>
 * <li>Boolean Or: ||</li>
 * </ul>
 * </p>
 * 
 * <p>The following <em>log and exponential functions</em> are supported:
 * <ul>
 * <li>Natural Logarithm: ln(x)</li>
 * <li>Logarithm Base 10: log(x)</li>
 * <li>Logarithm Dualis (Base 2): ld(x)</li>
 * <li>Exponential (e^x): exp(x)</li>
 * <li>Power: pow(x,y)</li>
 * </ul>
 * </p>
 * 
 * <p>The following <em>trigonometric functions</em> are supported:
 * <ul>
 * <li>Sine: sin(x)</li>
 * <li>Cosine: cos(x)</li>
 * <li>Tangent: tan(x)</li>
 * <li>Arc Sine: asin(x)</li>
 * <li>Arc Cosine: acos(x)</li>
 * <li>Arc Tangent: atan(x)</li>
 * <li>Arc Tangent (with 2 parameters): atan2(x,y)</li>
 * <li>Hyperbolic Sine: sinh(x)</li>
 * <li>Hyperbolic Cosine: cosh(x)</li>
 * <li>Hyperbolic Tangent: tanh(x)</li>
 * <li>Inverse Hyperbolic Sine: asinh(x)</li></li>
 * <li>Inverse Hyperbolic Cosine: acosh(x)</li></li>
 * <li>Inverse Hyperbolic Tangent: atanh(x)</li></li>
 * </ul>
 * </p>
 * 
 * <p>The following <em>statistical functions</em> are supported:
 * <ul>
 * <li>Round: round(x)</li>
 * <li>Round to p decimals: round(x,p)</li>
 * <li>Floor: floor(x)</li>
 * <li>Ceiling: ceil(x)</li>
 * </ul>
 * </p>
 *
 * <p>The following <em>miscellaneous functions</em> are supported:
 * <ul>
 * <li>Average: avg(x,y,z...)</li>
 * <li>Minimum: min(x,y,z...)</li>
 * <li>Maximum: max(x,y,z...)</li>
 * </ul>
 * </p>
 * 
 * <p>The following <em>text functions</em> are supported:
 * <ul>
 * <li>Number to String: str(x)</li>
 * <li>String to Number: parse(text)</li>
 * <li>Substring: cut(text, start, length)</li>
 * <li>Concatenation (also possible by &quot+&quot;): concat(text1, text2, text3...)</li>
 * <li>Replace: replace(text, what, by)</li>
 * <li>Replace All: replaceAll(text, what, by)</li> 
 * <li>To lower case: lower(text)</li>
 * <li>To upper case: upper(text)</li>
 * <li>First position of string in text: index(text, string)</li>
 * <li>Length: length(text)</li>
 * <li>Character at position pos in text: char(text, pos)</li>
 * <li>Compare: compare(text1, text2)</li>
 * <li>Contains string in text: contains(text, string)</li>
 * <li>Equals: equals(text1, text2)</li>
 * <li>Starts with string: starts(text, string)</li>
 * <li>Ends with string: ends(text, string)</li>
 * <li>Matches with regular expression exp: matches(text, exp)</li>
 * <li>Suffix of length: suffix(text, length)</li>
 * <li>Prefix of length: prefix(text, length)</li>
 * <li>Trim (remove leading and trailing whitespace): trim(text)</li>
 * </ul>
 * </p> 
 * 
 * <p>The following <em>miscellaneous functions</em> are supported:
 * <ul>
 * <li>If-Then-Else: if(cond,true-evaluation, false-evaluation)</li>
 * <li>Absolute: abs(x)</li>
 * <li>Constant: const(x)</li>
 * <li>Square Root: sqrt(x)</li>
 * <li>Signum (delivers the sign of a number): sgn(x)</li>
 * <li>Random Number (between 0 and 1): rand()</li>
 * <li>Modulus (x % y): mod(x,y)</li>
 * <li>Sum of k Numbers: sum(x,y,z...)</li>
 * <li>Binomial Coefficients: binom(n, i)</li>
 * <li>Retrieving parameter value: param(operator name, parameter name)</li>
 * </ul>
 * </p> 
 * 
 * <p>Beside those operators and functions, this operator also supports the constants
 * pi and e if this is indicated by the corresponding parameter (default: true). You can
 * also use strings in formulas (for example in a conditioned if-formula) but the string
 * values have to be enclosed in double quotes.</p>
 *  
 * <p>Please note that there are some restrictions for the attribute names in order
 * to let this operator work properly:
 * <ul>
 * <li>If the standard constants are usable, attribute names with names like &quot;e&quot; or 
 * &quot;pi&quot; are not allowed.</li>
 * <li>Attribute names with function or operator names are also not allowed.</li>
 * <li>Attribute names containing parentheses are not allowed.</li>
 * </ul>
 * If these conditions are not fulfilled, the names must be changed beforehand, for example
 * with the {@link ChangeAttributeName} operator.
 * </p>
 * 
 * <p><br/><em>Examples:</em><br/>
 * a1+sin(a2*a3)<br/>
 * if (att1>5, att2*att3, -abs(att1))<br/>
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class ExpressionParser {

	private final JEP parser = new JEP();

	public ExpressionParser(boolean useStandardConstants) {
		parser.addStandardFunctions();
		if (useStandardConstants)
			parser.addStandardConstants();

		addCustomFunctions(parser);

		parser.setAllowUndeclared(false);
		parser.setImplicitMul(false);		
	}

	/**
	 * This constructor allows additional functions if called within a 
	 * process.
	 */
	public ExpressionParser(boolean useStandardConstants, Process process) {
		this(useStandardConstants);
		if (process != null) {
			parser.addFunction("param", new ParameterValue(process));
		}
	}

	private void addCustomFunctions(JEP parser) {
		parser.addFunction("const", new Constant());
		
		parser.addFunction("avg", new Average());
		parser.addFunction("min", new Minimum());
		parser.addFunction("max", new Maximum());
		parser.addFunction("ld", new LogarithmDualis());
		parser.addFunction("sgn", new Signum());
		
		// text functions
		parser.addFunction("parse", new ParseNumber());
		parser.addFunction("cut", new Substring());
		parser.addFunction("concat", new Concat());
		parser.addFunction("replace", new Replace());
		parser.addFunction("replaceAll", new ReplaceRegex());
		parser.addFunction("lower", new LowerCase());
		parser.addFunction("upper", new UpperCase());
		parser.addFunction("index", new IndexOf());
		parser.addFunction("length", new Length());
		parser.addFunction("char", new CharAt());
		parser.addFunction("compare", new Compare());
		parser.addFunction("equals", new Equals());
		parser.addFunction("contains", new Contains());
		parser.addFunction("starts", new StartsWith());
		parser.addFunction("ends", new EndsWith());
		parser.addFunction("matches", new Matches());
		parser.addFunction("prefix", new Prefix());
		parser.addFunction("suffix", new Suffix());
		parser.addFunction("trim", new Trim());
	}

	public void addMacro(MacroHandler macroHandler, String name, String function) throws GenerationException {
		// parse expression
		parser.parseExpression(function);


		//check for errors
		if (parser.hasError()) {
			throw new GenerationException(parser.getErrorInfo());
		}

		// create the new attribute from the delivered type 
		Object result = parser.getValueAsObject();

		//check for errors
		if (parser.hasError()) {
			throw new GenerationException(parser.getErrorInfo());
		}

		// set result as macro
		if (result != null) {
			try {
				macroHandler.addMacro(name, Tools.formatIntegerIfPossible(Double.parseDouble(result.toString())));	
			} catch (NumberFormatException e) {
				macroHandler.addMacro(name, result.toString());	
			}
		}		
	}

	public void addAttributeMetaData(ExampleSetMetaData emd, String name, String function) {
		parser.setAllowUndeclared(true);

		// parse expression
		parser.parseExpression(function);

		//check for errors
		if (!parser.hasError()) {

			// derive all used variables
			SymbolTable symbolTable = parser.getSymbolTable();
			Map<String, AttributeMetaData> name2attributes = new HashMap<String, AttributeMetaData>();
			for (Object variableObj : symbolTable.values()) {
				Variable variable = (Variable)variableObj;//symbolTable.getVar(variableName.toString());
				if (!variable.isConstant()) {
					AttributeMetaData attribute = emd.getAttributeByName(variable.getName());
					if (attribute != null) {
						name2attributes.put(variable.getName(), attribute);
						if (attribute.isNominal()) {
							parser.addVariable(attribute.getName(), "");
						} else {
							parser.addVariable(attribute.getName(), Double.NaN);
						}
					}
				}
			}
			if (!parser.hasError()) {
				// create the new attribute from the delivered type 
				Object result = parser.getValueAsObject();

				if (!parser.hasError()) {
					AttributeMetaData newAttribute = null;
					if (result instanceof Boolean) {
						newAttribute = new AttributeMetaData(name, Ontology.BINOMINAL);
						HashSet<String> values = new HashSet<String>();
						values.add("false");
						values.add("true");
						newAttribute.setValueSet(values, SetRelation.EQUAL);
					} else if (result instanceof Number) {
						newAttribute = new AttributeMetaData(name, Ontology.REAL);
					} else if (result instanceof Complex) {
						newAttribute = new AttributeMetaData(name, Ontology.REAL);
					} else {
						newAttribute = new AttributeMetaData(name, Ontology.NOMINAL);
					}
					emd.addAttribute(newAttribute);
				} else {
					emd.addAttribute(new AttributeMetaData(name, Ontology.ATTRIBUTE_VALUE));	
				}
			} else {
				emd.addAttribute(new AttributeMetaData(name, Ontology.ATTRIBUTE_VALUE));
			}
		} else {
			emd.addAttribute(new AttributeMetaData(name, Ontology.ATTRIBUTE_VALUE));
		}
	}

	/** Iterates over the {@link ExampleSet}, interprets attributes as variables, evaluates
	 *  the function and creates a new attribute with the given name that takes the expression's value.
	 *  The type of the attribute depends on the expression type and is {@link Ontology#NOMINAL} for strings,
	 *  {@link Ontology#NUMERICAL} for reals and complex numbers and {@link Ontology#BINOMINAL} with values
	 *  &quot;true&quot; and &quot;false&quot; for booleans.
	 *  @return The generated attribute
	 * */
	public Attribute addAttribute(ExampleSet exampleSet, String name, String function) throws GenerationException {

		parser.setAllowUndeclared(true);

		// parse expression
		parser.parseExpression(function);


		//check for errors
		if (parser.hasError()) {
			throw new GenerationException(function + ": " + parser.getErrorInfo());
		}

		// derive all used variables
		SymbolTable symbolTable = parser.getSymbolTable();
		Map<String, Attribute> name2attributes = new HashMap<String, Attribute>();
		for (Object variableObj : symbolTable.values()) {
			Variable variable = (Variable)variableObj;//symbolTable.getVar(variableName.toString());
			if (!variable.isConstant()) {
				Attribute attribute = exampleSet.getAttributes().get(variable.getName());
				if (attribute == null) {
					throw new GenerationException("No such attribute: '" + variable.getName()+"'");
				} else {
					name2attributes.put(variable.getName(), attribute);
					// retrieve test example with real values (needed to compliance checking!)
					if (exampleSet.size() > 0) {
						Example example = exampleSet.iterator().next();
						if (attribute.isNominal()) {
							parser.addVariable(attribute.getName(), example.getValueAsString(attribute));
						} else {
							parser.addVariable(attribute.getName(), example.getValue(attribute));
						}
					} else {
						// nothing will be done later: no compliance to data must be met
						if (attribute.isNominal()) {
							parser.addVariable(attribute.getName(), "");
						} else {
							parser.addVariable(attribute.getName(), Double.NaN);
						}
					}
				}
			}
		}

		if (parser.hasError()) {
			throw new GenerationException(parser.getErrorInfo());
		}

		// create the new attribute from the delivered type 
		Object result = parser.getValueAsObject();

		if (parser.hasError()) {
			throw new GenerationException(parser.getErrorInfo());
		}

		Attribute newAttribute = null;
		if (result instanceof Boolean) {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.BINOMINAL);
			newAttribute.getMapping().mapString("false");
			newAttribute.getMapping().mapString("true");
		} else if (result instanceof Number) {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.REAL);
		} else if (result instanceof Complex) {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.REAL);
		} else {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.NOMINAL);
		}

		// set construction description
		newAttribute.setConstruction(function);

		// add new attribute to table and example set
		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);


		// create attribute of correct type and all values
		for (Example example : exampleSet) {

			// assign variable values
			for (Map.Entry<String, Attribute> entry : name2attributes.entrySet()) {
				String variableName = entry.getKey();
				Attribute attribute = entry.getValue();
				if (attribute.isNominal())
					parser.setVarValue(variableName, example.getValueAsString(attribute));
				else
					parser.setVarValue(variableName, example.getValue(attribute));
			}

			// calculate result
			result = parser.getValueAsObject();

			//check for errors
			if (parser.hasError()) {
				throw new GenerationException(parser.getErrorInfo());
			}

			// store result
			if (result instanceof Boolean) {
				if ((Boolean)result) {
					example.setValue(newAttribute, newAttribute.getMapping().mapString("true"));
				} else {
					example.setValue(newAttribute, newAttribute.getMapping().mapString("false"));
				}				
			} else if (result instanceof Number) {
				example.setValue(newAttribute, ((Number)result).doubleValue());
			} else if (result instanceof Complex) {
				example.setValue(newAttribute, ((Complex)result).doubleValue());
			} else {
				example.setValue(newAttribute, newAttribute.getMapping().mapString(result.toString()));
			}
		}
		return newAttribute;
	}
	
	public JEP getParser() {
		return parser;
	}
	
	/** Parses all lines of the AttributeConstruction file and returns a list containing all newly generated
	 * attributes. */
	public static List<Attribute> generateAll(LoggingHandler logging, ExampleSet exampleSet, InputStream in) throws IOException, GenerationException {
		LinkedList<Attribute> generatedAttributes = new LinkedList<Attribute>();
		Document document = null;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		} catch (SAXException e1) {
			throw new IOException(e1.getMessage());
		} catch (ParserConfigurationException e1) {
			throw new IOException(e1.getMessage());
		}

		Element constructionsElement = document.getDocumentElement();
		if (!constructionsElement.getTagName().equals("constructions")) {
			throw new IOException("Outer tag of attribute constructions file must be <constructions>");
		}

		NodeList constructions = constructionsElement.getChildNodes();
		for (int i = 0; i < constructions.getLength(); i++) {
			Node node = constructions.item(i);
			if (node instanceof Element) {
				Element constructionTag = (Element)node;
				String tagName = constructionTag.getTagName();
				if (!tagName.equals("attribute"))
					throw new IOException("Only <attribute> tags are allowed for attribute description files, but found " + tagName);
				String attributeName = constructionTag.getAttribute("name");
				String attributeConstruction = constructionTag.getAttribute("construction");
				
				ExpressionParser parser = new ExpressionParser(true);
				if (attributeName == null) {
					throw new IOException("<attribute> tag needs 'name' attribute.");
				}
				if (attributeConstruction == null) {
					throw new IOException("<attribute> tag needs 'construction' attribute.");
				}
				if (attributeConstruction.equals(attributeName)) {
					Attribute presentAttribute = exampleSet.getAttributes().get(attributeName);
					if (presentAttribute != null) {
						generatedAttributes.add(presentAttribute);
						continue;
					} else {
						throw new GenerationException("No such attribute: " + attributeName);
					}
				} else {
					generatedAttributes.add(parser.addAttribute(exampleSet, attributeName, attributeConstruction));
				}
			}
		}
		return generatedAttributes;
	}
}
