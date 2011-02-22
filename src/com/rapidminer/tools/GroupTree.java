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
package com.rapidminer.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.documentation.GroupDocumentation;
import com.rapidminer.tools.documentation.OperatorDocBundle;


/**
 * A group tree manages operator descriptions in a tree like manner. This is
 * useful to present the operators in groups and subgroups and eases operator
 * selection in the GUI.
 * 
 * @author Ingo Mierswa
 */
public class GroupTree implements Comparable<GroupTree> {

	/** Map for group name &lt;-&gt; group (list). */
	static final GroupTree ROOT = new GroupTree(null, "", null);

	private static final ImageIcon[] NO_ICONS = new ImageIcon[3];
	
	/** The list of operators in this group. */
	private final List<OperatorDescription> operators = new LinkedList<OperatorDescription>();

	/** The subgroups of this group. */
	//private final Map<String, GroupTree> children = new TreeMap<String, GroupTree>();
	private final Map<String, GroupTree> children = new LinkedHashMap<String, GroupTree>();

	/** The key used for mapping I18N support. */
	private String key = null;

	/** The parent of this group. */
	private GroupTree parent = null;

	private String iconName;
	
	private ImageIcon[] icons;

	private final GroupDocumentation documentation;
	
	/** Creates a new group tree with no operators and children. */
	private GroupTree(GroupTree parent, String key, OperatorDocBundle bundle) {
		this.parent = parent;
		this.key = key;		
		if (bundle != null) {			
			this.documentation = ((GroupDocumentation)bundle.getObject("group."+getQName()));
		} else {
			LogService.getRoot().fine("No documentation bundle associated with group "+getQName());
			this.documentation = new GroupDocumentation(key);
		}
	}

    /** Clone constructor. */
    private GroupTree(GroupTree other) {
        this.key = other.key;
        this.operators.addAll(other.operators);
        this.iconName = other.iconName;
        this.icons = other.icons;
        this.documentation = other.documentation;
        Iterator<GroupTree> g = other.getSubGroups().iterator();
        while (g.hasNext()) {
            GroupTree child = g.next();
            addSubGroup((GroupTree)child.clone());
        }
    }

//    private void sort() {
//    	Collections.sort(operators,
//    			new Comparator<OperatorDescription>() {
//    		@Override
//    		public int compare(OperatorDescription o1, OperatorDescription o2) {
//    			return o1.getName().compareTo(o2.getName());
//    		}
//    	});
//    }    
    
    /** Returns a deep clone of this tree. */
    @Override
	public Object clone() {
        return new GroupTree(this);
    }
    
	/** Returns the name of this group. */
	public String getName() {
		return getDocumentation().getName();
	}

	private GroupDocumentation getDocumentation() {
		return documentation;		
	}

    /** Returns the main group name, i.e. the name of the first parent group under the root. */
    public String getMainGroupName() {
        if (getParent() == null) {
            return "Root";
        } else {
            if (getParent().getParent() == null)
                return getName();
            else
                return getParent().getMainGroupName();
        }
    }
    
	/** Sets the parent of this group. */
	private void setParent(GroupTree parent) {
		this.parent = parent;
	}

	/** Returns the parent of this group. Returns null if no parent does exist. */
	public GroupTree getParent() {
		return parent;
	}

	/** Adds a subgroup to this group. */
	private void addSubGroup(GroupTree child) {
		children.put(child.key, child);
		child.setParent(this);
	}

	/** Returns or creates the subgroup with the given name. */
	public GroupTree getSubGroup(String key) {
		return children.get(key);
	}

	/** Returns or creates the subgroup with the given name, creating it if not present. */
	public GroupTree getOrCreateSubGroup(String key, OperatorDocBundle bundle) {
		GroupTree child = children.get(key);
		if (child == null) {
			child = new GroupTree(this, key, bundle);
			addSubGroup(child);
		}
		return child;
	}

	/** Returns a set of all children group trees. */
	public Collection<GroupTree> getSubGroups() {
		return children.values();
	}

	/** Returns the index of the given subgroup or -1 if the sub group is not a child of this node. */
	public int getIndexOfSubGroup(GroupTree child) {
		Iterator<GroupTree> i = getSubGroups().iterator();
		int index = 0;
		while (i.hasNext()) {
			GroupTree current = i.next();
			if (current.equals(child))
				return index;
			index++;
		}
		return -1;
	}
	
	/** Returns the i-th sub group. */
	public GroupTree getSubGroup(int index) {
		Collection<GroupTree> allChildren = getSubGroups();
		if (index >= allChildren.size()) {
			return null;
		} else {
			Iterator<GroupTree> i = allChildren.iterator();
			int counter = 0;
			while (i.hasNext()) {
				GroupTree current = i.next();
				if (counter == index)
					return current;
				counter++;
			}
			return null;
		}
	}
	
	/** Adds an operator to this group. */
	public void addOperatorDescription(OperatorDescription description) {
		operators.add(description);
	}

	/**
	 * Returns all operator descriptions in this group or an empty list if this
	 * group does not contain any operators.
	 */
	public List<OperatorDescription> getOperatorDescriptions() {
		return operators;
	}

	/**
	 * Returns all operator in this group and recursively the operators of all
	 * children.
	 */
	public Set<OperatorDescription> getAllOperatorDescriptions() {
		Set<OperatorDescription> result = new TreeSet<OperatorDescription>();
		addAllOperatorDescriptions(result);
		return result;
	}

	private void addAllOperatorDescriptions(Set<OperatorDescription> operators) {
		operators.addAll(this.operators);
		Iterator<GroupTree> i = children.values().iterator();
		while (i.hasNext()) {
			GroupTree child = i.next();
			child.addAllOperatorDescriptions(operators);
		}
	}

	@Override
	public String toString() {
		String result = getName();
		if (getParent() == null) 
			result = "Root";
		int size = countOperators();
		return result + (size > 0 ? " (" + size + ")" : "");
	}

    public int compareTo(GroupTree o) {
        return this.getName().compareTo(o.getName());
    }
    
    @Override
	public boolean equals(Object o) {
		if (!(o instanceof GroupTree))
			return false;
		GroupTree a = (GroupTree) o;
		if (!this.key.equals(a.key))
			return false;
		return true;
    }
    
    @Override
	public int hashCode() {
    	return this.key.hashCode();
    }

    /** Gets the qualified (dot separated) name of the group. */
	public String getQName() {
		if (parent == null) {
			return key;
		} else {
			String parentKey = parent.getQName();
			if (parentKey.length() > 0) {
				return parentKey + "." + key;
			} else {
				return key;
			}
		}
	}
	
	public String getGroupName() {
		String name = getName(); 
		if (parent == null) {
			return name;
		} else {
			String parentName = parent.getGroupName();
			if (parentName.length() > 0) {
				return parentName + "." + name;
			} else {
				return name;
			}
		}
	}

	/** Finds the group for the given qualified name (dot separated), creating subgroups along
	 *  the path as necessary.  */
	public static GroupTree findGroup(String qName, OperatorDocBundle bundle) {
		if (qName.isEmpty()) {
			return ROOT;
		} else {
			return ROOT.findGroup2(qName, bundle);
		}
	}
		
	private GroupTree findGroup2(String operatorGroup, OperatorDocBundle bundle) {
		String splitted[] = operatorGroup.split("\\.", 2);
		if (splitted.length == 1) {
			return getOrCreateSubGroup(splitted[0], bundle);
		} else {
			return getOrCreateSubGroup(splitted[0], bundle).findGroup2(splitted[1], bundle);
		}
		
	}

	public void setIconName(String icon) {
		this.iconName = icon;
		loadIcons();
	}
	
	public String getIconName() {
		if (this.iconName != null) {
			return this.iconName;
		} else if (this.parent != null) {
			return parent.getIconName();
		} else {
			return null;
		}
	}

	private void loadIcons() {
		if (iconName == null) {
			icons = null;
			return;
		}
		icons = new ImageIcon[3];
		icons[0] = SwingTools.createIcon("16/"+iconName);
		icons[1] = SwingTools.createIcon("24/"+iconName);
		icons[2] = SwingTools.createIcon("48/"+iconName);
	}
	
	public ImageIcon[] getIcons() {
		if (icons != null) {
			return icons;
		} else if (parent != null) {
			return parent.getIcons();
		} else {
			return NO_ICONS;
		}
	}

	public String getDescription() {
		return documentation.getHelp();
	}
	
	public String getKey() {
		return key;
	}
	
	private int countOperators() {
		int count = operators.size();
		for (GroupTree tree : children.values()) {
			count += tree.countOperators();
		}
		return count;
		
	}

	public void sort(Comparator<OperatorDescription> comparator) {
		Collections.sort(operators, comparator);
		for (GroupTree child : children.values()) {
			child.sort(comparator);
		}		
	}
}
