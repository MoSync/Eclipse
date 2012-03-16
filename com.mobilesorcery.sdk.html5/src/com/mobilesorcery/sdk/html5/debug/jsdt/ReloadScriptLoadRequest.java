package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.request.ScriptLoadRequest;

public class ReloadScriptLoadRequest extends ReloadEventRequest implements ScriptLoadRequest {

	ReloadScriptLoadRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

}
