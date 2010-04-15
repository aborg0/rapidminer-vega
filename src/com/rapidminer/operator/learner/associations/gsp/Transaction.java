/**
 * 
 */
package com.rapidminer.operator.learner.associations.gsp;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Sebastian Land
 *
 */
public class Transaction extends ArrayList<Item> {

	private static final long serialVersionUID = 8725134393133916536L;

	private double time;
	
	public Transaction(double time, Item...items) {
		this.time = time;
		for(Item item: items)
			super.add(item);
		Collections.sort(this);
	}
	
	public Transaction(Transaction transaction) {
		this.addAll(transaction);
	}
	
	public Item getLastItem() {
		return get(size() - 1);
	}

	public double getTime() {
		return time;
	}

	@Override
	public boolean add(Item item) {
		if (!contains(item)) {
			super.add(item);
			Collections.sort(this);
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		String separator = "";
		for (Item item: this) {
			buffer.append(separator + item.toString());
			separator = ", ";
		}
		return buffer.toString();
	}
}
