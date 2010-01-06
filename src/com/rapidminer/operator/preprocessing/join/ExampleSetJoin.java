/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
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
package com.rapidminer.operator.preprocessing.join;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;


/**
 * <p>
 * Build the join of two example sets using the id attributes of the sets, i.e. both example sets must have an id attribute where the same id indicate the same examples. If examples are missing an
 * exception will be thrown. The result example set will consist of the same number of examples but the union set or the union list (depending on parameter setting double attributes will be removed or
 * renamed) of both feature sets. In case of removing double attribute the attribute values must be the same for the examples of both example set, otherwise an exception will be thrown.
 * </p>
 * <p>
 * Please note that this check for double attributes will only be applied for regular attributes. Special attributes of the second input example set which do not exist in the first example set will
 * simply be added. If they already exist they are simply skipped.
 * </p>
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ExampleSetJoin extends AbstractExampleSetJoin {

	public static final String PARAMETER_JOIN_TYPE = "join_type";

	public static final String[] JOIN_TYPES = { "inner" , "left" , "right" , "outer" };

	public static final int JOIN_TYPE_INNER = 0;

	public static final int JOIN_TYPE_LEFT = 1;

	public static final int JOIN_TYPE_RIGHT = 2;

	public static final int JOIN_TYPE_OUTER = 3;

	public ExampleSetJoin(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MemoryExampleTable joinData(ExampleSet leftExampleSet, ExampleSet rightExampleSet, List<AttributeSource> originalAttributeSources, List<Attribute> unionAttributeList) throws OperatorException {
		int joinType = getParameterAsInt(PARAMETER_JOIN_TYPE);
		leftExampleSet.remapIds();
		rightExampleSet.remapIds();
		Attribute leftIdAttribute = leftExampleSet.getAttributes().getId();
		Attribute rightIdAttribute = rightExampleSet.getAttributes().getId();

		MemoryExampleTable unionTable = new MemoryExampleTable(unionAttributeList);
		switch (joinType) {
		case JOIN_TYPE_INNER:
			for (Example leftExample : leftExampleSet) {
				double leftIdValue = leftExample.getValue(leftIdAttribute);
				int[] rightExampleIndices = null;
				if (leftIdAttribute.isNominal()) {
					rightExampleIndices = rightExampleSet.getExampleIndicesFromId(rightIdAttribute.getMapping().getIndex(leftIdAttribute.getMapping().mapIndex((int) leftIdValue)));
				} else {
					rightExampleIndices = rightExampleSet.getExampleIndicesFromId(leftIdValue);
				}
				if (rightExampleIndices != null) {
					for (int rightExampleIndex : rightExampleIndices) {
						Example rightExample = rightExampleSet.getExample(rightExampleIndex);
						double[] unionDataRow = new double[unionAttributeList.size()];
						int attributeIndex = 0;
						for (AttributeSource attributeSource : originalAttributeSources) {
							if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
								unionDataRow[attributeIndex] = leftExample.getValue(attributeSource.getAttribute());
							} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
								unionDataRow[attributeIndex] = rightExample.getValue(attributeSource.getAttribute());
							}
							attributeIndex++;
						}
						unionTable.addDataRow(new DoubleArrayDataRow(unionDataRow));
					}
				}
				checkForStop();
			}
			break;
		case JOIN_TYPE_LEFT:
			for (Example leftExample : leftExampleSet) {
				double leftIdValue = leftExample.getValue(leftIdAttribute);
				int[] rightExampleIndices = null;
				if (leftIdAttribute.isNominal()) {
					rightExampleIndices = rightExampleSet.getExampleIndicesFromId(rightIdAttribute.getMapping().getIndex(leftIdAttribute.getMapping().mapIndex((int) leftIdValue)));
				} else {
					rightExampleIndices = rightExampleSet.getExampleIndicesFromId(leftIdValue);
				}
				if (rightExampleIndices != null) {
					for (int rightExampleIndex : rightExampleIndices) {
						Example rightExample = rightExampleSet.getExample(rightExampleIndex);
						double[] unionDataRow = new double[unionAttributeList.size()];
						int attributeIndex = 0;
						for (AttributeSource attributeSource : originalAttributeSources) {
							if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
								unionDataRow[attributeIndex] = leftExample.getValue(attributeSource.getAttribute());
							} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
								unionDataRow[attributeIndex] = rightExample.getValue(attributeSource.getAttribute());
							}
							attributeIndex++;
						}
						unionTable.addDataRow(new DoubleArrayDataRow(unionDataRow));
					}
				} else {
					double[] unionDataRow = new double[unionAttributeList.size()];
					int attributeIndex = 0;
					for (AttributeSource attributeSource : originalAttributeSources) {
						if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
							unionDataRow[attributeIndex] = leftExample.getValue(attributeSource.getAttribute());
						} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
							unionDataRow[attributeIndex] = Double.NaN;
						}
						attributeIndex++;
					}
					unionTable.addDataRow(new DoubleArrayDataRow(unionDataRow));
				}
				checkForStop();
			}
			break;
		case JOIN_TYPE_RIGHT:
			for (Example rightExample : rightExampleSet) {
				double rightIdValue = rightExample.getValue(rightIdAttribute);
				int[] leftExampleIndices = null;
				if (rightIdAttribute.isNominal()) {
					leftExampleIndices = leftExampleSet.getExampleIndicesFromId(leftIdAttribute.getMapping().getIndex(rightIdAttribute.getMapping().mapIndex((int) rightIdValue)));
				} else {
					leftExampleIndices = leftExampleSet.getExampleIndicesFromId(rightIdValue);
				}
				if (leftExampleIndices != null) {
					for (int leftExampleIndex : leftExampleIndices) {
						Example leftExample = leftExampleSet.getExample(leftExampleIndex);
						double[] unionDataRow = new double[unionAttributeList.size()];
						int attributeIndex = 0;
						for (AttributeSource attributeSource : originalAttributeSources) {
							if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
								unionDataRow[attributeIndex] = leftExample.getValue(attributeSource.getAttribute());
							} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
								unionDataRow[attributeIndex] = rightExample.getValue(attributeSource.getAttribute());
							}
							attributeIndex++;
						}
						unionTable.addDataRow(new DoubleArrayDataRow(unionDataRow));
					}
				} else {
					double[] unionDataRow = new double[unionAttributeList.size()];
					int attributeIndex = 0;
					for (AttributeSource attributeSource : originalAttributeSources) {
						if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
							// since ID is always taken from left example set, ID value must be fetched
							// from right example set explicitly
							if (attributeSource.getAttribute() == leftIdAttribute) {
								unionDataRow[attributeIndex] = rightExample.getValue(rightIdAttribute);
							} else {
								unionDataRow[attributeIndex] = Double.NaN;
							}
						} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
							unionDataRow[attributeIndex] = rightExample.getValue(attributeSource.getAttribute());
						}
						attributeIndex++;
					}
					unionTable.addDataRow(new DoubleArrayDataRow(unionDataRow));
				}
				checkForStop();
			}
			break;
		case JOIN_TYPE_OUTER:
			Set<Integer> mappedRightExampleIndices = new HashSet<Integer>(); 
			for (Example leftExample : leftExampleSet) {
				double leftIdValue = leftExample.getValue(leftIdAttribute);
				int[] rightExampleIndices = null;
				if (leftIdAttribute.isNominal()) {
					rightExampleIndices = rightExampleSet.getExampleIndicesFromId(rightIdAttribute.getMapping().getIndex(leftIdAttribute.getMapping().mapIndex((int) leftIdValue)));
				} else {
					rightExampleIndices = rightExampleSet.getExampleIndicesFromId(leftIdValue);
				}
				if (rightExampleIndices != null) {
					for (int rightExampleIndex : rightExampleIndices) {
						mappedRightExampleIndices.add(Integer.valueOf(rightExampleIndex));
						Example rightExample = rightExampleSet.getExample(rightExampleIndex);
						double[] unionDataRow = new double[unionAttributeList.size()];
						int attributeIndex = 0;
						for (AttributeSource attributeSource : originalAttributeSources) {
							if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
								unionDataRow[attributeIndex] = leftExample.getValue(attributeSource.getAttribute());
							} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
								unionDataRow[attributeIndex] = rightExample.getValue(attributeSource.getAttribute());
							}
							attributeIndex++;
						}
						unionTable.addDataRow(new DoubleArrayDataRow(unionDataRow));
					}
				} else {
					double[] unionDataRow = new double[unionAttributeList.size()];
					int attributeIndex = 0;
					for (AttributeSource attributeSource : originalAttributeSources) {
						if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
							unionDataRow[attributeIndex] = leftExample.getValue(attributeSource.getAttribute());
						} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
							unionDataRow[attributeIndex] = Double.NaN;
						}
						attributeIndex++;
					}
					unionTable.addDataRow(new DoubleArrayDataRow(unionDataRow));
				}
				checkForStop();
			}
			int rightExampleIndex = 0;
			for (Example rightExample : rightExampleSet) {
				if (!mappedRightExampleIndices.contains(Integer.valueOf(rightExampleIndex))) {
					double[] unionDataRow = new double[unionAttributeList.size()];
					int attributeIndex = 0;
					for (AttributeSource attributeSource : originalAttributeSources) {
						if (attributeSource.getSource() == AttributeSource.FIRST_SOURCE) {
							// since ID is always taken from left example set, ID value must be fetched
							// from right example set explicitly
							if (attributeSource.getAttribute() == leftIdAttribute) {
								unionDataRow[attributeIndex] = rightExample.getValue(rightIdAttribute);
							} else {
								unionDataRow[attributeIndex] = Double.NaN;
							}
						} else if (attributeSource.getSource() == AttributeSource.SECOND_SOURCE) {
							unionDataRow[attributeIndex] = rightExample.getValue(attributeSource.getAttribute());
						}
						attributeIndex++;
					}
					unionTable.addDataRow(new DoubleArrayDataRow(unionDataRow));		
				}
				rightExampleIndex++;
				checkForStop();
			}
			break;
		}
		return unionTable;
	}

	@Override
	protected boolean isIdNeeded() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_JOIN_TYPE, "Specifies which join should be executed.", JOIN_TYPES, JOIN_TYPE_INNER, false));
		return types;
	}
}
