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
package com.rapidminer.tools.container;

/**
 * A basic container class for a pair of objects.
 * 
 * @author Sebastian Land
 */
public class Pair<T, K> {

	private T first;

	private K second;

	public Pair(T t, K k) {
		this.setFirst(t);
		this.setSecond(k);
	}

	public T getFirst() {
		return first;
	}

	public void setFirst(T first) {
		this.first = first;
	}

	public K getSecond() {
		return second;
	}

	public void setSecond(K second) {
		this.second = second;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair))
			return false;
		Pair a = (Pair) o;
		return a.getFirst().equals(getFirst()) && a.getSecond().equals(getSecond());
	}

	@Override
	public int hashCode() {
		return this.getFirst().hashCode() ^ this.getSecond().hashCode();
	}

	@Override
	public String toString() {
		String tString = (getFirst() == null) ? "null" : getFirst().toString();
		String kString = (getSecond() == null) ? "null" : getSecond().toString();
		return tString + " : " + kString;
	}
}
