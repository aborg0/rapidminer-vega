package com.rapidminer.repository.db;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.jdbc.ColumnIdentifier;

/**
 * Entry representing an Database Connection.
 * 
 * @author Simon Fischer
 *
 */
public class DBConnectionEntry implements IOObjectEntry {

	private String tableName;
	private DBConnectionConverterFolder folder;
	private MetaData metaData;
	private DBConnectionToIOObjectConverter converter;
	
	public DBConnectionEntry(DBConnectionConverterFolder parent, DBConnectionToIOObjectConverter converter, String name, List<ColumnIdentifier> columns) {
		this.folder = parent;
		this.converter = converter;
		this.tableName = name;
		metaData = converter.convertMetaData(folder.getConnectionEntry(), name, columns);
	}

	@Override
	public int getRevision() {
		return 1;
	}

	@Override
	public long getSize() {
		return -1;
	}

	@Override
	public long getDate() {
		return -1;
	}

	@Override
	public String getName() {	
		return tableName;
	}

	@Override
	public String getType() {
		return IOObjectEntry.TYPE_NAME;
	}

	@Override
	public String getOwner() {
		return folder.getConnectionEntry().getUser();
	}

	@Override
	public String getDescription() {
		return "Table "+getName()+" in "+folder.getConnectionEntry().getURL();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		throw new RepositoryException("Cannot rename items in connection entry.");
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		throw new RepositoryException("Cannot move items in connection entry.");
	}

	@Override
	public Folder getContainingFolder() {
		return folder;
	}

	@Override
	public boolean willBlock() {
		return metaData == null;
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocation(this.folder.getLocation(), getName());
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete() throws RepositoryException {
		throw new RepositoryException("Cannot delete items in connection entry.");
	}

	@Override
	public Collection<Action> getCustomActions() {
		return Collections.emptyList();
	}

	@Override
	public IOObject retrieveData(ProgressListener l) throws RepositoryException {		
		try {
			return converter.convert(folder.getConnectionEntry(), tableName);
		} catch (Exception e) {
			throw new RepositoryException("Failed to read data: "+e, e);
		}
	}

	@Override
	public MetaData retrieveMetaData() throws RepositoryException {
		if (metaData == null) { // cannot happen since assigned in constructor
			metaData = new ExampleSetMetaData();
		}
		return metaData;
	}

	@Override
	public void storeData(IOObject data, Operator callingOperator, ProgressListener l) throws RepositoryException {
		throw new RepositoryException("Cannot store items in connection entry.");
	}

	@Override
	public Class<? extends IOObject> getObjectClass() {
		return metaData.getObjectClass();
	}
}
