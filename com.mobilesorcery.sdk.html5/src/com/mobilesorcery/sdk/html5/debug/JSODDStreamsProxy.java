package com.mobilesorcery.sdk.html5.debug;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDStreamsProxy.JSODDStreamMonitor;

public class JSODDStreamsProxy implements IStreamsProxy {

	public class JSODDStreamMonitor implements IStreamMonitor, ILineHandler {

		private CopyOnWriteArrayList<IStreamListener> listeners = new CopyOnWriteArrayList<IStreamListener>();
		
		@Override
		public void addListener(IStreamListener listener) {
			if (listeners.isEmpty()) {
				Html5Plugin.getDefault().getReloadServer().addConsoleListener(this);
			}
			listeners.add(listener);
		}

		@Override
		public String getContents() {
			return "";
		}

		@Override
		public void removeListener(IStreamListener listener) {
			listeners.remove(listener);
			if (listeners.isEmpty()) {
				Html5Plugin.getDefault().getReloadServer().removeConsoleListener(this);
			}
		}

		@Override
		public void start(Process process) {
			// Ignore.
		}

		@Override
		public void newLine(String line) {
			for (IStreamListener listener : listeners) {
				listener.streamAppended(line + "\n", this);
			}
		}

		@Override
		public void stop(IOException e) {
			// Ignore.
		}

	}

	private JSODDStreamMonitor outMonitor;

	public JSODDStreamsProxy() {
		
	}
	
	@Override
	public IStreamMonitor getErrorStreamMonitor() {
		return null;
	}

	@Override
	public synchronized IStreamMonitor getOutputStreamMonitor() {
		if (outMonitor == null) {
			outMonitor = new JSODDStreamMonitor();
		}
		return outMonitor;
	}

	@Override
	public void write(String input) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
