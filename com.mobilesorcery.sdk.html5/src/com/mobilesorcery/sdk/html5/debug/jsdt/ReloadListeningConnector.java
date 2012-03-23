package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.connect.ListeningConnector;

public class ReloadListeningConnector implements ListeningConnector {

	private static ReloadListeningConnector DEFAULT = null;

	public ReloadListeningConnector() {
		if (DEFAULT == null) {
			DEFAULT = this;
		}
	}

	@Override
	public Map defaultArguments() {
		HashMap<String, String> args = new HashMap<String, String>();
		//args.put("port", "8511");
		return args;
	}

	@Override
	public String description() {
		return "On-device debug support for MoSync Projects";
	}

	@Override
	public String name() {
		return "Reload Listening Connector";
	}

	@Override
	public String id() {
		return "reload.listening.connector";
	}

	@Override
	public VirtualMachine accept(Map arguments) throws IOException {
		int port = 8511;
		Object portStr = arguments.get("port");
		if (portStr != null) {
			try {
				port = Integer.parseInt((String) arguments.get("port"));
			} catch (Exception e) {
				throw new IOException(MessageFormat.format("Invalid port number: {0}", port));
			}
		}

		try {
			return new ReloadVirtualMachine(port);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public static ReloadListeningConnector getDefault() {
		if (DEFAULT == null) {
			new ReloadListeningConnector();
		}
		return DEFAULT;
	}
}
