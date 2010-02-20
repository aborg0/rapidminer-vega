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
package com.rapidminer.repository.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import com.rapid_i.repository.wsimport.EntryResponse;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.remote.RemoteRepository.EntryStreamType;

/**
 * Reference on BLOB entries in the repository, if using a remote repository.
 * 
 * @author Simon Fischer
 */
public class RemoteBlobEntry extends RemoteDataEntry implements BlobEntry {

	private String mimeType;

	RemoteBlobEntry(EntryResponse response, RemoteFolder container, RemoteRepository repository) {
		super(response, container, repository);
	}
	
	@Override
	public String getMimeType() {
		return mimeType;
	}

	@Override
	public InputStream openInputStream() throws RepositoryException {
		try {
			HttpURLConnection conn = getRepository().getHTTPConnection(getLocation().getPath(), EntryStreamType.BLOB);
			conn.setDoOutput(false);
			conn.setDoInput(true);
			try {
				return conn.getInputStream();
			} catch (IOException e) {
				throw new RepositoryException("Cannot download object: " + conn.getResponseCode()+": "+conn.getResponseMessage(), e);	
			}
		} catch (IOException e) {
			throw new RepositoryException("Cannot open connection to '"+getLocation()+"': "+e, e);
		}		
	}

	@Override
	public OutputStream openOutputStream(String mimeType) throws RepositoryException {
		try {
			HttpURLConnection conn = getRepository().getHTTPConnection(getLocation().getPath(), EntryStreamType.BLOB);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestProperty("Content-Type", mimeType);
			OutputStream out;
			try {
				out = conn.getOutputStream();
			} catch (IOException e) {
				throw new RepositoryException("Cannot upload object: " + conn.getResponseCode()+": "+conn.getResponseMessage(), e);
			}
			return out;
//			Tools.copyStreamSynchronously(in, out, true);
//			String returnMessage = Tools.readTextFile(new InputStreamReader(conn.getInputStream()));
//			LogService.getRoot().fine("Reply from server: "+returnMessage);
		} catch (IOException e) {
			throw new RepositoryException("Cannot open connection to '"+getLocation()+"': "+e, e);
		}		
	}

	@Override
	public boolean willBlock() {
		return false;
	}
}
