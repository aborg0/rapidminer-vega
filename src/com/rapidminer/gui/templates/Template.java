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
package com.rapidminer.gui.templates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import com.rapidminer.tools.LogService;

/**
 * A template process consisting of name, short description, a name for an
 * process file and a list parameters given as String pairs (operator, key).
 * Templates must look like this:
 * 
 * <pre>
 *   one line for the name
 *   one line of html description
 *   one line for the process file name
 *   Rest of the file: some important parameters in the form operatorname.parametername
 * </pre>
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class Template {

	public static final int PREDEFINED = 0;
	
	public static final int USER_DEFINED = 1;
	
	public static final int ALL = 2;
	
	private String name = "unnamed";

	private String description = "none";

	private String configResource;

	private Set<OperatorParameterPair> parameters = new TreeSet<OperatorParameterPair>();

	private File templateFile = null;

	public Template() {}

	
	public Template(File file) throws IOException {
		this(new FileInputStream(file));
		this.templateFile = file;
	}
		
	public Template(InputStream ins) throws IOException {		
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
			name = in.readLine();
			description = in.readLine();
			configResource = in.readLine();
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] split = line.split("\\.");
				if (split.length == 2) {
					parameters.add(new OperatorParameterPair(split[0], split[1]));
				} else {
					throw new IOException("Malformed operator parameter pair: "+line);
				}
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Cannot read template file: " + e, e);
			throw e;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, "Cannot close stream to template file: " + e.getMessage(), e);
				}
			}
		}
	}

	public Template(String name, String description, String configFile, Set<OperatorParameterPair> parameters) {
		this.name = name;
		this.description = description;
		this.configResource = configFile;
		this.parameters = parameters;
	}

	public File getFile() {
		return templateFile;
	}
	
	public InputStream getProcessStream() throws IOException {
		if (templateFile != null) {
			return new FileInputStream(getProcessFile());
		} else {
			String resource = "/com/rapidminer/resources/templates/"+getProcessResource();
			InputStream resourceAsStream = Template.class.getResourceAsStream(resource);
			if (resourceAsStream == null) {
				throw new IOException("Resource "+resource+" not found.");
			} else {
				return resourceAsStream;
			}
		}
	}


	public File getProcessFile() {
		return new File(templateFile.getParent(), getProcessResource());
	}

	private String getProcessResource() {
		return configResource;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Collection<OperatorParameterPair> getParameters() {
		return parameters;
	}

	public String toHTML() {
		return "<b>" + name + "</b><br />" + description;
	}

	public void save(File file) throws IOException {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(file));
			out.println(name);
			out.println(description);
			out.println(configResource);
			Iterator<OperatorParameterPair> i = parameters.iterator();
			while (i.hasNext()) {
				OperatorParameterPair pair = i.next();
				out.println(pair.toString());
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (out != null) {
				out.close();		
			}
		}
	}
}
