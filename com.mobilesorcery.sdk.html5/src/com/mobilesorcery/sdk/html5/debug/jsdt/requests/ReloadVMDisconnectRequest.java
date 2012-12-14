package com.mobilesorcery.sdk.html5.debug.jsdt.requests;

import org.eclipse.wst.jsdt.debug.core.jsdi.request.VMDisconnectRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadVMDisconnectRequest extends ReloadEventRequest implements VMDisconnectRequest {

	public ReloadVMDisconnectRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

}
