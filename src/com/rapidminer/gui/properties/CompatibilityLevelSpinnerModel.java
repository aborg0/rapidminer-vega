package com.rapidminer.gui.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractSpinnerModel;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorVersion;

/** Displays the {@link OperatorVersion}s as returned by {@link Operator#getIncompatibleVersionChanges()}.
 *  and {@link Operator#getCompatibilityLevel()}.
 * @author Simon Fischer
 *
 */
public class CompatibilityLevelSpinnerModel extends AbstractSpinnerModel {

	private int index = 0;
	private Operator operator;
	private List<OperatorVersion> versions = new LinkedList<OperatorVersion>();
	
	protected void setOperator(Operator operator) {
		this.operator = operator;		
		this.versions = new LinkedList<OperatorVersion>(Arrays.asList(operator.getIncompatibleVersionChanges()));
		OperatorVersion latest = OperatorVersion.getLatestVersion(operator.getOperatorDescription());
		if (!versions.contains(latest)) {
			versions.add(latest);
		}
//		this.index = versions.indexOf(operator.getCompatibilityLevel());
//		fireStateChanged();
		setValue(operator.getCompatibilityLevel());
	}
	
	@Override
	public Object getNextValue() {
		if (index + 1 >= versions.size()) {
			return null;
		} else {
			return versions.get(index + 1);
		}
	}

	@Override
	public Object getPreviousValue() {
		if (index <= 0) {
			return null;
		} else {
			return versions.get(index - 1);
		}
	}

	@Override
	public Object getValue() {
		if (operator != null) {
			return operator.getCompatibilityLevel();
		} else {
			return "-------";
		}
	}

	@Override
	public void setValue(Object value) {
		if (operator != null) {
			if (value instanceof String) {
				value = new OperatorVersion((String) value);
			}
			if (value == null) {
				value = OperatorVersion.getLatestVersion(operator.getOperatorDescription());
			}
			operator.setCompatibilityLevel((OperatorVersion) value);			
			index = versions.indexOf(value);
			if (index == -1) {
				versions.add((OperatorVersion) value);
				Collections.sort(versions);
				index = versions.indexOf(value);
			}
			fireStateChanged();
		}	
	}	
}
