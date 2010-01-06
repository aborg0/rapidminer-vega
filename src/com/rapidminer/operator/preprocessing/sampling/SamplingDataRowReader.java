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
package com.rapidminer.operator.preprocessing.sampling;

import java.util.Iterator;

import com.rapidminer.example.Example;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowReader;

/** A DataRowReader that skips DataRows from a parent.
 * 
 * @author Simon Fischer
 *  
 */
public abstract class SamplingDataRowReader implements DataRowReader {

	private final Iterator<Example> delegate;
	private DataRow next;
	private boolean isInitilized = false;
	
	protected SamplingDataRowReader(Iterator<Example> delegate) {
		super();
		this.delegate = delegate;
	}
	
	public abstract boolean uses(Example example);

	private void toNext() {
		isInitilized = true;
		while (true) {
			if (!delegate.hasNext()) {
				next = null;
				break;
			}
			Example example = delegate.next();
			if (uses(example)) {
				next = example.getDataRow();
				break;
			}			
		}
	}
	
	@Override
	public boolean hasNext() {
		if (!isInitilized) {
			toNext();
		}
		return (next != null);
	}

	@Override
	public DataRow next() {
		if (!isInitilized) {
			toNext();
		}
		DataRow result = next;
		toNext();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Cannot remove data rows during sampling.");
	}

}
