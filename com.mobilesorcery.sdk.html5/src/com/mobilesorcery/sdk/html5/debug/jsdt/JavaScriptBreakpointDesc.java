package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.core.resources.IResource;

public class JavaScriptBreakpointDesc {

	public static final String SUSPEND_ON_CHANGE = "change";
	public static final String SUSPEND_ON_TRUE = "true";

	private final IResource resource;
	private final int lineNumber;
	private final String condition;
	private final int hitCount;
	private final String conditionSuspend;

	public JavaScriptBreakpointDesc(IResource resource, int lineNumber,
			String condition, String conditionSuspend, int hitCount) {
		this.resource = resource;
		this.lineNumber = lineNumber;
		this.condition = condition;
		this.conditionSuspend = conditionSuspend;
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

	public String getConditionSuspend() {
		return conditionSuspend;
	}


}
