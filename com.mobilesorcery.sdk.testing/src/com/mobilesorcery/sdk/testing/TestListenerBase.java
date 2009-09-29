package com.mobilesorcery.sdk.testing;

import java.util.concurrent.CopyOnWriteArrayList;

public class TestListenerBase {

	private CopyOnWriteArrayList<ITestSessionListener> listeners = new CopyOnWriteArrayList<ITestSessionListener>();

	protected void notifyListeners(TestSessionEvent event) {
		for (ITestSessionListener listener : listeners) {
			listener.handleEvent(event);
		}
	}

	public void addSessionListener(ITestSessionListener listener) {
		listeners.add(listener);
	}

	public void removeSessionListener(ITestSessionListener listener) {
		listeners.remove(listener);
	}
}
