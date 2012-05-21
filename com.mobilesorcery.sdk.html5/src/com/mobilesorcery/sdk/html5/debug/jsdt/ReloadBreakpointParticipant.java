package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptBreakpoint;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptBreakpointParticipant;
import org.eclipse.wst.jsdt.debug.core.jsdi.ScriptReference;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptThread;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;

public class ReloadBreakpointParticipant implements
		IJavaScriptBreakpointParticipant {

	public ReloadBreakpointParticipant() {

	}

	@Override
	public int breakpointHit(IJavaScriptThread thread,
			IJavaScriptBreakpoint breakpoint) {
		if (isApplicable(breakpoint.getMarker().getResource())) {
			return SUSPEND;
		}
		return DONT_CARE;
	}

	@Override
	public int scriptLoaded(IJavaScriptThread thread, ScriptReference script,
			IJavaScriptBreakpoint breakpoint) {
		if (script instanceof SimpleScriptReference) {
			if (isApplicable(((SimpleScriptReference) script).getFile())) {
				return SUSPEND;
			}
		}
		return DONT_CARE;
	}

	private boolean isApplicable(IResource resource) {
		if (resource == null) {
			return false;
		}

		IProject project = resource.getProject();
		return Html5Plugin.getDefault().hasHTML5Support(MoSyncProject.create(project));
	}

}
