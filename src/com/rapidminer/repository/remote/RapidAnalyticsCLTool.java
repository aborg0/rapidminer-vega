/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
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

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapid_i.repository.wsimport.ExecutionResponse;
import com.rapid_i.repository.wsimport.ProcessResponse;
import com.rapid_i.repository.wsimport.ProcessService;
import com.rapid_i.repository.wsimport.ProcessStackTrace;
import com.rapid_i.repository.wsimport.ProcessStackTraceElement;
import com.rapid_i.repository.wsimport.RepositoryService;
import com.rapidminer.repository.RemoteProcessState;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.tools.Tools;

/** This class can be used to access a RapidAnalytics installation from a remote machine.
 *  Currently, it can only be used to trigger the execution of jobs.
 * 
 * @author Simon Fischer
 *
 */
public class RapidAnalyticsCLTool {

	private static final String NULL = new String();
	
	private Map<String,String> argsMap = new HashMap<String,String>();
	private long delay = 1000;
	private boolean dumpStatus = false;
	private boolean watch = false;
	
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
				switch (split.length) {
				case 1:
					argsMap.put(split[0], NULL);
					break;
				case 2:
					argsMap.put(split[0], split[1]);
					break;
				default:
					// cannot happen
					System.err.println("Arguments must be of the form \"--key=value\".");
					System.exit(1);
					break;
				}				
			} else {
				System.err.println("Arguments must be of the form \"--key=value\".");
				System.exit(1);
			}
		}
	}	
	
	private void printUsage() {
		System.out.println(RapidAnalyticsCLTool.class.getName()+" [OPTIONS]");
		System.out.println("   --check-state");
		System.out.println("       Check state and exit with code 0 if server is up, state 1 if server does not respond or causes an error.");
		System.out.println("   --url=URL ");
		System.out.println("       Base URL of the RapidAnalytics installation.");
		System.out.println("   --user=USER ");
		System.out.println("       User name to use for logging in.");
		System.out.println("   --password=PASSWORD");
		System.out.println("       Password to use for logging in.");
		System.out.println("   --execute-process=/PATH/TO/PROCESS");
		System.out.println("       Process location to execute.");
		System.out.println("   --watch={true|false}");
		System.out.println("       Watch process until completed.");
		System.out.println("   --process-id=ID");
		System.out.println("       Manually specify process ID (for --watch). Unnecessary for --execute process.");
		System.out.println("   --delay=MILLIS");
		System.out.println("       Loop delay in milliseconds when --watch is enabled.");
	}

	private boolean isArgumentSet(String argName) {
		return argsMap.containsKey(argName);
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

	private int getArgumentInt(String argName, int defaultValue) {
		String value = argsMap.get(argName);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Parameter "+argName+" must be a number.");
			}
		} else {			
			return defaultValue;
		}
	}
	
	public void run() throws IllegalArgumentException, MalformedURLException, RepositoryException {
		String url = getArgument("url", "http://localhost:8080");
		String user = getArgument("user", "admin");
		String password = getArgument("password", "changeit");

		System.err.println("Using RapidAnalytics server at "+url+"...");
		RemoteRepository repository = new RemoteRepository(new URL(url), 
				"Temp", user, password.toCharArray(), true);
		RepositoryManager.getInstance(null).addRepository(repository);

		if (isArgumentSet("check-state")) {
			long startTime;
			try {
				final RepositoryService repoService = repository.getRepositoryService();
				startTime = System.currentTimeMillis();
				repoService.getFolderContents("/");
			} catch (Exception e) {
				System.err.println("Error checking state of RapidAnalytics server at "+url+": "+e);
				System.exit(1);
				return;
			}
			long responseTime = System.currentTimeMillis() - startTime;
			System.err.println("RapidAnalytics server at "+url+" is up. Response time was "+responseTime+" ms.");
			System.exit(0);
		}
		

		delay = getArgumentInt("delay", 1000);
		if ("true".equals(getArgument("watch", "false"))) {
			watch = true;
			dumpStatus = true;
		}
		
		int processId = -1;
		String executeProcess = getArgument("execute-process", null);
		if (executeProcess != null) {
			System.err.println("Scheduling process execution for process "+executeProcess);
			ExecutionResponse result = repository.getProcessService().executeProcessSimple(executeProcess, null, null);
			if (result.getStatus() != 0) {
				System.err.println("ERROR. Server responded with code "+result.getStatus()+": "+result.getErrorMessage());
				System.exit(result.getStatus());
			} else {
				System.out.println("Process scheduled for "+result.getFirstExecution());
				int jobId = result.getJobId();
				if (dumpStatus) {
					processId = getJobId(jobId, repository.getProcessService());
				} else {
					processId = -1;
				}
			}
		} else {
			processId = getArgumentInt("process-id", -1);
			if (processId != -1) {
				dumpStatus = true;
			}			
		}
			

		if (dumpStatus) {
			if (processId == -1) {
				throw new IllegalArgumentException("You must use --process-id or --execute-service if --watch=true.");
			}
			RemoteProcessState state;
			do {
				ProcessResponse pInfo = repository.getProcessService().getRunningProcessesInfo(processId);
				if (pInfo == null) {
					throw new IllegalArgumentException("Process with id "+processId + " does not exist.");
				}
				dump(pInfo, System.out);
				if (watch) {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {	}
				}
				state = RemoteProcessState.valueOf(pInfo.getState());
			} while (watch && !state.isTerminated());
			if (!state.isSuccessful()) {
				System.exit(1);				
			} else {
				System.exit(0);
			}
		}		
	}
	
	private int getJobId(int jobId, ProcessService processService) {
		while (true) {
			System.err.println("Waiting for server to assign process id to scheduled job id "+jobId+"...");
			List<Integer> result = processService.getProcessIdsForJobId(jobId);
			if ((result == null) || result.isEmpty()) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) { }
			} else {
				if (result.size() == 1) {
					final Integer id = result.get(0);
					System.err.println("Process id is "+id+".");					
					return id;
				} else {
					throw new RuntimeException("Server delivered non-unique process id: "+result);
				}
			}
		}		
	}

	private void dump(ProcessResponse pInfo, PrintStream out) {
		out.println("State of process " + pInfo.getProcessLocation()+ " (id="+pInfo.getId()+")");
		out.println("  Started:   "+pInfo.getStartTime());
		if (pInfo.getCompletionTime() != null) {
			out.println("  Completed: "+pInfo.getCompletionTime());
		}
		out.println("  State:     "+pInfo.getState());
		if (pInfo.getException() != null) {
			out.println("  Exception: "+pInfo.getException());
		}
		final ProcessStackTrace trace = pInfo.getTrace();
		if (trace != null) {
			out.println("  Trace:");
			for (ProcessStackTraceElement ste : trace.getElements()) {
				System.out.println("     "+ste.getOperatorName()+" ("+ste.getApplyCount()+", "+Tools.formatDuration(ste.getExecutionTime())+")");
			}
		}
	}

	public static void main(String[] args) {
		try {
			new RapidAnalyticsCLTool(args).run();
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (MalformedURLException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (RepositoryException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
}
