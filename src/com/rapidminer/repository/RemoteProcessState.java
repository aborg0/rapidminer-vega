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
	PENDING(false), RUNNING(false), COMPLETED(true), FAILED(true), STOPPED(true);

	private boolean terminated;
	private RemoteProcessState(boolean terminated) {
		this.terminated = terminated;
	}
	public boolean isTerminated() {
		return terminated;
	}
}