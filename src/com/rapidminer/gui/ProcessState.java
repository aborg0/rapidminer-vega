/*
 * 
 */
package com.rapidminer.gui;

import com.rapidminer.Process;

/**
 * This interface gives access to the process related methods.
 * 
 * @author Gábor Bakos
 */
public interface ProcessState {

	public void validateProcess(boolean force);

	public int getProcessState();

	public Process getProcess();

	/** Creates a new process. */
	public void newProcess();

	/** Runs or resumes the current process. */
	public void runProcess();

	/**
	 * Can be used to stop the currently running process. Please note that the
	 * ProcessThread will still be running in the background until the current
	 * operator is finished.
	 */
	public void stopProcess();

	public void pauseProcess();

	/**
	 * Sets a new process and registers the MainFrame listener. Please note that
	 * this method only invoke {@link #processChanged()} if the parameter
	 * newProcess is true.
	 * 
	 * Note: {@link MainFrame#processChanged()} is deprecated.
	 */
	public void setProcess(Process process, boolean newProcess);

	/** Returns true if the process has changed since the last save. */
	public boolean isChanged();

	public void undo();

	public void redo();

	public void setOpenedProcess(Process process, boolean showInfo,
			final String sourceName);

	public void saveAsTemplate();

	public void fireProcessUpdated();

	public void processHasBeenSaved();

}