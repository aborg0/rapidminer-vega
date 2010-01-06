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
package com.rapidminer.gui.tools;

/** A queue of runnables in which only execution of the last is relevant.
 *  Older runnables become obsolete as soon as a new is inserted. 
 * 
 * @author Simon Fischer
 *
 */
public class UpdateQueue extends Thread {

	private Runnable pending;

	public UpdateQueue(String name) {
		super("UpdateQueue-"+name);
		setDaemon(true);
	}

	private Object lock = new Object();

	/** Queues runnable for execution. Will be executed as soon as the current
	 *  runnable has terminated. If there is no current executable, will be
	 *  executed immediately (in the thread created by this instance). 
	 *  If this method is called again before the current runnable is executed,
	 *  runnable will be discarded in favor of the new. */
	public void execute(Runnable runnable) {
		synchronized (lock) {
			pending = runnable;
			lock.notifyAll();
		}
	}

	@Override
	public void run() {
		while (true) {
			final Runnable target;
			synchronized (lock) {				
				target = pending;
				pending = null;				
			}
			if (target != null) {
				target.run();
			}
			synchronized (lock) {
				if (pending == null) {
					try {
						lock.wait();
					} catch (InterruptedException e) { }
				}
			}
		}
	}		
}
