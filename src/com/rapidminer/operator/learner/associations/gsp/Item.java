/**
 * 
 */
package com.rapidminer.operator.learner.associations.gsp;

import java.io.Serializable;

/**
 * @author Sebastian Land
 *
 */
public class Item implements Comparable<Item>, Serializable {

	private static final long serialVersionUID = 34234L;
	private String name;
	private int index;
	
	public Item(String name, int i) {
		this.name = name;
		this.index = i;
	}
	
	@Override
	public int hashCode() {
		return index;
	}

	public int getIndex() {
		return index;
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(Item o) {
		return Integer.signum(index - o.index);
	}
}
