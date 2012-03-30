package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequest;


public class ReloadEventRequest extends ReloadMirror implements EventRequest {

	protected boolean enabled;

	ReloadEventRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
