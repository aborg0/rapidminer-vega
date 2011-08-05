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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.ViewAttribute;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Pair;

/**
 * The model class for the {@link NominalToNumericModel} operator. Can either
 * transform nominals to numeric by simply replacing the nominal values by the
 * respective integer mapping, or by using effect coding or dummy coding.
 * 
 * @author Marius Helf
 */
class NominalToNumericModel extends PreprocessingModel {
		private static final long serialVersionUID = -4203775081616082145L;
		private int codingType;
		
		/**
		 * maps a target attribute to the value for which it becomes one (for dummy coding)
		 */
		private Map<String,Double> attributeTo1ValueMap = null;
		
		/**
		 * maps a target attribute to the value for which it becomes -1 or 1 respectively (for effect coding).
		 * The first value of the pair is the value for 1, the second for -1. 
		 */
		private Map<String,Pair<Double,Double>> attributeToValuesMap = null;
		
		
		/**
		 * maps source attributes to their comparison group.
		 */
		private Map<String,Double> sourceAttributeToComparisonGroupMap = null;
		
		
		/**
		 * Relevant only when using dummy coding or effect coding.
		 * 
		 * If true, the naming scheme for target attributes is "sourceAttribute_value",
		 * if false, "sourceAttribute = value"
		 */
		private boolean useUnderscoreInName = false;
		
		/**
		 * Constructs a new model. Use this ctor to create a model for value encoding.
		 * @param exampleSet
		 * @param codingType the coding type. Should be NominalToNumeric.INTEGERS when called manually. 
		 */
		protected NominalToNumericModel(ExampleSet exampleSet, int codingType) {
			super(exampleSet);
			this.codingType = codingType;
		}

		/**
		 * Constructs a new model. Use this ctor to create a model for dummy encoding or effect encoding.
		 * @param exampleSet
		 * @param codingType the coding type. Should be NominalToNumeric.EFFECT_CODING or DUMMY_CODING. 
		 * @param useUnderscoreInName @see NominalToNumericModel#useUnderscoreInName
		 * @param sourceAttributeToComparisonGroupMap @see NominalToNumericModel#sourceAttributeToComparisonGroupMap  @see NominalToNumeric#getSourceAttributeToComparisonGroupMap
		 * @param attributeTo1ValueMap @see NominalToNumericModel#attributeTo1ValueMap. Must be non-null for dummy coding, should be null for effect coding. @see NominalToNumeric#getAttributeTo1ValueMap
		 * @param attributeToValuesMap @see NominalToNumericModel#attributeToValuesMap. Must be non-null for effect coding, should be null for dummy coding. @see NominalToNumeric#getAttributeToValuesMap
		 */
		protected NominalToNumericModel(ExampleSet exampleSet, int codingType, boolean useUnderscoreInName, Map<String,Double> sourceAttributeToComparisonGroupMap, Map<String,Double> attributeTo1ValueMap, Map<String, Pair<Double,Double>> attributeToValuesMap) {
			this(exampleSet, codingType);
			this.useUnderscoreInName = useUnderscoreInName;
			this.sourceAttributeToComparisonGroupMap = sourceAttributeToComparisonGroupMap;
			this.attributeTo1ValueMap = attributeTo1ValueMap;
			this.attributeToValuesMap = attributeToValuesMap; 			
		}

		@Override
		public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {
			switch(codingType) {
			case NominalToNumeric.INTEGERS_CODING:
				return applyOnDataIntegers(exampleSet);
			case NominalToNumeric.DUMMY_CODING:
				return applyOnDataDummyCoding(exampleSet, false);
			case NominalToNumeric.EFFECT_CODING:
				return applyOnDataDummyCoding(exampleSet, true);
			default:
				assert(false);	// codingType must be one of the above
				return null;					
			}
		}
		
		/**
		 * Returns a list containing the names of those attributes which will
		 * represent the coding of the given source attribute.
		 */
		private List<String> getTargetAttributesFromSourceAttribute(Attribute sourceAttribute) {
			List<String> targetNames = new LinkedList<String>();
			double comparisonGroup = sourceAttributeToComparisonGroupMap.get( sourceAttribute.getName() );
			for ( int currentValue = 0; currentValue < sourceAttribute.getMapping().size(); ++currentValue ) {
				if ( currentValue != comparisonGroup ) {
					targetNames.add( NominalToNumeric.getTargetAttributeName(sourceAttribute.getName(), sourceAttribute.getMapping().mapIndex(currentValue), useUnderscoreInName) );
				}
			}
			return targetNames;
		}

		/**
		 * Creates a dummy coding or effect coding from the given example set.
		 * @param effectCoding If true, the function does effect coding. If false, dummy coding. 
		 */
		private ExampleSet applyOnDataDummyCoding(ExampleSet exampleSet, boolean effectCoding ) {
			// selecting transformation attributes and creating new numeric attributes
			LinkedList<Attribute> nominalAttributes = new LinkedList<Attribute>();
			LinkedList<Attribute> transformedAttributes = new LinkedList<Attribute>();
			for (Attribute attribute : exampleSet.getAttributes()) {
				if (!attribute.isNumerical()) {
					nominalAttributes.add(attribute);

					List<String> targetNames = getTargetAttributesFromSourceAttribute(attribute);
					for ( String targetName : targetNames ) {
						transformedAttributes.add(AttributeFactory.createAttribute(targetName, Ontology.INTEGER));
					}

				}
			}

			// ensuring capacity in ExampleTable
			exampleSet.getExampleTable().addAttributes(transformedAttributes);
			for ( Attribute attribute : transformedAttributes ) {
				exampleSet.getAttributes().addRegular(attribute);
			}
			
			// copying values
			for (Example example: exampleSet) {	
				for ( Attribute nominalAttribute : nominalAttributes ) {
					double sourceValue = example.getValue(nominalAttribute);
					for ( String targetName : getTargetAttributesFromSourceAttribute(nominalAttribute) ) {
						Attribute targetAttribute = exampleSet.getAttributes().get(targetName);
						example.setValue(targetAttribute, getValue(targetAttribute, sourceValue));
					}
				}
			}
			
			// remove nominal attributes
			for ( Attribute nominalAttribute : nominalAttributes ) {
				exampleSet.getAttributes().remove(nominalAttribute);
			}
			return exampleSet;
		}

		/**
		 * Transforms the numerical attributes to integer values (corresponding to the internal mapping).
		 */
		private ExampleSet applyOnDataIntegers(ExampleSet exampleSet) {
			// selecting transformation attributes and creating new numeric attributes
			LinkedList<Attribute> nominalAttributes = new LinkedList<Attribute>();
			LinkedList<Attribute> transformedAttributes = new LinkedList<Attribute>();
			for (Attribute attribute : exampleSet.getAttributes()) {
				if (!attribute.isNumerical()) {
					nominalAttributes.add(attribute);
					// creating new attributes for nominal attributes
					transformedAttributes.add(AttributeFactory.createAttribute(attribute.getName(), Ontology.NUMERICAL));
				}
			}

			// ensuring capacity in ExampleTable
			exampleSet.getExampleTable().addAttributes(transformedAttributes);

			// copying values
			for (Example example: exampleSet) {
				Iterator<Attribute> target = transformedAttributes.iterator();
				for (Attribute attribute: nominalAttributes) {
					example.setValue(target.next(), example.getValue(attribute));
				}
			}

			// removing nominal attributes from example Set
			Attributes attributes = exampleSet.getAttributes();
			for(Attribute attribute: exampleSet.getAttributes()) {
				if (!attribute.isNumerical())
					attributes.replace(attribute, transformedAttributes.poll());
			}
			return exampleSet;
		}

		@Override
		public Attributes getTargetAttributes(ExampleSet parentSet) {
			SimpleAttributes attributes = new SimpleAttributes();
			// add special attributes to new attributes
			Iterator<AttributeRole> specialRoles = parentSet.getAttributes().specialAttributes();
			while (specialRoles.hasNext()) {
				attributes.add(specialRoles.next());
			}
			
			// add regular attributes
			for (Attribute attribute : parentSet.getAttributes()) {
				if (!attribute.isNumerical()) {
					if ( codingType == NominalToNumeric.EFFECT_CODING || codingType == NominalToNumeric.DUMMY_CODING ) {
						double comparisonGroup = sourceAttributeToComparisonGroupMap.get(attribute.getName());
						for ( int currentValue = 0; currentValue < attribute.getMapping().size(); ++currentValue ) {
							if ( currentValue != comparisonGroup ) {
								ViewAttribute viewAttribute = new ViewAttribute(
										this, 
										attribute, 
										NominalToNumeric.getTargetAttributeName(attribute.getName(), attribute.getMapping().mapIndex(currentValue), useUnderscoreInName), 
										Ontology.INTEGER, 
										null);
								attributes.addRegular(viewAttribute);
							}
						}
					} else if ( codingType == NominalToNumeric.INTEGERS_CODING ) {
						attributes.addRegular(new ViewAttribute(this, attribute, attribute.getName(), Ontology.INTEGER, null));
					} else {
						assert(false); // unsupported coding
					}
				} else {
					attributes.addRegular(attribute);
				}
			}
			return attributes;
		}

		@Override
		public double getValue(Attribute targetAttribute, double value) {
			if ( codingType == NominalToNumeric.DUMMY_CODING ) {
				String targetName = targetAttribute.getName();
				if ( attributeTo1ValueMap.get(targetName) == value ) {
					return 1;
				} else {
					return 0;
				}
			} else if ( codingType == NominalToNumeric.EFFECT_CODING ) {
				String targetName = targetAttribute.getName();
				Pair<Double,Double> storedValue = attributeToValuesMap.get(targetName);
				if ( storedValue.getFirst() == value ) {
					return 1;
				} else if ( storedValue.getSecond() == value ) {
					return -1;
				} else {
					return 0;
				}
			} else if ( codingType == NominalToNumeric.INTEGERS_CODING ) {
				return value;
			} else {
				assert(false); // unsupported coding
				return Double.NaN;
			}						
		}

		@Override
		public String getName() {
			return "Nominal2Numerical Model";
		}


	}