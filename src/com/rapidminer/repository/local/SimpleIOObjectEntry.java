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
package com.rapidminer.repository.local;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.ExampleSetToStream;
import com.rapidminer.operator.tools.IOObjectSerializer;
import com.rapidminer.operator.tools.RMObjectInputStream;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.ProgressListener;

/** Stores IOObject in a file. Either as IOO serialized files using
 *  {@link ExampleSetToStream} where appropriate.
 *  
 * @author Simon Fischer
 *
 */
public class SimpleIOObjectEntry extends SimpleDataEntry implements IOObjectEntry {
	
	private WeakReference<MetaData> metaData = null;	
	
	SimpleIOObjectEntry(String name, SimpleFolder containingFolder, LocalRepository repository) {
		super(name, containingFolder, repository);
	}
	
	private File getDataFile() {
		return new File(((SimpleFolder)getContainingFolder()).getFile(), getName()+".ioo");
	}

	protected File getMetaDataFile() {
		return new File(((SimpleFolder)getContainingFolder()).getFile(), getName()+".md");
	}

	@Override
	public IOObject retrieveData(ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}		
		File dataFile = getDataFile();
		if (dataFile.exists()) {
			try {
				return (IOObject)IOObjectSerializer.getInstance().deserialize(new BufferedInputStream(new FileInputStream(dataFile)));
			} catch (Exception e) {
				throw new RepositoryException("Cannot load data from '"+dataFile+"': "+e, e);
			}
		} else {
			throw new RepositoryException("File '"+dataFile+" does not exist'.");
		}		
	}

	@Override
	public MetaData retrieveMetaData() throws RepositoryException {
		if (metaData != null) {
			MetaData storedData = metaData.get();
			if (storedData != null) {
				return storedData;
			}
		}
		// otherwise metaData == null OR get() == null -> re-read
		MetaData readObject;
		File metaDataFile = getMetaDataFile();
		if (metaDataFile.exists()) {
			ObjectInputStream objectIn = null;
			try {
				objectIn = new RMObjectInputStream(new FileInputStream(metaDataFile));
				readObject = (MetaData)objectIn.readObject();
				this.metaData = new WeakReference<MetaData>(readObject);
				if (readObject instanceof ExampleSetMetaData) {
					for (AttributeMetaData amd : ((ExampleSetMetaData)readObject).getAllAttributes()) {
						if (amd.isNominal()) {
							amd.shrinkValueSet();
						}
					}
				}
				objectIn.close();
			} catch (Exception e) {
				throw new RepositoryException("Cannot load meta data from '"+metaDataFile+"': "+e, e);
			} finally {
				if (objectIn != null) {
					try {
						objectIn.close();
					} catch (IOException e) { }
				}
			}
		} else {
			throw new RepositoryException("Meta data file '"+metaDataFile+" does not exist'.");
		}
		return readObject;
	}
	
	@Override
	public void storeData(IOObject data, Operator callingOperator, ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}
		MetaData md = MetaData.forIOObject(data);
		// Serialize Non-ExampleSets as IOO
		OutputStream out = null;		
		try {
			out = new BufferedOutputStream(new FileOutputStream(getDataFile()));
			IOObjectSerializer.getInstance().serialize(out, data);			
			if (l != null) {
				l.setCompleted(75);
			}
		} catch (Exception e) {
			throw new RepositoryException("Cannot store data at '"+getDataFile()+"': "+e, e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) { }
			}
		}	
		// Save MetaData
		ObjectOutputStream mdOut = null;
		try {
			mdOut = new ObjectOutputStream(new FileOutputStream(getMetaDataFile()));
			mdOut.writeObject(md);
			mdOut.close();
			if (l != null) {
				l.setCompleted(90);
			}
		} catch (Exception e) {
			throw new RepositoryException("Cannot store data at '"+getMetaDataFile()+"': "+e, e);
		} finally {
			if (mdOut != null) {
				try {
					mdOut.close();
				} catch (IOException e) { }
			}
			if (l != null) {
				l.setCompleted(100);
				l.complete();
			}
		}		
		this.metaData = new WeakReference<MetaData>(md);
	}

	@Override
	public String getType() {
		return IOObjectEntry.TYPE_NAME;
	}

	@Override
	public String getDescription() {
		if (metaData != null) {
			MetaData md = metaData.get();
			if (md != null) {
				return md.getDescription();	
			} else {
				return "Simple entry.";
			}			
		} else {
			return "Simple entry.";
		}
	}

	@Override
	public long getSize() {
		if (getDataFile().exists()) {
			return getDataFile().length();
		} else {
			return 0;
		}
	}
	
	@Override
	public void delete() throws RepositoryException {
		if (getDataFile().exists()) {
			getDataFile().delete();
		}		
		if (getMetaDataFile().exists()) {
			getMetaDataFile().delete();
		}
		super.delete();
	}
	
	@Override
	public boolean rename(String newName) {
		renameFile(getDataFile(), newName);
		renameFile(getMetaDataFile(), newName);
		return super.rename(newName);
	}
	
	@Override
	public boolean move(Folder newParent) {	
		moveFile(getDataFile(), ((SimpleFolder)newParent).getFile());
		moveFile(getMetaDataFile(), ((SimpleFolder)newParent).getFile());
		return super.move(newParent);
	};

	@Override
	public long getDate() {
		return getDataFile().lastModified();
	}
	
	@Override
	public boolean willBlock() {
		return (metaData == null) || (metaData.get() == null);
	}
}
