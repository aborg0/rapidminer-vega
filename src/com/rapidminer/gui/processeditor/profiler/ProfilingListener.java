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
package com.rapidminer.gui.processeditor.profiler;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Logger;

import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.processeditor.profiler.data.ProfilerData;
import com.rapidminer.gui.processeditor.profiler.data.ProfilerDataManager;
import com.rapidminer.operator.Operator;


/**
 * Listener which is registered as a Process Editor in the MainFrame.
 * 
 * @author Marco Boeck
 */
public class ProfilingListener extends Observable implements ProcessListener, ProcessEditor {
	
	/** map storing the time in ms it took an operator to finish execution (System.currentTimeMillis()) */
	private Map<String, Long> operatorExecSystemTimeMap;
	
	/** map storing the time in ns it took an operator to finish execution (getThreadCpuTime()) */
	private Map<String, Long> operatorExecCPUTimeMap;
	
	/** the currently active process */
	private Process process;
	
	/** will gather data if true; otherwise will do nothing */
	private boolean profilingEnabled;
	
	/** will automatically merge data if true; otherwise will do nothing */
	private boolean autoMergeEnabled;
	
	/** the list of currently selected operators */
	private List<Operator> selectedOperatorList;
	
	private static final Logger LOGGER = Logger.getLogger(ProfilingListener.class.getName());
	
	
	/**
	 * Standard constructor.
	 */
	public ProfilingListener() {
		operatorExecSystemTimeMap = new HashMap<String, Long>();
		operatorExecCPUTimeMap = new HashMap<String, Long>();
		profilingEnabled = false;
		autoMergeEnabled = false;
	}
	
	@Override
	public void processEnded(Process process) {
		setChanged();
		notifyObservers(true);
	}
	
	/**
	 * Returns a list with all currently selected operators.
	 * @return the list of all currently selected operators
	 */
	protected List<Operator> getSelectedOperatorList() {
		return selectedOperatorList;
	}
	
	/**
	 * Returns the current process.
	 * @return the process to which the listener is attached
	 */
	protected Process getCurrentProcess() {
		return process;
	}

	@Override
	public void processFinishedOperator(Process process, Operator op) {
		if (process != this.process) {
			LOGGER.warning("Process executed did not match currently monitored process!");
		}
		long execTimeReal = Math.abs(operatorExecSystemTimeMap.get(op.getName()) - System.currentTimeMillis());
		long execTimeCPU = Math.abs(operatorExecCPUTimeMap.get(op.getName()) - ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId()));
		ProfilerData data;
		if (!ProfilerDataManager.getInstance().isOperatorDataAvailable(op.getName())) {
			data = ProfilerDataManager.getInstance().addOperatorData(
					process, op.getName(), new ProfilerData());
		} else {
			data = ProfilerDataManager.getInstance().getOperatorData(op.getName());
		}
		data.incrementRunCount();
		data.setLastRunTimeReal(execTimeReal);
		data.setLastRunTimeCpu(execTimeCPU);
	}

	@Override
	public void processStartedOperator(Process process, Operator op) {
		operatorExecSystemTimeMap.put(op.getName(), System.currentTimeMillis());
		operatorExecCPUTimeMap.put(op.getName(), ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId()));
	}

	@Override
	public void processStarts(Process process) {
		// currently not needed
	}

	@Override
	public void processChanged(Process process) {
		if (ProfilingListener.this.process != null) {
			ProfilingListener.this.process.getRootOperator().removeProcessListener(ProfilingListener.this);
		}
		ProfilingListener.this.process = process;
		ProfilerDataManager.getInstance().setProcess(process);
		// don't register if profiling is disabled
		if (!profilingEnabled) {
			return;
		}
		if (ProfilingListener.this.process != null) {
			ProfilingListener.this.process.getRootOperator().addProcessListener(ProfilingListener.this);
		}
	}

	@Override
	public void processUpdated(Process process) {
		// currently not needed
	}

	@Override
	public void setSelection(List<Operator> selection) {
		selectedOperatorList = new ArrayList<Operator>(selection);
		setChanged();
		notifyObservers();
	}
	
	/**
	 * Disables profiling.
	 */
	public void disableProfiling() {
		profilingEnabled = false;
		if (ProfilingListener.this.process != null) {
			ProfilingListener.this.process.getRootOperator().removeProcessListener(ProfilingListener.this);
		}
	}
	
	/**
	 * Enables profiling.
	 */
	public void enableProfiling() {
		profilingEnabled = true;
		if (ProfilingListener.this.process != null) {
			ProfilingListener.this.process.getRootOperator().addProcessListener(ProfilingListener.this);
		}
	}
	
	/**
	 * Toggles profiling.
	 */
	public void toggleProfiling() {
		if (profilingEnabled) {
			disableProfiling();
		} else {
			enableProfiling();
		}
	}
	
	/**
	 * Toggles auto merge.
	 */
	public void toggleAutoMerge() {
		autoMergeEnabled = !autoMergeEnabled;
	}
	
	/**
	 * Returns true if auto merge has been enabled; false otherwise.
	 * @return true if auto merge has been enabled; false otherwise
	 */
	public boolean isAutoMergeEnabled() {
		return autoMergeEnabled;
	}

}
