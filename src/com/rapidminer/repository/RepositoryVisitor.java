package com.rapidminer.repository;

/** Used by the visitor pattern defined in {@link RepositoryManager#walk(Entry, RepositoryVisitor, Class)}.
 * 
 * @author Simon Fischer
 *
 */
public interface RepositoryVisitor<T extends Entry> {

	/**
	 * 
	 * @return true iff children should be visited
	 */
	public boolean visit(T entry);
	
}
