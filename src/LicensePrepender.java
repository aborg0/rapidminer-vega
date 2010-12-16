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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

import com.rapidminer.tools.Tools;


/**
 * Prepends the license text before the package statement. Replaces all existing
 * comments before. Ignores files without package statement.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class LicensePrepender {

	private char[] license;
	private Pattern pattern;

	private char[] readFile(File file, String from) throws IOException {
		StringBuffer contents = new StringBuffer((int) file.length());

		BufferedReader in = new BufferedReader(new FileReader(file));
		try {
			String line = null;
			while (((line = in.readLine()) != null) && (!line.startsWith("package")));
			if (line == null) {
				System.err.println("'package' not found in file '" + file + "'.");
				return null;
			}

			do {
				contents.append(line);
				contents.append(Tools.getLineSeparator());
			} while ((line = in.readLine()) != null);
		}
		finally {
			/* Close the stream even if we return early. */
			in.close();
		}
		return contents.toString().toCharArray();
	}

	private void readLicense(File file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;
		StringBuffer licenseText = new StringBuffer(); 
		while ((line = in.readLine()) != null) {
			licenseText.append(line + Tools.getLineSeparator());
		}
		in.close();
		this.license = licenseText.toString().toCharArray();
	}

	private void prependLicense(File file) throws IOException {
		System.out.print(file + "...");

		char[] fileContents = readFile(file, "package");
		if (fileContents == null)
			return;

		Writer out = new FileWriter(file);
		out.write(license);
		out.write(fileContents);
		out.close();
		System.out.println("ok");
	}

	private void recurse(File currentDirectory, String currentPackage) {
		if (currentDirectory.isDirectory()) {
			File[] files = currentDirectory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					recurse(files[i], currentPackage + files[i].getName() + ".");
				} else {
					if (files[i].getName().endsWith(".java") && pattern.matcher(currentPackage).matches()) {
						try {
							prependLicense(files[i]);
						} catch (IOException e) {
							System.err.println("failed: " + e.getClass().getName() + ": " + e.getMessage());
						}
					}
				}
			}
		} else {
			System.err.println("Can only work on directories.");
		}
	}

	public static void main(String[] argv) throws Exception {

		LicensePrepender lp = new LicensePrepender();

		if ((argv.length < 2) || (argv[0].equals("-help"))) {
			System.out.println("Usage: java " + lp.getClass().getName() + " licensefile directory [pattern]");
			System.exit(1);
		}
		
		lp.readLicense(new File(argv[0]));
		System.out.println("Prepending license:");
		System.out.print(lp.license);

		if (argv.length >= 3) {
			lp.pattern = Pattern.compile(argv[2]);
		} else {
			lp.pattern = Pattern.compile(".*");
		}
		lp.recurse(new File(argv[1]), "");
	}
}
