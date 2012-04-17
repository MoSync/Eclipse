package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.core.resources.IResource;

public class JavaScriptBreakpointDesc {

	private final IResource resource;
	private final int lineNumber;
	private final String condition;
	private final int hitCount;

	public JavaScriptBreakpointDesc(IResource resource, int lineNumber,
			String condition, int hitCount) {
		this.resource = resource;
		this.lineNumber = lineNumber;
		this.condition = condition;
		this.hitCount = hitCount;
	}

	public IResource getResource() {
		return resource;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String getCondition() {
		return condition;
	}

	public int getHitCount() {
		return hitCount;
	}


}
