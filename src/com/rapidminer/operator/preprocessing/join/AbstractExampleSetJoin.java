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
package com.rapidminer.operator.preprocessing.join;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetUnionRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.Ontology;


/**
 * <p>
 * Build the join of two example sets.
 * </p>
 * <p>
 * Please note that this check for duplicate attributes will only be applied 
 * for regular attributes. Special attributes of the second input example set
 * which do not exist in the first example set will simply be added. If they 
 * already exist they are simply skipped.
 * </p>
 * 
 * @author Ingo Mierswa
 */
public abstract class AbstractExampleSetJoin extends Operator {

	private InputPort leftInput = getInputPorts().createPort("left");
	private InputPort rightInput = getInputPorts().createPort("right");
	private OutputPort joinOutput = getOutputPorts().createPort("join");
	
	/** The parameter name for &quot;Indicates if double attributes should be removed or renamed&quot; */
	public static final String PARAMETER_REMOVE_DOUBLE_ATTRIBUTES = "remove_double_attributes";
	
    /** Helper class to find the correct data for all union attributes. */
    protected static class AttributeSource {
    	
        protected static final int FIRST_SOURCE = 1;

        protected static final int SECOND_SOURCE = 2;

        protected int source;

        protected Attribute attribute;

        public AttributeSource(int source, Attribute attribute) {
            this.source = source;
            this.attribute = attribute;
        }
        
        protected int getSource() {
            return source;
        }

        protected Attribute getAttribute() {
            return attribute;
        }
    }

    public AbstractExampleSetJoin(OperatorDescription description) {
        super(description);
        if (isIdNeeded()) {
        	leftInput.addPrecondition(new ExampleSetPrecondition(leftInput, Ontology.ATTRIBUTE_VALUE, Attributes.ID_NAME));
        	rightInput.addPrecondition(new ExampleSetPrecondition(rightInput, Ontology.ATTRIBUTE_VALUE, Attributes.ID_NAME));
        } else {
        	leftInput.addPrecondition(new ExampleSetPrecondition(leftInput));
        	rightInput.addPrecondition(new ExampleSetPrecondition(rightInput));
        }
        getTransformer().addRule(new ExampleSetUnionRule(rightInput, leftInput, joinOutput, "_from_ES2") {
        	 @Override
        	 protected String getPrefix() {
        		 return getParameterAsBoolean(PARAMETER_REMOVE_DOUBLE_ATTRIBUTES) ? null : "_from_ES2";
        	 }
        });
    }

    protected abstract MemoryExampleTable joinData(ExampleSet es1, ExampleSet es2, List<AttributeSource> originalAttributeSources, List<Attribute> unionAttributeList) throws OperatorException;

    protected abstract boolean isIdNeeded();
    
    @Override
	public void doWork() throws OperatorException {
        // please note the order of calls: the second data generator in the tree will be the first delivered
        ExampleSet es2 = leftInput.getData();
        ExampleSet es1 = rightInput.getData();
        if (this.isIdNeeded()) {
            Attribute id1 = es1.getAttributes().getId();
            Attribute id2 = es2.getAttributes().getId();

            // sanity checks
            if ((id1 == null) || (id2 == null)) {
                throw new UserError(this, 129);
            }
            if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(id1.getValueType(), id2.getValueType()) 
        	    && !Ontology.ATTRIBUTE_VALUE_TYPE.isA(id2.getValueType(), id1.getValueType()) ){
//            if (id1.getValueType() != id2.getValueType()) {
                throw new UserError(this, 120, new Object[] {
                        id2.getName(), Ontology.VALUE_TYPE_NAMES[id2.getValueType()], Ontology.VALUE_TYPE_NAMES[id1.getValueType()]
                });
            }
        }
        
        // regular attributes
        List<AttributeSource> originalAttributeSources = new LinkedList<AttributeSource>();
        List<Attribute> unionAttributeList = new LinkedList<Attribute>();
        for (Attribute attribute : es1.getAttributes()) {
            originalAttributeSources.add(new AttributeSource(AttributeSource.FIRST_SOURCE, attribute));
            unionAttributeList.add((Attribute) attribute.clone());
        }
        
        for (Attribute attribute : es2.getAttributes()) {
            Attribute cloneAttribute = (Attribute) attribute.clone();
            if (containsAttribute(unionAttributeList, attribute)) { // in list...
                if (!getParameterAsBoolean(PARAMETER_REMOVE_DOUBLE_ATTRIBUTES)) { // ... but should not be removed --> rename
                    originalAttributeSources.add(new AttributeSource(AttributeSource.SECOND_SOURCE, attribute));
                    cloneAttribute.setName(cloneAttribute.getName() + "_from_ES2");
                    if (containsAttribute(unionAttributeList, cloneAttribute)) {
                        cloneAttribute.setName(cloneAttribute.getName() + "_from_ES2");
                    }
                    unionAttributeList.add(cloneAttribute);
                } // else do nothing, i.e. remove
            } else { // not in list --> add
                originalAttributeSources.add(new AttributeSource(AttributeSource.SECOND_SOURCE, attribute));
                unionAttributeList.add(cloneAttribute);
            }
        }

        // special attributes
        Map<Attribute, String> unionSpecialAttributes = new HashMap<Attribute, String>();
        Set<String> usedSpecialAttributes = new HashSet<String>();
        
        // first example set's special attributes
        Iterator<AttributeRole> s = es1.getAttributes().specialAttributes();
        while (s.hasNext()) {
            AttributeRole role = s.next();
            Attribute specialAttribute = role.getAttribute();
            Attribute specialAttributeClone = (Attribute) specialAttribute.clone();
            originalAttributeSources.add(new AttributeSource(AttributeSource.FIRST_SOURCE, specialAttribute));
            unionAttributeList.add(specialAttributeClone);
            unionSpecialAttributes.put(specialAttributeClone, role.getSpecialName());
            usedSpecialAttributes.add(role.getSpecialName());
        }
        // second example set's special attributes
        s = es2.getAttributes().specialAttributes();
        while (s.hasNext()) {
            AttributeRole role = s.next();
            String specialName = role.getSpecialName();
            Attribute specialAttribute = role.getAttribute();
            if (!usedSpecialAttributes.contains(specialName)) { // not there
                originalAttributeSources.add(new AttributeSource(AttributeSource.SECOND_SOURCE, specialAttribute));
                Attribute specialAttributeClone = (Attribute) specialAttribute.clone();
                unionAttributeList.add(specialAttributeClone);
                unionSpecialAttributes.put(specialAttributeClone, specialName);
                usedSpecialAttributes.add(specialName);
            } else {
                logWarning("Special attribute '" + specialName + "' already exist, skipping!");
            }
        }

        // join data
        MemoryExampleTable unionTable = joinData(es1, es2, originalAttributeSources, unionAttributeList);

        // create new example set
        joinOutput.deliver(unionTable.createExampleSet(unionSpecialAttributes));
    }

    /**
     * Returns true if the list already contains an attribute with the given name. The method contains from List cannot be used since the equals method of Attribute also checks for the same table
     * index which is not applicable here.
     */
    public boolean containsAttribute(List<Attribute> attributeList, Attribute attribute) {
        Iterator<Attribute> i = attributeList.iterator();
        while (i.hasNext()) {
            if (i.next().getName().equals(attribute.getName()))
                return true;
        }
        return false;
    }

    @Override
	public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeBoolean(PARAMETER_REMOVE_DOUBLE_ATTRIBUTES, "Indicates if double attributes should be removed or renamed", true));
        return types;
    }
}
