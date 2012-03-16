package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.connect.Connector;
import org.eclipse.wst.jsdt.debug.core.jsdi.connect.ListeningConnector;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

public class ReloadListeningConnector implements ListeningConnector {

	public ReloadListeningConnector() {
		// TODO Auto-generated constructor stub
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
		try {
			port = Integer.parseInt((String) arguments.get("port"));
		} catch (Exception e) {
			//
		}
		try {
			return new ReloadVirtualMachine(port);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
