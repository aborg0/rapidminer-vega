package com.rapidminer.operator.preprocessing.filter;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;

/**
 * <p>Allows the declaration of a missing value (nominal or numeric) on a selected subset. The given value 
 * will be converted to Double.NaN, so subsequent operators will treat is as a missing value.
 * 
 * @author Marco Boeck
 */
public class DeclareMissingValueOperator extends AbstractExampleSetProcessing {
	
	/** parameter to set the missing value for numeric type*/
	public static final String PARAMETER_MISSING_VALUE_NUMERIC = "numeric_value";
	
	/** parameter to set the missing value for nominal type*/
	public static final String PARAMETER_MISSING_VALUE_NOMINAL = "nominal_value";
	
	/** parameter to set the missing value type (numeric or nominal) */
	public static final String PARAMETER_MODE = "mode";
	
	/** Subset Selector for parameter use */
	private AttributeSubsetSelector subsetSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());
	
	/** constant for PARAMETER_VALUE_TYPE */
	private static final String NUMERIC = "numeric";
	
	/** constant for PARAMETER_VALUE_TYPE */
	private static final String NOMINAL = "nominal";
	
	/** value types to choose from in {@link #PARAMETER_MODE}*/
	private static final String[] VALUE_TYPES = new String[]{NUMERIC, NOMINAL};
	
	
	public DeclareMissingValueOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		if (isParameterSet(PARAMETER_MISSING_VALUE_NOMINAL) || isParameterSet(PARAMETER_MISSING_VALUE_NUMERIC)) {
			ExampleSetMetaData subset = subsetSelector.getMetaDataSubset(metaData, false);
			if (subset != null) {
				MDInteger missingValueNumber = new MDInteger();
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
					}
					if (mode.equals(NOMINAL)) {
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
		for (Example example : subset) {
			for (Attribute attribute : attributes) {
				if (mode.equals(NUMERIC)) {
					if (example.getValue(attribute) == getParameterAsDouble(PARAMETER_MISSING_VALUE_NUMERIC)) {
						example.setValue(attribute, Double.NaN);
					}
				}
				if (mode.equals(NOMINAL)) {
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
		
		return parameters;
	}

}
