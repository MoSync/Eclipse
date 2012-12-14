package com.mobilesorcery.sdk.html5.debug.jsdt.requests;

import org.eclipse.wst.jsdt.debug.core.jsdi.request.ScriptLoadRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadScriptLoadRequest extends ReloadEventRequest implements ScriptLoadRequest {

	public ReloadScriptLoadRequest(ReloadVirtualMachine vm) {
		super(vm);
	}

}
