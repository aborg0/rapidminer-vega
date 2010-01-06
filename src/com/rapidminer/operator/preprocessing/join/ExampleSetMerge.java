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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;

/**
 * <p>
 * This operator merges two or more given example sets by adding all examples in one example table containing all data
 * rows. Please note that the new example table is built in memory and this operator might therefore not be applicable
 * for merging huge data set tables from a database. In that case other preprocessing tools should be used which
 * aggregates, joins, and merges tables into one table which is then used by RapidMiner.
 * </p>
 * 
 * <p>
 * All input example sets must provide the same attribute signature. That means that all examples sets must have the
 * same number of (special) attributes and attribute names. If this is true this operator simply merges all example sets
 * by adding all examples of all table into a new set which is then returned.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class ExampleSetMerge extends Operator {

	private final InputPortExtender inputExtender = new InputPortExtender("example set", getInputPorts()) {
		@Override
		protected Precondition makePrecondition(InputPort port) {
			return new ExampleSetPrecondition(port) {
				@Override
				public void makeAdditionalChecks(ExampleSetMetaData emd) throws UndefinedParameterError {
					for (MetaData metaData: inputExtender.getMetaData(true)) {
						MetaDataInfo result = emd.equalHeader((ExampleSetMetaData)metaData); 
						if (result == MetaDataInfo.NO) {
							addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), "exampleset.sets_incompatible"));
							break;
						}
						if (result == MetaDataInfo.UNKNOWN) {
							addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "exampleset.sets_incompatible"));
							break;
						}
					}
				}
			};
		}
	};
	private final OutputPort mergedOutput = getOutputPorts().createPort("merged set");

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";

	public ExampleSetMerge(OperatorDescription description) {
		super(description);

		inputExtender.start();

		getTransformer().addRule(inputExtender.makeFlatteningPassThroughRule(mergedOutput));
		getTransformer().addRule(new MDTransformationRule() {
			@Override
			public void transformMD() {
				List<MetaData> metaDatas = inputExtender.getMetaData(true);
				List<ExampleSetMetaData> emds = new ArrayList<ExampleSetMetaData>(metaDatas.size());
				for (MetaData metaData: metaDatas) {
					if (metaData instanceof ExampleSetMetaData) {
						emds.add((ExampleSetMetaData) metaData);
					}
				}

				// now unify all single attributes meta data
				if (emds.size() > 0) {
					ExampleSetMetaData resultEMD = emds.get(0);
					for (int i = 1; i < emds.size(); i++) {
						ExampleSetMetaData mergerEMD = emds.get(i);
						resultEMD.getNumberOfExamples().add(mergerEMD.getNumberOfExamples());

						// now iterating over all single attributes in order to merge their meta data
						for (AttributeMetaData amd : resultEMD.getAllAttributes()) {
							String name = amd.getName();
							AttributeMetaData mergingAMD = mergerEMD.getAttributeByName(name);
							// values
							if (amd.isNominal()) {
								amd.getValueSet().addAll(mergingAMD.getValueSet());
							}else {
								amd.getValueRange().union(mergingAMD.getValueRange());
							}
							amd.getValueSetRelation().merge(mergingAMD.getValueSetRelation());
							// missing values
							amd.getNumberOfMissingValues().add(mergingAMD.getNumberOfMissingValues());
						}
					}
					mergedOutput.deliverMD(resultEMD);
				}
			}
		});
	}
	@Override
	public void doWork() throws OperatorException {
		List<ExampleSet> allExampleSets = inputExtender.getData(true);
		mergedOutput.deliver(merge(allExampleSets));
	}

	ExampleSet merge(List<ExampleSet> allExampleSets) throws OperatorException {
		// throw error if no example sets were available
		if (allExampleSets.size() == 0)
			throw new MissingIOObjectException(ExampleSet.class);

		// checks if all example sets have the same signature
		checkForCompatibility(allExampleSets);

		// create new example table
		ExampleSet firstSet = allExampleSets.get(0);
		List<Attribute> attributeList = new LinkedList<Attribute>();
		Map<Attribute, String> specialAttributes = new HashMap<Attribute, String>();
		Iterator<AttributeRole> a = firstSet.getAttributes().allAttributeRoles();
		while (a.hasNext()) {
			AttributeRole role = a.next();
			Attribute attributeClone = (Attribute) role.getAttribute().clone();
			attributeList.add(attributeClone);
			if (role.isSpecial()) {
				specialAttributes.put(attributeClone, role.getSpecialName());
			}
		}
		MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);

		Iterator<ExampleSet> i = allExampleSets.iterator();
		DataRowFactory factory = new DataRowFactory(getParameterAsInt(PARAMETER_DATAMANAGEMENT), '.');
		while (i.hasNext()) {
			ExampleSet currentExampleSet = i.next();
			Iterator<Example> e = currentExampleSet.iterator();
			while (e.hasNext()) {
				DataRow dataRow = e.next().getDataRow();
				String[] newData = new String[attributeList.size()];
				// Iterator<Attribute> oldAttributes = currentExampleSet.getAttributes().allAttributes();
				Iterator<Attribute> newAttributes = attributeList.iterator();
				int counter = 0;
				while (newAttributes.hasNext()) {
					// Attribute oldAttribute = oldAttributes.next();
					Attribute newAttribute = newAttributes.next();
					Attribute oldAttribute = currentExampleSet.getAttributes().get(newAttribute.getName());
					double oldValue = dataRow.get(oldAttribute);
					if (Double.isNaN(oldValue)) {
						newData[counter] = Attribute.MISSING_NOMINAL_VALUE;
					} else {
						if (newAttribute.isNominal()) {
							newData[counter] = oldAttribute.getMapping().mapIndex((int) oldValue);
						} else {
							newData[counter] = oldValue + "";
						}
					}
					counter++;
				}
				exampleTable.addDataRow(factory.create(newData, exampleTable.getAttributes()));
				checkForStop();
			}
		}

		// create result example set
		ExampleSet resultSet = exampleTable.createExampleSet(specialAttributes);
		return resultSet;
	}

	private void checkForCompatibility(List<ExampleSet> allExampleSets) throws OperatorException {
		ExampleSet first = allExampleSets.get(0);
		Iterator<ExampleSet> i = allExampleSets.iterator();
		while (i.hasNext()) {
			checkForCompatibility(first, i.next());
		}
	}

	private void checkForCompatibility(ExampleSet first, ExampleSet second) throws OperatorException {
		if (first.getAttributes().allSize() != second.getAttributes().allSize()) {
			throw new UserError(this, 925, "numbers of attributes are different");
		}

		Iterator<Attribute> firstIterator = first.getAttributes().allAttributes();
		while (firstIterator.hasNext()) {
			Attribute firstAttribute = firstIterator.next();
			Attribute secondAttribute = second.getAttributes().get(firstAttribute.getName());
			if (secondAttribute == null)
				throw new UserError(this, 925, "attribute with name '" + firstAttribute.getName() + "' is not part of second example set.");
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_DATAMANAGEMENT, "Determines, how the data is represented internally.", DataRowFactory.TYPE_NAMES, DataRowFactory.TYPE_DOUBLE_ARRAY));

		// deprecated parameter
		ParameterType type = new ParameterTypeCategory("merge_type", "Indicates if all input example sets or only the first two example sets should be merged.", new String[] { "all", "first_two" }, 0);
		type.setDeprecated();
		types.add(type);
		return types;
	}
}
