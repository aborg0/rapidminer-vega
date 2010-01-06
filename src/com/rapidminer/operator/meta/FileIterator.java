/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
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
package com.rapidminer.operator.meta;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rapidminer.MacroHandler;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDirectory;
import com.rapidminer.parameter.ParameterTypeString;

/**
 * This operator iterates over the files in the specified directory (and 
 * subdirectories if the corresponding parameter is set to true). 
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class FileIterator extends OperatorChain {

	private final PortPairExtender inputExtender = new PortPairExtender("in", getInputPorts(), getSubprocess(0).getInnerSources());

	private static final String PARAMETER_DIRECTORY = "directory";

	private static final String PARAMETER_FILTER = "filter";

	private static final String PARAMETER_FILE_NAME_MACRO = "file_name_macro";
	private static final String PARAMETER_FILE_PATH_MACRO = "file_path_macro";
	private static final String PARAMETER_PARENT_PATH_MACRO = "parent_path_macro";

	private static final String PARAMETER_RECURSIVE = "recursive";

	private static final String PARAMETER_ITERATE_OVER_SUBDIRS = "iterate_over_subdirs";
	private static final String PARAMETER_ITERATE_OVER_FILES = "iterate_over_files";


	public FileIterator(OperatorDescription description) {
		super(description, "Nested Process");
		inputExtender.start();
		getTransformer().addRule(inputExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
	}

	@Override
	public void doWork() throws OperatorException {
		File dir = getParameterAsFile(PARAMETER_DIRECTORY);

		Pattern filter = null;
		if (isParameterSet(PARAMETER_FILTER)) {
			String filterString = getParameterAsString(PARAMETER_FILTER);
			filter = Pattern.compile(filterString);
		}

		String fileNameMacro = getParameterAsString(PARAMETER_FILE_NAME_MACRO);
		String pathNameMacro = getParameterAsString(PARAMETER_FILE_PATH_MACRO);
		String parentPathMacro = getParameterAsString(PARAMETER_PARENT_PATH_MACRO);

		boolean recursive = getParameterAsBoolean(PARAMETER_RECURSIVE);

		boolean iterateFiles = getParameterAsBoolean(PARAMETER_ITERATE_OVER_FILES);
		boolean iterateSubDirs = getParameterAsBoolean(PARAMETER_ITERATE_OVER_SUBDIRS);

		MacroHandler handler = getProcess().getMacroHandler();

		iterate(dir, handler, fileNameMacro, pathNameMacro, parentPathMacro, filter, iterateSubDirs, iterateFiles, recursive);
	}

	private void iterate(File dir, MacroHandler handler, String fileNameMacro, String pathNameMacro, String parentPathMacro, Pattern filter, boolean iterateSubDirs, boolean iterateFiles, boolean recursive) throws OperatorException {
		if (dir.isDirectory()) {
			for(File child : dir.listFiles()) {
				if (iterateSubDirs && child.isDirectory() || iterateFiles && child.isFile()) {
					boolean nameAccept = true;
					if (filter != null) {
						Matcher matcher = filter.matcher(child.getName());
						nameAccept = matcher.matches();
					}
					if (nameAccept) {
						handler.addMacro(fileNameMacro, child.getName());
						handler.addMacro(pathNameMacro, child.getAbsolutePath());
						handler.addMacro(parentPathMacro, child.getParent());
						inputExtender.passDataThrough();
						super.doWork();
					}
				}

				if (recursive && child.isDirectory()) {
					iterate(child, handler, fileNameMacro, pathNameMacro, parentPathMacro, filter, iterateSubDirs, iterateFiles, recursive);
				}
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeDirectory(PARAMETER_DIRECTORY, "Specifies the directory to iterate over.", false);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeString(PARAMETER_FILTER, "Specifies a regular expression which is used as filter for the file and directory names, e.g. 'a.*b' for all files starting with 'a' and ending with 'b'.", true, false));

		types.add(new ParameterTypeString(PARAMETER_FILE_NAME_MACRO, "Specifies the name of the macro, which delievers the current file name without path. Use %{macro_name} to use the file name in suboperators.", "file_name", false));
		types.add(new ParameterTypeString(PARAMETER_FILE_PATH_MACRO, "Specifies the name of the macro containing the absolute path and file name of the current file. Use %{macro_name} to address the file in suboperators.", "file_path", false));
		types.add(new ParameterTypeString(PARAMETER_PARENT_PATH_MACRO, "Specifies the name of the macro containing the absolute path of the current file's directory. Use %{macro_name} to address the file in suboperators.", "parent_path", false));

		types.add(new ParameterTypeBoolean(PARAMETER_RECURSIVE, "Indicates if the operator will also deliver the files / directories of subdirectories (resursively).", false, false));

		types.add(new ParameterTypeBoolean(PARAMETER_ITERATE_OVER_FILES, "If checked, the operator will iterate over files in the given directory and set their path and name macros.", true, false));
		types.add(new ParameterTypeBoolean(PARAMETER_ITERATE_OVER_SUBDIRS, "If checked, the operator will iterate over subdirectories in the given directory and set their path and name macros.", false, false));
		return types;
	}
}
