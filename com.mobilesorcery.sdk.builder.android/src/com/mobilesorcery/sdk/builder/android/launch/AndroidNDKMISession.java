package com.mobilesorcery.sdk.builder.android.launch;

import org.eclipse.cdt.debug.mi.core.IMITTY;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIProcess;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.debug.mi.core.command.MITargetSelect;

public class AndroidNDKMISession extends MISession {

	private String serialNumber;

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	
	public AndroidNDKMISession(MIProcess process, IMITTY tty, int type,
			CommandFactory commandFactory, int commandTimeout)
			throws MIException {
		super(process, tty, type, commandFactory, commandTimeout);
	}
	
	protected void initialize() throws MIException {
		super.initialize();
		MITargetSelect selectTarget = getCommandFactory().createMITargetSelect(new String[] { "remote", ":5039" });
	    postCommand(selectTarget);
		selectTarget.getMIInfo();
	}

}
