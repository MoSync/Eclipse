package com.mobilesorcery.sdk.html5.debug.jsdt.requests;

import org.eclipse.wst.jsdt.debug.core.jsdi.request.VMDeathRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadVMDeathRequest extends ReloadEventRequest implements VMDeathRequest {

	public ReloadVMDeathRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

}
