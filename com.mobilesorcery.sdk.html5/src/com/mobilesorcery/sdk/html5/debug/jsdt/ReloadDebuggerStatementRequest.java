package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.DebuggerStatementRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadDebuggerStatementRequest extends ReloadEventRequest implements DebuggerStatementRequest {

	private ThreadReference thread;

	ReloadDebuggerStatementRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

	@Override
	public void addThreadFilter(ThreadReference thread) {
		this.thread = thread;
	}

}
