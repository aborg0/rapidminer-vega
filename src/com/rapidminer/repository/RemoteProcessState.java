/**
 * 
 */
package com.rapidminer.repository;

/**
 * 
 * @author Simon Fischer
 *
 */
public enum RemoteProcessState {	
	PENDING(false, "clock_run.png"), 
	RUNNING(false, "media_play.png"), 
	COMPLETED(true, "check.png"), 
	FAILED(true, "error.png"), 
	STOPPED(true, "media_stop.png"); 
	//STOP_REQUESTED(false, "media_stop.png");

	private String iconName;
	private boolean terminated;
	private RemoteProcessState(boolean terminated, String iconName) {
		this.terminated = terminated;
		this.iconName = iconName;
	}
	public boolean isTerminated() {
		return terminated;
	}
	
	public String getIconName() {
		return iconName;
	}
}