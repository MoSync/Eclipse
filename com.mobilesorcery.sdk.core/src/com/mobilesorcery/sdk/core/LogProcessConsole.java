package com.mobilesorcery.sdk.core;

public class LogProcessConsole extends AbstractProcessConsole {

	public LogProcessConsole(String consoleName) {
	}

	protected void writeLine(int type, String line) {
		System.out.println(line);
		//CoreMoSyncPlugin.getDefault().getLog().log(new Status(IStatus.OK, CoreMoSyncPlugin.PLUGIN_ID, line));
	}


}
