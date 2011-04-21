package com.rapidminer.parameter;

import com.rapidminer.operator.ports.MetaDataChangeListener;
import com.rapidminer.operator.ports.metadata.MetaData;

/** Always returns the same meta data. Listeners are ignored because meta data never changes.
 * 
 * @author Simon Fischer
 *
 */
public class StaticMetaDataProvider implements MetaDataProvider {

	private final MetaData metaData;
	
	public StaticMetaDataProvider(MetaData metaData) {
		this.metaData = metaData;
	}

	@Override
	public MetaData getMetaData() {
		return metaData;
	}

	@Override
	public void addMetaDataChangeListener(MetaDataChangeListener l) {
		// nothing to do: meta data does not change
	}

	@Override
	public void removeMetaDataChangeListener(MetaDataChangeListener l) {
		// nothing to do: meta data does not change
	}

}
