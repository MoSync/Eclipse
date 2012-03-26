package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.request.VMDeathRequest;

public class ReloadVMDeathRequest extends ReloadEventRequest implements VMDeathRequest {

	ReloadVMDeathRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

}
