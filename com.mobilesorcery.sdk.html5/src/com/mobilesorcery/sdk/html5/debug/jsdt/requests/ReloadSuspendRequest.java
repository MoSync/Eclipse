package com.mobilesorcery.sdk.html5.debug.jsdt.requests;

import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.SuspendRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadSuspendRequest extends ReloadEventRequest implements SuspendRequest {

	private final ThreadReference thread;

	public ReloadSuspendRequest(ReloadVirtualMachine vm, ThreadReference thread) {
		super(vm);
		this.thread = thread;
	}

	@Override
	public ThreadReference thread() {
		return thread;
	}

}
