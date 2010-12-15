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


/**
 * This is an abstract class for all ParameterTypes that are a combination
 * of several other {@link ParameterType}s.
 * 
 * @author Sebastian Land
 *
 */
public abstract class CombinedParameterType extends ParameterType {

	private static final long serialVersionUID = 1674072082952288334L;

	private ParameterType[] types;
	/**
	 * @param key
	 * @param description
	 */
	public CombinedParameterType(String key, String description, ParameterType...types) {
		super(key, description);
		this.types = types;
	}

	public boolean containsType(Class<? extends ParameterType> type) {
		for (ParameterType member: types)
			if (type.isAssignableFrom(member.getClass()))
				return true;
		return false;
	}
}
