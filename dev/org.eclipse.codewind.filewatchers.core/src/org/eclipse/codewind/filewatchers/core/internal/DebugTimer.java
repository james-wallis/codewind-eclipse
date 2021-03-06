/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.filewatchers.core.internal;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.Filewatcher;

/**
 * Every X minutes, this timer thread will run and output the internal state of
 * each of the internal components of the filewatcher. This should run
 * infrequently, as it can be fairly verbose (eg every 30 minutes).
 * 
 * The goal of this timer is to identify bugs/performance issues that might only
 * be detectable when the workbench is open for long periods of time (memory
 * leaks, resources we aren't closing, etc).
 */
public class DebugTimer {

	private final FWLogger log = FWLogger.getInstance();

	private Timer timer_synch_lock;

	private final Object lock = new Object();

	private final long TIME_TO_WAIT_IN_MSECS = TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES);

	private final Filewatcher parent;

	public DebugTimer(Filewatcher parent) {

		this.parent = parent;
		schedule();
	}

	public void schedule() {
		synchronized (lock) {
			timer_synch_lock = new Timer(DebugTimer.class.getName(), true);
			timer_synch_lock.schedule(new DebugTimerTask(), TIME_TO_WAIT_IN_MSECS);
		}
	}

	/**
	 * Timer task to query the state of our parent, print the result, then schedule
	 * the next run if not shutdown.
	 */
	private class DebugTimerTask extends TimerTask {

		public DebugTimerTask() {
		}

		@Override
		public void run() {
			Optional<String> result = null;
			try {

				result = parent.generateDebugString();
				if (!result.isPresent()) {
					// FW is disposed if we get an empty optional
					return;
				}

				for (String str : result.get().split("\n")) {
					log.logInfo("[status] " + str);
				}

			} finally {

				// Reschedule if an exception was thrown (null), or the result was not-empty
				if (result == null || result.isPresent()) {
					schedule();
				}
			}
		}

	}
}
