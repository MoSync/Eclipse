package com.mobilesorcery.sdk.html5.debug;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.html5.Html5Plugin;

public class JSODDStreamsProxy implements IStreamsProxy {

	public class JSODDStreamMonitor implements IStreamMonitor, ILineHandler {

		private final CopyOnWriteArrayList<IStreamListener> listeners = new CopyOnWriteArrayList<IStreamListener>();
		private final int type;

		public JSODDStreamMonitor(int type) {
			this.type = type;
		}

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
			String[] levelAndMessage = line.split("\\|", 2);
			String level = levelAndMessage.length > 1 ? levelAndMessage[0] : "";
			String message = levelAndMessage.length > 1 ? levelAndMessage[1] : levelAndMessage[0];
			if (levelMatches(level)) {
				for (IStreamListener listener : listeners) {
					listener.streamAppended(message + "\n", this);
				}
			}
		}

		private boolean levelMatches(String level) {
			if ("warn".equalsIgnoreCase(level) || "error".equalsIgnoreCase(level)) {
				return type == IProcessConsole.ERR;
			} else {
				return type == IProcessConsole.OUT;
			}
		}

		@Override
		public void stop(IOException e) {
			// Ignore.
		}

	}

	private JSODDStreamMonitor outMonitor;
	private JSODDStreamMonitor errMonitor;
	private final ReloadVirtualMachine vm;

	public JSODDStreamsProxy(ReloadVirtualMachine vm) {
		this.vm = vm;
	}

	@Override
	public IStreamMonitor getErrorStreamMonitor() {
		if (errMonitor == null) {
			errMonitor = new JSODDStreamMonitor(IProcessConsole.ERR);
		}
		return errMonitor;
	}

	@Override
	public synchronized IStreamMonitor getOutputStreamMonitor() {
		if (outMonitor == null) {
			outMonitor = new JSODDStreamMonitor(IProcessConsole.OUT);
		}
		return outMonitor;
	}

	@Override
	public void write(String input) throws IOException {
		try {
			int endIndex = input.charAt(input.length() - 1) == '\n' ? input.length() - 1 : input.length();
			input = input.substring(0, endIndex);
			if (input.length() > 0) {
				Object result = vm.evaluate(input);
				outMonitor.newLine("> " + result);
			}
		} catch (Exception e) {
			errMonitor.newLine(MessageFormat.format("warn|Could not evaluate {0}. Reason: {1}", input, e.getMessage()));
		}
	}

}
