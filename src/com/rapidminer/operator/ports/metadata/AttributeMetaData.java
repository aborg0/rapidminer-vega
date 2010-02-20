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
package com.rapidminer.operator.ports.metadata;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.container.Range;

/** Meta data about an attribute
 * 
 * @author Simon Fischer
 *
 */
public class AttributeMetaData implements Serializable {

	private static final long serialVersionUID = 1L;

	private ExampleSetMetaData owner = null;
	
	private String name;

	private int type = Ontology.ATTRIBUTE_VALUE;
	private String role = null;
	private MDInteger numberOfMissingValues = new MDInteger(0);

	// it has to be ensured that the appropriate value set type is constructed anyway 
	private SetRelation valueSetRelation = SetRelation.UNKNOWN;
	private Range valueRange = new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	private Set<String> valueSet = new TreeSet<String>();
	private String mode;

	private MDReal mean = new MDReal();

	public AttributeMetaData(String name, int type) {
		this(name, type, null);
	}

	public AttributeMetaData(AttributeRole role, ExampleSet exampleSet) {
		this(role.getAttribute().getName(), role.getAttribute().getValueType(), role.getSpecialName());
		Attribute att = role.getAttribute();
		if (att.isNominal()) {
			valueSet.clear();
			valueSet.addAll(att.getMapping().getValues());
			valueSetRelation = SetRelation.EQUAL;
		}		
		if (exampleSet != null) {
			numberOfMissingValues = new MDInteger((int)exampleSet.getStatistics(att, Statistics.UNKNOWN));
			if (att.isNumerical() || Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), Ontology.DATE_TIME)) {
				valueSetRelation = SetRelation.EQUAL;
				valueRange = new Range(exampleSet.getStatistics(att, Statistics.MINIMUM), exampleSet.getStatistics(att, Statistics.MAXIMUM));				
				setMean(new MDReal(exampleSet.getStatistics(att, Statistics.AVERAGE)));
			}
			if (att.isNominal()) {
				int modeIndex = (int)exampleSet.getStatistics(att, Statistics.MODE);
				if (modeIndex >= 0) {
					setMode(att.getMapping().mapIndex(modeIndex));
				}
			}
		} else {
			numberOfMissingValues = new MDInteger();
			if (att.isNumerical()) {
				setMean(new MDReal());
			}
			if (att.isNominal()) {
				setMode(null);
			}
		}
	}

	public AttributeMetaData(String name, int type, String role) {
		this.name = name;
		this.type = type;
		this.role = role;
	}

	public AttributeMetaData(String name, String role, int nominalType, String...values) {
		this(name, role, values);
		this.type = nominalType;
	}

	public AttributeMetaData(String name, String role, String...values) {
		this.name = name;
		this.type = Ontology.NOMINAL;
		this.role = role;
		this.valueSetRelation = SetRelation.EQUAL;
		for (String string: values)
			valueSet.add(string);
	}

	public AttributeMetaData(String name, String role, Range range) {	
		this.name = name;
		this.role = role;
		this.type = Ontology.REAL;
		this.valueRange = range;
		this.valueSetRelation = SetRelation.EQUAL;
	}

	public AttributeMetaData(String name, String role, int type, Range range) {
		this(name, role, range);
		this.type = type;
	}

	private AttributeMetaData(AttributeMetaData attributeMetaData) {
		this.name = attributeMetaData.name;
		this.role = attributeMetaData.role;
		this.type= attributeMetaData.type;
		this.numberOfMissingValues = attributeMetaData.numberOfMissingValues;
		this.mean = attributeMetaData.mean;
		this.mode = attributeMetaData.mode;
		this.valueSetRelation = attributeMetaData.getValueSetRelation();
		this.valueRange = new Range(attributeMetaData.getValueRange());
		this.valueSet = new TreeSet<String>();
		valueSet.addAll(attributeMetaData.getValueSet());
	}

	public AttributeMetaData(Attribute attribute) {
		this.name = attribute.getName();
		this.type = attribute.getValueType();
	}

	public String getRole() {
		return role;
	}

	public String getName() {		
		return name;
	}

	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		// informing ExampleSetMEtaData if one registered
		if (owner != null)
			owner.attributeRenamed(this, oldName);
	}

	public String getTypeName() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(type);
	}

	public int getValueType() {
		return type;
	}

	/** 
	 * If you change the type, keep in mind to set the value sets and their relation
	 */
	public void setType(int type) {
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NUMERICAL)) {
			valueSet.clear();
		} else {
			setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
		}
		this.type = type;
	}

	@Override
	public String toString() {
		return getDescription();
	}

	public String getDescription() {
		StringBuffer buf = new StringBuffer();		
		if (role != null && !role.equals(Attributes.ATTRIBUTE_NAME)) {
			buf.append("<em>");
			buf.append(role);
			buf.append("</em>: ");
		}
		buf.append(getName());
		buf.append(" (");
		buf.append(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(getValueType()));
		if (valueSetRelation != SetRelation.UNKNOWN) {
			if (isNominal()) {
				buf.append(" in " + valueSetRelation + " {");
				boolean first = true;
				String mode = getMode();
				int index = 0;
				for (String value : valueSet) {
					index++;
					if (first) {
						first = false;
					} else {
						buf.append(", ");
					}

					if (index >= 10) {
						buf.append("...");
						break;
					}

					boolean isMode = value.equals(mode);					
					if (isMode) {
						buf.append("<span style=\"text-decoration:underline\">");
					}
					buf.append(value);
					if (isMode) {
						buf.append("</span>");
					}
				}
				buf.append("}");
			}
			if (isNumerical()) {
				buf.append(" in " + valueSetRelation + " [");
				if (getValueRange() != null) {
					buf.append(Tools.formatNumber(getValueRange().getLower(), 3));
					buf.append("...");
					buf.append(Tools.formatNumber(getValueRange().getUpper(), 3));
					buf.append("]");
				}
				if (getMean().isKnown()) {
					buf.append("; mean ");
					buf.append(getMean().toString());
				}
			}
			if (valueRange != null &&
				Ontology.ATTRIBUTE_VALUE_TYPE.isA(getValueType(), Ontology.DATE_TIME) &&
				!Double.isInfinite(getValueRange().getLower()) &&
				!Double.isInfinite(getValueRange().getUpper())) {
				buf.append(" in " + valueSetRelation + " [");
				switch (getValueType()) {
				case Ontology.DATE:
					buf.append(Tools.formatDate(new Date((long) getValueRange().getLower())));
					buf.append("...");
					buf.append(Tools.formatDate(new Date((long) getValueRange().getUpper())));
					buf.append("]");
					break;
				case Ontology.TIME:
					buf.append(Tools.formatTime(new Date((long) getValueRange().getLower())));
					buf.append("...");
					buf.append(Tools.formatTime(new Date((long) getValueRange().getUpper())));
					buf.append("]");
					break;
				case Ontology.DATE_TIME:
					buf.append(Tools.formatDateTime(new Date((long) getValueRange().getLower())));
					buf.append("...");
					buf.append(Tools.formatDateTime(new Date((long) getValueRange().getUpper())));
					buf.append("]");
					break;
				}
			}
		} else {
			if (isNominal())
				buf.append(", values unkown");
			else
				buf.append(", range unknown");
		}
		switch (containsMissingValues()) {
		case NO: 
			buf.append("; no missing values"); 
			break;
		case YES:
			buf.append("; ");
			buf.append(numberOfMissingValues.toString());
			buf.append(" missing values");
			break;
		case UNKNOWN:
			buf.append("; may contain missing values");			
			break;
		}		
		buf.append(")");
		return buf.toString();
	}

	public String getDescriptionAsTableRow() {
		StringBuilder b = new StringBuilder();
		b.append("<tr><td>");
		String role2 = getRole();
		if (role2 == null) {
			role2 = "-";
		}
		b.append(role2).append("</td><td>");
		b.append(getName()).append("</td><td>");
		b.append(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(getValueType())).append("</td><td>");

		if (valueSetRelation != SetRelation.UNKNOWN) {
			if (isNominal()) {
				b.append(valueSetRelation + " {");
				boolean first = true;
				String mode = getMode();
				int index = 0;
				for (String value : valueSet) {
					index++;
					if (first) {
						first = false;
					} else {
						b.append(", ");
					}

					if (index >= 10) {
						b.append("...");
						break;
					}

					boolean isMode = value.equals(mode);					
					if (isMode) {
						b.append("<span style=\"text-decoration:underline\">");
					}
					b.append(value);
					if (isMode) {
						b.append("</span>");
					}
				}
				b.append("}");
			}
			if (isNumerical()) {
				b.append(valueSetRelation + " [");
				if (getValueRange() != null) {
					b.append(Tools.formatNumber(getValueRange().getLower(), 3));
					b.append("...");
					b.append(Tools.formatNumber(getValueRange().getUpper(), 3));
					b.append("]");
				}
				if (getMean().isKnown()) {
					b.append("; mean ");
					b.append(getMean().toString());
				}
			}
			if (valueRange != null &&
				Ontology.ATTRIBUTE_VALUE_TYPE.isA(getValueType(), Ontology.DATE_TIME) &&
				!Double.isInfinite(getValueRange().getLower()) &&
				!Double.isInfinite(getValueRange().getUpper())) {
				b.append(valueSetRelation + " [");
				switch (getValueType()) {
				case Ontology.DATE:
					b.append(Tools.formatDate(new Date((long) getValueRange().getLower())));
					b.append("...");
					b.append(Tools.formatDate(new Date((long) getValueRange().getUpper())));
					b.append("]");
					break;
				case Ontology.TIME:
					b.append(Tools.formatTime(new Date((long) getValueRange().getLower())));
					b.append("...");
					b.append(Tools.formatTime(new Date((long) getValueRange().getUpper())));
					b.append("]");
					break;
				case Ontology.DATE_TIME:
					b.append(Tools.formatDateTime(new Date((long) getValueRange().getLower())));
					b.append("...");
					b.append(Tools.formatDateTime(new Date((long) getValueRange().getUpper())));
					b.append("]");
					break;
				}
			}
		} else {
			if (isNominal())
				b.append("values unkown");
			else
				b.append("range unknown");
		}
		b.append("</td><td>");
		
		switch (containsMissingValues()) {
		case NO: 
			b.append("no missing values"); 
			break;
		case YES:
			b.append(numberOfMissingValues.toString());
			b.append(" missing values");
			break;
		case UNKNOWN:
			b.append("may contain missing values");			
			break;
		}		

		b.append("</td></tr>");
		return b.toString();
	}

	@Override
	public AttributeMetaData clone() {
		return new AttributeMetaData(this);
	}

	public boolean isNominal() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NOMINAL);
	}

	public boolean isBinominal() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.BINOMINAL);
	}

	public boolean isPolynominal() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.POLYNOMINAL);
	}

	public boolean isNumerical() {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NUMERICAL);
	}
	
	public MetaDataInfo containsMissingValues() {
		return numberOfMissingValues.isAtLeast(1);
	}

	public void setNumberOfMissingValues(MDInteger numberOfMissingValues) {
		this.numberOfMissingValues = numberOfMissingValues;
	}

	public MDInteger getNumberOfMissingValues() {
		return this.numberOfMissingValues;
	}

	public SetRelation getValueSetRelation() {
		return valueSetRelation;
	}

	public Set<String> getValueSet() {
		return valueSet;
	}

	public void setValueSet(Set<String> valueSet, SetRelation relation) {
		this.valueSetRelation = relation;
		this.valueSet = valueSet;
	}

	public Range getValueRange() {
		return valueRange;
	}

	public void setValueRange(Range range, SetRelation relation) {
		this.valueSetRelation = relation;
		this.valueRange = range;
	}

	public AttributeMetaData copy() {
		return new AttributeMetaData(this);
	}

	/**
	 * Sets the role of this attribute. The name is equivalent with the names from Attributes.
	 * To reset use null as parameter.
	 */
	public void setRole(String role) {
		this.role = role;	
	}

	public void setRegular() {
		this.role = null;
	}

	public boolean isSpecial() {
		return (role != null);
	}

	/**
	 * This method returns a AttributeMetaData object for the prediction attribute
	 * created on applying a model on an exampleset with the given label.
	 */
	public static AttributeMetaData createPredictionMetaData(AttributeMetaData labelMetaData) {
		AttributeMetaData result = labelMetaData.clone();
		result.setName("prediction(" + result.getName() + ")");
		result.setRole(Attributes.PREDICTION_NAME);
		return result;
	}

	/**
	 * This method creates the attribute meta data for the confidence attributes in the given 
	 * exampleSetMetaData. If the values are not known precisely the attributeSet relation of the
	 * exampleSetMetaData object is set appropriate.
	 * @return
	 */
	public static ExampleSetMetaData createConfidenceAttributeMetaData(ExampleSetMetaData exampleSetMD) {
		if (exampleSetMD.hasSpecial(Attributes.LABEL_NAME) == MetaDataInfo.YES) {
			AttributeMetaData labelMetaData = exampleSetMD.getLabelMetaData();
			if (labelMetaData.isNominal()) {
				for (String value: labelMetaData.getValueSet()) {
					AttributeMetaData conf = new AttributeMetaData(Attributes.CONFIDENCE_NAME + "_" + value, Ontology.REAL, Attributes.CONFIDENCE_NAME);
					conf.setValueRange(new Range(0d, 1d), SetRelation.EQUAL);
					exampleSetMD.addAttribute(conf);
				}
				// setting attribute set relation according to value set relation
				exampleSetMD.mergeSetRelation(labelMetaData.getValueSetRelation());
				return exampleSetMD;
			}
		}
		return exampleSetMD;
	}

	public void setValueSetRelation(SetRelation valueSetRelation) {
		this.valueSetRelation = valueSetRelation;
	}

	public void setMean(MDReal mean) {
		this.mean = mean;
	}

	public MDReal getMean() {
		return mean;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getMode() {
		return mode;
	}

	/** Sets types and ranges to the superset of this and the argument. */
	public void merge(AttributeMetaData amd) {
		if (amd.isNominal() != this.isNominal()) {
			this.type = Ontology.ATTRIBUTE_VALUE;
		}
		if (isNominal()) {
			if ((amd.valueSet != null) && (this.valueSet != null)) {
				if (!amd.valueSet.equals(this.valueSet)) {
					this.valueSetRelation.merge(SetRelation.SUBSET);
				}
				this.valueSet.addAll(amd.valueSet);
			}
			this.valueSetRelation.merge(amd.valueSetRelation);
		}
		if (isNumerical()) {
			if ((valueRange != null) && (amd.valueRange != null)) {
				double min = Math.min(amd.valueRange.getLower(), this.valueRange.getLower());
				double max = Math.max(amd.valueRange.getUpper(), this.valueRange.getUpper());
				this.valueRange = new Range(min, max);
			}
			this.valueSetRelation.merge(amd.valueSetRelation);
		}
	}

	/** Returns either the value range or the value set, depending on the type of attribute. */
	public String getRangeString() {

		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(getValueType(), Ontology.DATE_TIME)) {
			if (!Double.isInfinite(getValueRange().getLower()) && !Double.isInfinite(getValueRange().getUpper())) {
				StringBuilder buf = new StringBuilder();
				buf.append(valueSetRelation.toString());
				if (valueSetRelation != SetRelation.UNKNOWN) {
					buf.append("[");
					switch (getValueType()) {
					case Ontology.DATE:
						buf.append(Tools.formatDate(new Date((long) getValueRange().getLower())));
						buf.append(" \u2013 ");
						buf.append(Tools.formatDate(new Date((long) getValueRange().getUpper())));				
						break;
					case Ontology.TIME:
						buf.append(Tools.formatTime(new Date((long) getValueRange().getLower())));
						buf.append(" \u2013 ");
						buf.append(Tools.formatTime(new Date((long) getValueRange().getUpper())));				
						break;
					case Ontology.DATE_TIME:
						buf.append(Tools.formatDateTime(new Date((long) getValueRange().getLower())));
						buf.append(" \u2013 ");
						buf.append(Tools.formatDateTime(new Date((long) getValueRange().getUpper())));
						break;
					}
					buf.append("]");
					return buf.toString();
				} else {
					return "Unknown date range";		
				}
			}
			return "Unbounded date range";
		} else 	if (!isNominal() && (valueRange != null)) {
			return valueSetRelation.toString() + (valueSetRelation != SetRelation.UNKNOWN ? valueRange.toString() : "");
		} else if (isNominal() && (valueSet != null)) {			
			return valueSetRelation.toString() + (valueSetRelation != SetRelation.UNKNOWN ? valueSet.toString() : "");
		} else {
			return "unknown";
		}
	}

	/** Throws away nominal values until the value set size is at most the value specified by property
	 *  {@link RapidMiner#PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES}. */
	public void shrinkValueSet() {
		int maxSize = getMaximumNumerOfNominalValues();
		shrinkValueSet(maxSize);
	}

	/** Returns the maximum number of values to be used for meta data generation as specified by  
	 *  {@link RapidMiner#PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES}. */
	public static int getMaximumNumerOfNominalValues() {
		int maxSize = 100;
		String maxSizeString = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES);
		if (maxSizeString != null) {
			maxSize = Integer.parseInt(maxSizeString);
			if (maxSize == 0) {
				maxSize = Integer.MAX_VALUE;
			}
		}
		return maxSize;
	}

	/** Throws away nominal values until the value set size is at most the given value.*/
	private void shrinkValueSet(int maxSize) {
		if (valueSet != null) {
			if (valueSet.size() > maxSize) {				
				Set<String> newSet = new TreeSet<String>();
				Iterator<String> i = valueSet.iterator();
				int count = 0;
				while (i.hasNext() && (count < maxSize)) {
					newSet.add(i.next());
					count++;
				}	
				this.valueSet = newSet;
				valueSetRelation = valueSetRelation.merge(SetRelation.SUPERSET);
				
				if (owner != null) {
					owner.setNominalDataWasShrinked(true);
				}
			}			
			
		}		
	}
	
	/**
	 * This method is only to be used by ExampleSetMetaData to register as owner of this attributeMetaData.
	 * Returnes is this object or a clone if this object already has an owner.
	 */
	/*pp*/ AttributeMetaData registerOwner(ExampleSetMetaData owner) {
		if (this.owner == null) {
			this.owner = owner;
			return this;
		} else {
			AttributeMetaData clone = this.clone();
			clone.owner = owner;
			return clone;
		}
	}
}
