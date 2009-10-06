/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.testing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestManager implements ITestSessionListener {

	private static TestManager manager = new TestManager();
	
	private ArrayList<ITestSession> sessions = new ArrayList<ITestSession>();
	private CopyOnWriteArrayList<ITestSessionListener> listeners = new CopyOnWriteArrayList<ITestSessionListener>();
	
	public void addTestSession(ITestSession session) {
		sessions.add(session);
		session.addSessionListener(this);
	}
	
	public void removeTestSession(ITestSession session) {
		sessions.remove(session);
		session.removeSessionListener(this);
	}
	
	public void addSessionListener(ITestSessionListener listener) {
		listeners.add(listener);
	}
	
	public void removeSessionListener(ITestSessionListener listener) {
		listeners.remove(listener);
	}

	public static TestManager getInstance() {
		return manager;
	}

	public void handleEvent(TestSessionEvent event) {
		for (ITestSessionListener listener : listeners) {
			listener.handleEvent(event);
		}
	}
	
	public ITestSession getLastSession() {
		return sessions.isEmpty() ? null : sessions.get(sessions.size() - 1);
	}

}
