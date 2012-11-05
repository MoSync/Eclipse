package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ScriptReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.BreakpointRequest;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

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

	@Override
	public synchronized void setEnabled(boolean enabled) {
		if (enabled != isEnabled()) {
			super.setEnabled(enabled);

			ScriptReference scriptRef = location.scriptReference();
			if (scriptRef instanceof SimpleScriptReference) {
				IFile file = ((SimpleScriptReference) scriptRef).getFile();

				// NOTE: This breakpoint will be modified to reflect properties
				// of the breakpoint
				// that are difficult to reliable get at this point (condition
				// enablement etc).

				JavaScriptBreakpointDesc bp = new JavaScriptBreakpointDesc(
						file, location.lineNumber(), condition,
						JavaScriptBreakpointDesc.SUSPEND_ON_TRUE, hitcount);
				Html5Plugin.getDefault().getReloadServer()
						.setLineBreakpoint(enabled, bp);
			}
		}
	}
}
