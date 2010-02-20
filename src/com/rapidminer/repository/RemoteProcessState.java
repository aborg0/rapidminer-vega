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
