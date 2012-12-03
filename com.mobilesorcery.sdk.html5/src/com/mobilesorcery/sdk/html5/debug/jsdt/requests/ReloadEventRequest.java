package com.mobilesorcery.sdk.html5.debug.jsdt.requests;

import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadMirror;


public class ReloadEventRequest extends ReloadMirror implements EventRequest {

	protected boolean enabled = false;
	private boolean deleted = false;

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

	public void delete() {
		this.deleted = true;
		setEnabled(false);
	}

	public boolean isDeleted() {
		return deleted;
	}
}
