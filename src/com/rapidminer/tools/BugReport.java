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
package com.rapidminer.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.xmlrpc.client.XmlRpcClient;

import com.rapidminer.Process;


/**
 * A bug report can be send by the user. It should only be used in cases where
 * an exception does not occur due to a user error.
 * 
 * @author Simon Fischer, Ingo Mierswa, Marco Boeck
 */
public class BugReport {

	private static final int BUFFER_SIZE = 1024;
	
	
	private static void getProperties(String prefix, StringBuffer string) {
		string.append(prefix + " properties:" + Tools.getLineSeparator());
		Enumeration keys = System.getProperties().propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.startsWith(prefix)) {
				string.append("  " + key + "\t= " + System.getProperty(key) + Tools.getLineSeparator());
			}
		}
	}

	public static String getProperties() {
		StringBuffer string = new StringBuffer();
		string.append("System properties:" + Tools.getLineSeparator());
		string.append("------------------" + Tools.getLineSeparator() + Tools.getLineSeparator());
		getProperties("os", string);
		getProperties("java", string);
		getProperties("rapidminer", string);
		return string.toString();
	}

	public static String getStackTrace(Throwable throwable) {
		StringBuffer string = new StringBuffer();
		string.append("Stack trace:" + Tools.getLineSeparator());
		string.append("------------" + Tools.getLineSeparator() + Tools.getLineSeparator());
		while (throwable != null) {
			string.append("Exception:\t" + throwable.getClass().getName() + Tools.getLineSeparator());
			string.append("Message:\t" + throwable.getMessage() + Tools.getLineSeparator());
			string.append("Stack trace:"+ Tools.getLineSeparator());
			StackTraceElement[] ste = throwable.getStackTrace();
			for (int i = 0; i < ste.length; i++) {
				string.append("  " + ste[i] + Tools.getLineSeparator());
			}
			string.append(Tools.getLineSeparator());

			throwable = throwable.getCause();
			if (throwable != null) {
				string.append("");
				string.append("Cause:");
			}
		}
		return string.toString();
	}

	public static void createBugReport(File reportFile, Throwable exception, String userMessage, Process process, String logMessage, File[] attachments) throws IOException {
		ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(reportFile));
		zipOut.setComment("RapidMiner bug report - generated " + new Date());
		write("message.txt", "User message", userMessage, zipOut);
		write("_process.xml", "Process as in memory.", process.getRootOperator().getXML(false), zipOut);
		if (process.getProcessLocation() != null) {
			try {
				String contents = process.getProcessLocation().getRawXML();
				write(process.getProcessLocation().getShortName(), "Raw process file in repository.", contents, zipOut);
			} catch (Throwable t) {
				write(process.getProcessLocation().getShortName(), "Raw process file in repository.", "could not read: " + t, zipOut);
			}
		}
		write("_log.txt", "Log message", logMessage, zipOut);
		write("_properties.txt", "System properties, information about java version and operating system", getProperties(), zipOut);
		write("_exception.txt", "Exception stack trace", getStackTrace(exception), zipOut);

		for (int i = 0; i < attachments.length; i++)
			writeFile(attachments[i], zipOut);
		zipOut.close();
	}
	
	/**
	 * Creates the BugZilla bugreport.
	 * @param client the logged in BugZilla client
	 * @param exception the exception which was thrown by the bug
	 * @param userSummary summary of the bug
	 * @param userDescription description of the bug
	 * @param process the currently active process
	 * @param logMessage the RM log
	 * @param attachments optional attachements
	 * @param attachProcess if the process xml should be attached
	 * @param sendSystemProps if the system properties should be included
	 * @throws Exception
	 */
	public static void createBugZillaReport(XmlRpcClient client, Throwable exception, String userSummary, String userDescription, String component, String version, String severity, String platform, String os, Process process, String logMessage, File[] attachments, boolean attachProcess, boolean sendSystemProps) throws Exception {
		// create temp files with all the data we need
//		File processFile = File.createTempFile("_process", ".xml");
//		processFile.deleteOnExit();
//		writeFile(processFile, process.getRootOperator().getXML(false));
		String xmlProcess = "";
		
		if (process.getProcessLocation() != null) {
//			File rawProcessFile = File.createTempFile(process.getProcessLocation().getShortName(), ".xml");
//			rawProcessFile.deleteOnExit();
			try {
				xmlProcess = process.getProcessLocation().getRawXML();
//				writeFile(rawProcessFile, xmlProcess);
			} catch (Throwable t) {
				xmlProcess = "could not read: " + t;
//				writeFile(rawProcessFile, "could not read: " + t);
			}
		}
		
//		File logFile = File.createTempFile("_log", ".txt");
//		logFile.deleteOnExit();
//		writeFile(logFile, logMessage);
		
//		File stackTraceFile = File.createTempFile("_exception", ".txt");
//		stackTraceFile.deleteOnExit();
//		writeFile(stackTraceFile, getStackTrace(exception));
		
		//TODO: remove once BugZilla version is upgraded to 4.x, add attachement handling
		userDescription = userDescription + Tools.getLineSeparator() + Tools.getLineSeparator() + getStackTrace(exception);
		if (attachProcess) {
			userDescription = userDescription + 
			Tools.getLineSeparator() + Tools.getLineSeparator() +
			"Process:" + 
			Tools.getLineSeparator() + 
			"------------" + 
			Tools.getLineSeparator() + Tools.getLineSeparator() +
			xmlProcess;
		}
		if (sendSystemProps) {
			userDescription = userDescription + 
			Tools.getLineSeparator() + Tools.getLineSeparator() +
			"System Properties:" + 
			"------------" + 
			Tools.getLineSeparator() + Tools.getLineSeparator() +
			getProperties();
		}
		
		// call BugZilla via xml-rpc
		XmlRpcClient rpcClient = client;

        Map<String, String> bugMap = new HashMap<String, String>();
        //TODO: change to RapidMiner
//        bugMap.put("product", "RapidMiner");
        bugMap.put("product", "Test Product");
        bugMap.put("component", component);
        bugMap.put("summary", userSummary);
        bugMap.put("description", userDescription);
        //TODO: change to version
//        bugMap.put("version", version);
        bugMap.put("version", "1.0");
        bugMap.put("op_sys", os);
        bugMap.put("platform", platform);
        bugMap.put("severity", severity);
        bugMap.put("status", "NEW");

        //TODO: enable & remove syso
//        Map createResult = (Map)rpcClient.execute("Bug.create", new Object[]{ bugMap });
//        LogService.getRoot().fine("Bug " + createResult.get("id") + " submitted successfully.");
        System.out.println("BUG SUBMITTED:");
        for (String bug : bugMap.keySet()) {
        	System.out.println(bug + " : " + bugMap.get(bug));
        }
	}
	
	private static void writeFile(File file, String content) throws IOException {
		FileWriter writer = new FileWriter(file);
		writer.write(content);
		writer.close();
	}

	private static void writeFile(File file, ZipOutputStream out) throws IOException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			out.putNextEntry(new ZipEntry(file.getName()));
			byte[] buffer = new byte[BUFFER_SIZE];
			int read = -1;
			do {
				read = in.read(buffer);
				if (read > -1) {
					out.write(buffer, 0, read);
				}
			} while (read > -1);
			out.closeEntry();
		} catch (IOException e) {
			throw e;
		} finally {
			if (in != null)
				in.close();
		}
	}

	private static void write(String name, String comment, String string, ZipOutputStream out) throws IOException {
		ZipEntry entry = new ZipEntry(name);
		entry.setComment(comment);
		out.putNextEntry(entry);

		PrintStream print = new PrintStream(out);
		print.println(string);
		print.flush();

		out.closeEntry();
	}
}
