package com.rapidminer.operator;

/** General purpose interface to convert objects to IOObjects.
 * 
 * @author Simon Fischer
 *
 */
public interface IOObjectConverter<T> {

	public IOObject convert(T object) throws OperatorException;
	
}
