package com.mobilesorcery.sdk.html5.debug.jsdt.events;

import org.eclipse.wst.jsdt.debug.core.jsdi.ScriptReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.ScriptLoadEvent;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ScriptLoadRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.debug.jsdt.SimpleScriptReference;

public class ReloadScriptEvent extends ReloadLocatableEvent implements ScriptLoadEvent {

	private final SimpleScriptReference script;

	public ReloadScriptEvent(ReloadVirtualMachine vm, ThreadReference thread, SimpleScriptReference script, ScriptLoadRequest request) {
		super(vm, thread, null, request);
		this.script = script;
	}

	@Override
	public ScriptReference script() {
		return script;
	}

}
