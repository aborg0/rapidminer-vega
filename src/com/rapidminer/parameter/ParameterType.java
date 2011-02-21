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
package com.rapidminer.parameter;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.MacroHandler;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;


/**
 * A ParameterType holds information about type, range, and default value of a
 * parameter. Lists of ParameterTypes are provided by operators.
 * 
 * @author Ingo Mierswa, Simon Fischer
 * @see com.rapidminer.operator.Operator#getParameterTypes()
 */
public abstract class ParameterType implements Comparable, Serializable {

    private static final long serialVersionUID = 5296461242851710130L;

    /** The key of this parameter. */
    private String key;

    /** The documentation. Used as tooltip text... */
    private String description;

    /**
     * Indicates if this is a parameter only viewable in expert mode. Mandatory
     * parameters are always viewable. The default value is true.
     */
    private boolean expert = true;

    /**
     * Indicates if this parameter is hidden and is not shown in the GUI.
     * May be used in conjunction with a configuration wizard which lets the
     * user configure the parameter.
     */
    private boolean isHidden = false;

    /** Indicates if the range should be displayed. */
    private boolean showRange = true;

    /**
     * Indicates that this parameter is deprecated and remains only for compatibility reasons during
     * loading of older processes.
     * It should neither be shown nor documented.
     */
    private boolean isDeprecated = false;

    /**
     * This collection assembles all conditions to be met to show this parameter within the gui.
     */
    private final Collection<ParameterCondition> conditions = new LinkedList<ParameterCondition>();


    /** Creates a new ParameterType. */
    public ParameterType(String key, String description) {
        this.key = key;
        this.description = description;
    }

    public abstract Element getXML(String key, String value, boolean hideDefault, Document doc);

    /** Returns a human readable description of the range. */
    public abstract String getRange();

    /** Returns a value that can be used if the parameter is not set. */
    public abstract Object getDefaultValue();

    /**
     * Returns the correct string representation of the default value. If the default is
     * undefined, it returns null.
     */
    public String getDefaultValueAsString() {
        return toString(getDefaultValue());
    }

    /** Sets the default value. */
    public abstract void setDefaultValue(Object defaultValue);

    /** Returns true if the values of this parameter type are numerical, i.e. might be parsed
     *  by {@link Double#parseDouble(String)}. Otherwise false should be returned. This method
     *  might be used by parameter logging operators. */
    public abstract boolean isNumerical();

    /** Writes an xml representation of the given key-value pair.
     *  @deprecated Use the DOM version of this method.  At the moment, we cannot delete it, because {@link Parameters#equals(Object)}
     *  and {@link Parameters#hashCode()} rely on it. */
    @Deprecated
    public abstract String getXML(String indent, String key, String value, boolean hideDefault);


    public boolean showRange() {
        return showRange;
    }

    public void setShowRange(boolean showRange) {
        this.showRange = showRange;
    }

    /** This method will be invoked by the Parameters after a parameter was set.
     *  The default implementation is empty but subclasses might override this
     *  method, e.g. for a decryption of passwords. */
    public String transformNewValue(String value) {
        return value;
    }

    /**
     * Returns true if this parameter can only be seen in expert mode. The
     * default implementation returns true if the parameter is optional.
     * Please note that this method cannot be accessed during getParameterTypes() method invocations,
     * because it relies on getting the Parameters object, which is then not created.
     */
    public boolean isExpert() {
        return expert;
    }

    /**
     * Sets if this parameter can be seen in expert mode (true) or beginner mode
     * (false).
     * 
     */
    public void setExpert(boolean expert) {
        this.expert = expert;
    }

    /**
     * Returns true if this parameter is hidden or not all dependency conditions are fulfilled.
     * Then the parameter will not be shown in the
     * GUI. The default implementation returns true which should be the normal case.
     * 
     * Please note that this method cannot be accessed during getParameterTypes() method invocations,
     * because it relies on getting the Parameters object, which is then not created.
     */
    public boolean isHidden() {
        boolean conditionsMet = true;
        for (ParameterCondition condition : conditions) {
            conditionsMet &= condition.dependencyMet();
        }
        return isDeprecated || isHidden || !conditionsMet;
    }

    public Collection<ParameterCondition> getConditions() {
        return Collections.unmodifiableCollection(conditions);
    }

    /**
     * Sets if this parameter is hidden (value true) and will not be shown in the GUI.
     */
    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    /**
     * This returns whether this parameter is deprecated.
     */
    public boolean isDeprecated() {
        return this.isDeprecated;
    }

    /**
     * This method indicates that this parameter is deprecated and isn't used anymore beside from
     * loading old process files.
     */
    public void setDeprecated() {
        this.isDeprecated = true;
    }

    /** Registers the given dependency condition. */
    public void registerDependencyCondition(ParameterCondition condition) {
        this.conditions.add(condition);
    }

    public Collection<ParameterCondition> getDependencyConditions() {
        return this.conditions;
    }

    /**
     * Returns true if this parameter is optional. The default implementation
     * returns true.
     * Please note that this method cannot be accessed during getParameterTypes() method invocations,
     * because it relies on getting the Parameters object, which is then not created.
     * 
     */
    public boolean isOptional() {
        boolean becomeMandatory = false;
        for (ParameterCondition condition : conditions) {
            if (condition.dependencyMet()) {
                becomeMandatory |= condition.becomeMandatory();
            } else {
                return true;
            }
        }
        return !becomeMandatory;
    }

    /** Sets the key. */
    public void setKey(String key) {
        this.key = key;
    }

    /** Returns the key. */
    public String getKey() {
        return key;
    }

    /** Returns a short description. */
    public String getDescription() {
        return description;
    }

    /** Sets the short description. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** This method gives a hook for the parameter type to react on a renaming of an operator.
     * It must return the correctly modified String value. The default implementation does nothing.
     */
    public String notifyOperatorRenaming(String oldOperatorName, String newOperatorName, String parameterValue) {
        return parameterValue;
    }

    /** Returns a string representation of this value. */
    public String toString(Object value) {
        if (value == null)
            return "";
        else
            return value.toString();
    }

    public String toXMLString(Object value) {
        return Tools.escapeXML(toString(value));
    }

    @Override
    public String toString() {
        return key + " (" + description + ")";
    }

    /**
     * Can be called in order to report an illegal parameter value which is
     * encountered during <tt>checkValue()</tt>.
     */
    public void illegalValue(Object illegal, Object corrected) {
        LogService.getGlobal().log("Illegal value '" + illegal + "' for parameter '" + key + "' has been corrected to '" + corrected.toString() + "'.", LogService.WARNING);
    }

    /** ParameterTypes are compared by key. */
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof ParameterType))
            return 0;
        else
            return this.key.compareTo(((ParameterType) o).key);
    }

    /**
     *  This method operates on the internal string representation of parameter values
     *  and replaces macro expressions of the form %{macroName}.
     * 
     *  NOTE: This method will soon be removed or changed again since the internal representation
     *  of parameter values will no longer be strings. Then, this method will accept an Object,
     *  (possibly using generics) as input.
     */
    public abstract String substituteMacros(String parameterValue, MacroHandler mh);
}
