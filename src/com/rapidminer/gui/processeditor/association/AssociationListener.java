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
package com.rapidminer.gui.processeditor.association;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

import com.rapidminer.Process;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.BinominalMapping;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.container.Pair;

/**
 * ProcessEditor which is responsible for updating the operator recommendations.
 * 
 * @author Marco Boeck
 *
 */
public class AssociationListener extends Observable implements ProcessEditor {
	
	private List<Pair<String, Double>> list = new LinkedList<Pair<String, Double>>();
	

	@Override
	public void processChanged(Process process) {
		setChanged();
		notifyObservers(applyAssociationRules(process));
	}

	@Override
	public void processUpdated(Process process) {
		setChanged();
		notifyObservers(applyAssociationRules(process));
	}

	@Override
	public void setSelection(List<Operator> selection) {
		// not needed
	}
	
	/**
	 * Creates an ExampleSet with one example from the given process.
	 * Each operator key is an attribute with a value of 1
	 * @param process
	 * @return
	 */
	private ExampleSet toExampleSet(Process process) {
		List<Attribute> attributes = new LinkedList<Attribute>();
		List<String> alreadyIncludedOperatorKeys = new LinkedList<String>();
		String key;
		for (Operator op : process.getAllOperators()) {
			key = op.getOperatorDescription().getKey();
			if (alreadyIncludedOperatorKeys.contains(key)) {
				continue;
			}
			Attribute att = AttributeFactory.createAttribute(key, Ontology.BINOMINAL);
			((BinominalMapping)att.getMapping()).setMapping("true", BinominalMapping.POSITIVE_INDEX);
			((BinominalMapping)att.getMapping()).setMapping("false", BinominalMapping.NEGATIVE_INDEX);
			attributes.add(att);
			alreadyIncludedOperatorKeys.add(key);
		}
		double dataRow[] = new double[attributes.size()];
		for (int i = 0; i < dataRow.length; i++) {
			dataRow[i] = BinominalMapping.POSITIVE_INDEX;
		}		
		MemoryExampleTable table = new MemoryExampleTable(attributes);
		table.addDataRow(new DoubleArrayDataRow(dataRow));		
		return table.createExampleSet();
	}
	
	/**
	 * Applies the association rules to get the 10 best operator choices.
	 * 
	 * @param process the current process
	 * @return list of {@link Pair} where {@link Pair#getFirst()} is the operator name and {@link Pair#getSecond()} is the confidence (e.g. 0.15)
	 */
	private List<Pair<String, Double>> applyAssociationRules(Process process) {
		list.clear();
		ExampleSet newExampleSet = toExampleSet(process);
		try {
			Process applyAssociationRulesProcess = new Process(
					"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
					"<process version=\"5.1.003\">" +
						"<context>" +
							"<input>" +
								"<location/>" +
								"<location>//LocalRepository/Prozesse/Test/D01 - Operator Usage Association Rules</location>" +
							"</input>" +
							"<output/>" +
							"<macros/>" +
						"</context>" +
						"<operator activated=\"true\" class=\"process\" compatibility=\"5.1.003\" expanded=\"true\" name=\"Process\">" +
							"<process expanded=\"true\" height=\"464\" width=\"693\">" +
								"<operator activated=\"true\" class=\"apply_association_rules\" compatibility=\"5.1.003\" expanded=\"true\" height=\"76\" name=\"Apply Association Rules\" width=\"90\" x=\"179\" y=\"30\">" +
									"<parameter key=\"confidence_aggregation_method\" value=\"aggregated confidence\"/>" +
								"</operator>" +
								"<connect from_port=\"input 1\" to_op=\"Apply Association Rules\" to_port=\"example set\"/>" +
								"<connect from_port=\"input 2\" to_op=\"Apply Association Rules\" to_port=\"association rules\"/>" +
								"<connect from_op=\"Apply Association Rules\" from_port=\"example set\" to_port=\"result 1\"/>" +
								"<portSpacing port=\"source_input 1\" spacing=\"0\"/>" +
								"<portSpacing port=\"source_input 2\" spacing=\"0\"/>" +
								"<portSpacing port=\"source_input 3\" spacing=\"0\"/>" +
								"<portSpacing port=\"sink_result 1\" spacing=\"0\"/>" +
								"<portSpacing port=\"sink_result 2\" spacing=\"0\"/>" +
							"</process>" +
						"</operator>" +
					"</process>");
			IOContainer ioInput = new IOContainer(new IOObject[]{newExampleSet});
			IOContainer ioResult = applyAssociationRulesProcess.run(ioInput);
			ExampleSet resultSet;
			String name;
			if (ioResult.getElementAt(0) instanceof ExampleSet) {
				resultSet = (ExampleSet)ioResult.getElementAt(0);
				Example example = resultSet.getExample(0);
				Iterator<AttributeRole> iterator = example.getAttributes().specialAttributes();
				while(iterator.hasNext()) {
					AttributeRole role = iterator.next();
					name = role.getSpecialName().substring("confidence(".length());
					if (name.trim().endsWith(")")) {
						name = name.substring(0, name.length()-1);
					}
					list.add(new Pair<String, Double>(name, example.getValue(role.getAttribute())));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XMLException e) {
			e.printStackTrace();
		} catch (OperatorException e) {
			e.printStackTrace();
		}
		Collections.sort(list, new Comparator<Pair<String, Double>>() {

			@Override
			public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
				if (o1.getSecond() == o2.getSecond()) {
					return 0;
				} else if (o1.getSecond() > o2.getSecond()) {
					return 1;
				} else {
					return -1;
				}
			}
			
		});
		int start = list.size() > 10 ? list.size()-10 : 0;
		int end = list.size();
		list = list.subList(start, end);
		return list;
	}

}
