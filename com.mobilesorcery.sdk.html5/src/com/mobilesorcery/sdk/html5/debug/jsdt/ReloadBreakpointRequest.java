package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ScriptReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.BreakpointRequest;

import com.mobilesorcery.sdk.html5.Html5Plugin;

public class ReloadBreakpointRequest extends ReloadEventRequest implements
		BreakpointRequest {

	private final Location location;
	private ThreadReference thread;
	private String condition;
	private int hitcount;

	ReloadBreakpointRequest(ReloadVirtualMachine vm, Location location) {
		super(vm);
		this.location = location;
	}

	@Override
	public Location location() {
		return location;
	}

	@Override
	public void addThreadFilter(ThreadReference thread) {
		this.thread = thread;
	}

	@Override
	public void addConditionFilter(String condition) {
		this.condition = condition;
	}

	@Override
	public void addHitCountFilter(int hitcount) {
		this.hitcount = hitcount;
	}

	public synchronized void setEnabled(boolean enabled) {
		if (enabled != isEnabled()) {
			super.setEnabled(enabled);
			
			ScriptReference scriptRef = location.scriptReference();
			if (scriptRef instanceof SimpleScriptReference) {
				IFile file = ((SimpleScriptReference) scriptRef).getFile();
				Html5Plugin.getDefault().getReloadServer().setLineBreakpoint(enabled, file, location.lineNumber());	
			}
		}
	}
}
