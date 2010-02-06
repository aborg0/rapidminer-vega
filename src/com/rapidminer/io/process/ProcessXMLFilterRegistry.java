package com.rapidminer.io.process;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;

/**
 * 
 * @author Simon Fischer
 *
 */
public class ProcessXMLFilterRegistry {

	private static final Object LOCK = new Object();
	
	private static final List<ProcessXMLFilter> FILTERS = new LinkedList<ProcessXMLFilter>();
	
	public static void registerFilter(ProcessXMLFilter filter) {
		synchronized (LOCK) {
			FILTERS.add(filter);
		}
	}
	
	protected static void fireOperatorExported(Operator operator, Element element) {
		for (ProcessXMLFilter filter : FILTERS) {
			filter.operatorExported(operator, element);
		}
	}
	
	protected static void fireOperatorImported(Operator operator, Element element) {
		for (ProcessXMLFilter filter : FILTERS) {
			filter.operatorImported(operator, element);
		}
	}

	protected static void fireExecutionUnitExported(ExecutionUnit unit, Element element) {
		for (ProcessXMLFilter filter : FILTERS) {
			filter.executionUnitExported(unit, element);
		}
	}
	
	protected static void fireProcessImported(ExecutionUnit unit, Element element) {
		for (ProcessXMLFilter filter : FILTERS) {
			filter.executionUnitImported(unit, element);
		}	
	}

}
