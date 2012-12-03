package com.mobilesorcery.sdk.html5.debug.jsdt.requests;

import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.StepRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadThreadReference;

public class ReloadStepRequest extends ReloadEventRequest implements StepRequest {

	public static final int NO_STEPPING = -1;

	private final ReloadThreadReference thread;
	private final int step;

	public ReloadStepRequest(ReloadVirtualMachine vm, ThreadReference thread, int step) {
		super(vm);
		this.thread = (ReloadThreadReference) thread;
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

	@Override
	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		if (enabled) {
			thread.setStepType(step);
		} else {
			thread.setStepType(NO_STEPPING);
		}
		this.enabled = enabled;
	}
}
