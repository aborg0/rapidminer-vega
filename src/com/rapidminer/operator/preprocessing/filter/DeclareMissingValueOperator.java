/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
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
package com.rapidminer.operator.preprocessing.filter;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.function.ExpressionParser;
import com.rapidminer.tools.math.function.UnknownValue;

/**
 * Allows the declaration of a missing value (nominal or numeric) on a selected subset. The given value 
 * will be converted to Double.NaN, so subsequent operators will treat is as a missing value.
 * 
 * @author Marco Boeck
 */
public class DeclareMissingValueOperator extends AbstractExampleSetProcessing {
	
	/** parameter to set the missing value for numeric type*/
	public static final String PARAMETER_MISSING_VALUE_NUMERIC = "numeric_value";
	
	/** parameter to set the missing value for nominal type*/
	public static final String PARAMETER_MISSING_VALUE_NOMINAL = "nominal_value";
	
	/** parameter to set the epxression */
	public static final String PARAMETER_MISSING_VALUE_EXPRESSION = "expression_value";
	
	/** parameter to set the missing value type (numeric or nominal) */
	public static final String PARAMETER_MODE = "mode";
	
	/** Subset Selector for parameter use */
	private AttributeSubsetSelector subsetSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());
	
	/** constant for PARAMETER_VALUE_TYPE */
	private static final String NUMERIC = "numeric";
	
	/** constant for PARAMETER_VALUE_TYPE */
	private static final String NOMINAL = "nominal";
	
	/** constant for PARAMETER_VALUE_TYPE */
	private static final String EXPRESSION = "expression";
	
	/** value types to choose from in {@link #PARAMETER_MODE}*/
	private static final String[] VALUE_TYPES = new String[]{NUMERIC, NOMINAL, EXPRESSION};
	
	/** the ExpressionParser instance */
	private static ExpressionParser expParser;
	
	
	public DeclareMissingValueOperator(OperatorDescription description) {
		super(description);
		expParser = new ExpressionParser(true);
		expParser.getParser().setAllowUndeclared(true);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		if (isParameterSet(PARAMETER_MISSING_VALUE_NOMINAL) || isParameterSet(PARAMETER_MISSING_VALUE_NUMERIC)) {
			ExampleSetMetaData subset = subsetSelector.getMetaDataSubset(metaData, false);
			if (subset != null) {
				MDInteger missingValueNumber;
				boolean parameterAttributeTypeExistsInSubset = false;
				String mode = getParameterAsString(PARAMETER_MODE);
				for (AttributeMetaData amd : subset.getAllAttributes()) {
					AttributeMetaData originalAMD = metaData.getAttributeByName(amd.getName());
					missingValueNumber = originalAMD.getNumberOfMissingValues();
					missingValueNumber.increaseByUnknownAmount();
					
					if (mode.equals(NUMERIC)) {
						switch(amd.getValueType()) {
						case Ontology.NUMERICAL:
						case Ontology.INTEGER:
						case Ontology.REAL:
							parameterAttributeTypeExistsInSubset = true;
							break;
						default:
							continue;
						}
					} else if (mode.equals(NOMINAL)) {
						switch(amd.getValueType()) {
						case Ontology.NOMINAL:
						case Ontology.STRING:
						case Ontology.BINOMINAL:
						case Ontology.POLYNOMINAL:
						case Ontology.FILE_PATH:
						case Ontology.DATE_TIME:
							parameterAttributeTypeExistsInSubset = true;
							break;
						default:
							continue;
						}
					} else if (mode.equals(EXPRESSION)) {
						// expression can be on all types so always true
						parameterAttributeTypeExistsInSubset = true;
					}
				}
				if (!parameterAttributeTypeExistsInSubset) {
					if (subset.getAllAttributes().size() <= 0) {
						getInputPort().addError(new SimpleMetaDataError(Severity.ERROR, getInputPort(), "attribute_selection_empty"));
					} else {
						if (mode.equals(NUMERIC)) {
							getInputPort().addError(new SimpleMetaDataError(Severity.ERROR, getInputPort(), "exampleset.must_contain_numerical_attribute"));
						}
						if (mode.equals(NOMINAL)) {
							getInputPort().addError(new SimpleMetaDataError(Severity.ERROR, getInputPort(), "exampleset.must_contain_nominal_attribute"));
						}
					}
				}
			}
		}
		
		return metaData;		
	}
	
	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		ExampleSet subset = subsetSelector.getSubset(exampleSet, false);
		Attributes attributes = subset.getAttributes();
		String mode = getParameterAsString(PARAMETER_MODE);
		
		// handle EXPRESSION mode
		if (mode.equals(EXPRESSION)) 
		{
			// parse expression
			expParser.getParser().parseExpression(getParameterAsString(PARAMETER_MISSING_VALUE_EXPRESSION));
			// error after parsing?
			if (expParser.getParser().hasError()) {
		        throw new OperatorException(expParser.getParser().getErrorInfo());
			}
			
			SymbolTable symbolTable = expParser.getParser().getSymbolTable();
			Map<String, Attribute> name2attributes = new HashMap<String, Attribute>();
	        for (Object variableObj : symbolTable.values()) {
	            Variable variable = (Variable) variableObj;// symbolTable.getVar(variableName.toString());
	            if (!variable.isConstant()) {
	                Attribute attribute = exampleSet.getAttributes().get(variable.getName());
	                if (attribute == null) {
	                    throw new OperatorException("No such attribute: '" + variable.getName() + "'");
	                } else {
	                    name2attributes.put(variable.getName(), attribute);
	                    // retrieve test example with real values (needed to
	                    // compliance checking!)
	                    if (exampleSet.size() > 0) {
	                        Example example = exampleSet.iterator().next();
	                        if (attribute.isNominal()) {
	                            if (Double.isNaN(example.getValue(attribute))) {
	                            	expParser.getParser().addVariable(attribute.getName(), UnknownValue.UNKNOWN_NOMINAL); // ExpressionParserConstants.MISSING_VALUE);
	                            } else {
	                            	expParser.getParser().addVariable(attribute.getName(), example.getValueAsString(attribute));
	                            }
	                        } else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
	                            Calendar cal = Calendar.getInstance();
	                            cal.setTime(new Date((long) example.getValue(attribute)));
	                            expParser.getParser().addVariable(attribute.getName(), cal);
	                        } else {
	                        	expParser.getParser().addVariable(attribute.getName(), example.getValue(attribute));
	                        }
	                    } else {
	                        // nothing will be done later: no compliance to data
	                        // must be met
	                        if (attribute.isNominal()) {
	                        	expParser.getParser().addVariable(attribute.getName(), UnknownValue.UNKNOWN_NOMINAL);
	                        } else {
	                        	expParser.getParser().addVariable(attribute.getName(), Double.NaN);
	                        }
	                    }
	                }
	            }
	        }
	        
	        for (Example example : subset) {
				// handle expression mode
				if (mode.equals(EXPRESSION)) {
					for (Map.Entry<String, Attribute> entry : name2attributes.entrySet()) {
						String variableName = entry.getKey();
						Attribute attribute = entry.getValue();
						double value = example.getValue(attribute);
						if (attribute.isNominal()) {
							if (Double.isNaN(value)) {
								expParser.getParser().setVarValue(variableName, UnknownValue.UNKNOWN_NOMINAL);
							} else {
								expParser.getParser().setVarValue(variableName, example.getValueAsString(attribute));
							}
						} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
							if (Double.isNaN(value)) {
								expParser.getParser().setVarValue(variableName, UnknownValue.UNKNOWN_DATE);
							} else {
								Calendar cal = Calendar.getInstance();
								cal.setTime(new Date((long) value));
								expParser.getParser().setVarValue(variableName, cal);
							}
						} else {
							expParser.getParser().setVarValue(variableName, value);
						}
					}
					
					for (Attribute attribute : attributes) {
						
						Object result = expParser.getParser().getValueAsObject();
						if (!(result instanceof Boolean)) {
							//throw new OperatorException("expression does not evaluate to boolean!");
						} else {
							Boolean resultBoolean = (Boolean)result;
							// change to missing on true evaluation
							if (resultBoolean) {
								example.setValue(attribute, Double.NaN);
							}
						}
					}
				}
	        }
		}
		
		// handle NUMERIC and NOMINAL modes
		for (Example example : subset) {
			for (Attribute attribute : attributes) {
				if (mode.equals(NUMERIC)) {
					if (example.getValue(attribute) == getParameterAsDouble(PARAMETER_MISSING_VALUE_NUMERIC)) {
						example.setValue(attribute, Double.NaN);
					}
				} else if (mode.equals(NOMINAL)) {
					if (example.getNominalValue(attribute).equals(getParameterAsString(PARAMETER_MISSING_VALUE_NOMINAL))) {
						example.setValue(attribute, Double.NaN);
					}
				}
			}
		}
		
		return exampleSet;
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameters = super.getParameterTypes();
		
		parameters.addAll(subsetSelector.getParameterTypes());
		
		ParameterType type = new ParameterTypeCategory(PARAMETER_MODE, "Select the value type of the missing value", VALUE_TYPES, 0);
		type.setExpert(false);
		parameters.add(type);
		
		type = new ParameterTypeDouble(PARAMETER_MISSING_VALUE_NUMERIC, "This parameter defines the missing numerical value", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MODE, VALUE_TYPES, true, 0));
		type.setExpert(false);
		parameters.add(type);
		
		type = new ParameterTypeString(PARAMETER_MISSING_VALUE_NOMINAL, "This parameter defines the missing nominal value", true, false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MODE, VALUE_TYPES, true, 1));
		type.setExpert(false);
		parameters.add(type);
		
		type = new ParameterTypeExpression(PARAMETER_MISSING_VALUE_EXPRESSION, "This parameter defines the expression which if true equals the missing value", getInputPort(), true, false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_MODE, VALUE_TYPES, true, 2));
		type.setExpert(false);
		parameters.add(type);
		
		return parameters;
	}
	
	@Override
	public boolean writesIntoExistingData() {
		return true;
	}
	
	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), DeclareMissingValueOperator.class, null);
	}
}
