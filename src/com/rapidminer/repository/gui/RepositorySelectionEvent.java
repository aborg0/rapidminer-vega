package com.rapidminer.repository.gui;

import com.rapidminer.repository.Entry;

/**
 * 
 * @author Simon Fischer
 *
 */
public class RepositorySelectionEvent {

	private Entry entry;


	public RepositorySelectionEvent(Entry entry) {
		super();
		this.entry = entry;
	}
	
	public Entry getEntry() {
		return entry;
	}	
}
