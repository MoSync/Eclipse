package com.mobilesorcery.sdk.html5.debug.jsdt.requests;

import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ExceptionRequest;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadExceptionRequest extends ReloadEventRequest implements ExceptionRequest {

	private ThreadReference thread;

	public ReloadExceptionRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

	@Override
	public void addThreadFilter(ThreadReference thread) {
		this.thread = thread;
	}
	
	public void setEnabled(boolean enabled) {
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			String enabledStr = enabled ? "Enabled" : "Disabled";
			CoreMoSyncPlugin.trace(enabledStr + " exception breakpoints");
		}
		vm.setBreakOnException(enabled);
		super.setEnabled(enabled);
	}

}
