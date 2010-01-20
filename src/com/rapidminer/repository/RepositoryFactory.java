package com.rapidminer.repository;

import java.util.List;

/** Repository factories can be registered to create repositories on a per
 *  accessor basis. This is, e.g., useful if repositories require access
 *  privileges.
 * 
 * @author Simon Fischer
 *
 */
public interface RepositoryFactory {

	public List<? extends Repository> createRepositoriesFor(RepositoryAccessor accessor);

}