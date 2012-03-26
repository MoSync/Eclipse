package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.StepRequest;

public class ReloadStepRequest extends ReloadEventRequest implements StepRequest {

	private ReloadThreadReference thread;
	private final int step;

	ReloadStepRequest(ReloadVirtualMachine vm, ThreadReference thread, int step) {
		super(vm);
		this.step = step;
	}

	@Override
	public int step() {
		return step;
	}

	@Override
	public ThreadReference thread() {
		return thread;
	}

}
