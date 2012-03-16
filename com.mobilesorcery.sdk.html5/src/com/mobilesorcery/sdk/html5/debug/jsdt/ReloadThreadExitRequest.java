package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ThreadExitRequest;

public class ReloadThreadExitRequest extends ReloadEventRequest implements ThreadExitRequest {

	private ThreadReference thread;

	public ReloadThreadExitRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

	@Override
	public void addThreadFilter(ThreadReference thread) {
		this.thread = thread;
	}

}
