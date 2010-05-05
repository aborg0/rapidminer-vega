package com.rapidminer.repository.remote;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.rapid_i.repository.wsimport.ExecutionResponse;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;

/** This class can be used to access a RapidAnalytics installation from a remote machine.
 *  Currently, it can only be used to trigger the execution of jobs.
 * 
 * @author Simon Fischer
 *
 */
public class RapidAnalyticsCLTool {

	private Map<String,String> argsMap = new HashMap<String,String>();
	
	private RapidAnalyticsCLTool(String[] args) {
		extractArguments(args);	
	}

	private void extractArguments(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("--")) {
				args[i] = args[i].substring(2);
				if (args[i].equals("help")) {
					printUsage();
					System.exit(0);
				}
				String[] split = args[i].split("=", 2);
				if (split.length != 2) {
					System.err.println("Arguments must be of the form \"--key=value\".");
					System.exit(1);
				} else {
					argsMap.put(split[0], split[1]);
				}
			} else {
				System.err.println("Arguments must be of the form \"--key=value\".");
				System.exit(1);
			}
		}
	}	
	
	private void printUsage() {
		System.out.println(RapidAnalyticsCLTool.class.getName()+" [OPTIONS]");
		System.out.println("   --url=URL ");
		System.out.println("       Base URL of the RapidAnalytics installation.");
		System.out.println("   --user=USER ");
		System.out.println("       User name to use for logging in.");
		System.out.println("   --password=PASSWORD");
		System.out.println("       Password to use for logging in.");
		System.out.println("   --execute-process=/PATH/TO/PROCESS");
		System.out.println("       Process location to execute.");
	}

	private String getArgument(String argName, String defaultValue) {
		String value = argsMap.get(argName);
		if (value != null) {
			return value;
		} else {
			if (defaultValue != null) {
				System.err.println("No value specified for --"+argName+", using default ("+defaultValue+").");
			}
			return defaultValue;
		}
	}

	public void run() throws MalformedURLException, RepositoryException {
		
		String url = getArgument("url", "http://localhost:8080");
		String user = getArgument("user", "admin");
		String password = getArgument("password", "changeit");
		
		System.err.println("Using RapidAnalytics server at "+url+"...");
		RemoteRepository repository = new RemoteRepository(new URL(url), 
				"Temp", user, password.toCharArray(), true);
		RepositoryManager.getInstance(null).addRepository(repository);
		
		String executeProcess = getArgument("execute-process", null);
		if (executeProcess != null) {
			System.err.println("Scheduling process execution for process "+executeProcess);
			ExecutionResponse result = repository.getProcessService().executeProcessSimple(executeProcess, null);
			if (result.getStatus() != 0) {
				System.err.println("ERROR. Server responded with code "+result.getStatus()+": "+result.getErrorMessage());
				System.exit(result.getStatus());
			} else {
				System.out.println("Process scheduled for "+result.getFirstExecution());
			}
		}
	}
	
	public static void main(String[] args) throws MalformedURLException, RepositoryException {
		new RapidAnalyticsCLTool(args).run();
	}
}
