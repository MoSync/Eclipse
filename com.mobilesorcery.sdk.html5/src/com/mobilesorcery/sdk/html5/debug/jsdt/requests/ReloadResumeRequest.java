package com.mobilesorcery.sdk.html5.debug.jsdt.requests;

import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ResumeRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;


public class ReloadResumeRequest extends ReloadEventRequest implements ResumeRequest {

	private final ThreadReference thread;

	public ReloadResumeRequest(ReloadVirtualMachine vm, ThreadReference thread) {
		super(vm);
		this.thread = thread;
	}

	@Override
	public ThreadReference thread() {
		return thread;
	}

}
