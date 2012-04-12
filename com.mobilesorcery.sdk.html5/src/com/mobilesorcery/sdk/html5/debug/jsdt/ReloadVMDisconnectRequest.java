package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.request.VMDisconnectRequest;

public class ReloadVMDisconnectRequest extends ReloadEventRequest implements VMDisconnectRequest {

	ReloadVMDisconnectRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

}
