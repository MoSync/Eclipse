package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ThreadEnterRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadThreadEnterRequest extends ReloadEventRequest implements ThreadEnterRequest {

	private ThreadReference thread;

	ReloadThreadEnterRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

	@Override
	public void addThreadFilter(ThreadReference thread) {
		this.thread = thread;
	}

}
