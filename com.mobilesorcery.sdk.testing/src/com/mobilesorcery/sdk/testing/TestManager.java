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
