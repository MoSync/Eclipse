package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.core.resources.IResource;

public class JavaScriptBreakpointDesc {

	public static final String SUSPEND_ON_CHANGE = "change";
	public static final String SUSPEND_ON_TRUE = "true";

	private final IResource resource;
	private final int lineNumber;
	private String condition;
	private final int hitCount;
	private String conditionSuspend;

	public JavaScriptBreakpointDesc(IResource resource, int lineNumber,
			String condition, String conditionSuspend, int hitCount) {
		this.resource = resource;
		this.lineNumber = lineNumber;
		this.condition = condition;
		this.conditionSuspend = conditionSuspend;
		this.hitCount = hitCount;
	}
	
	private JavaScriptBreakpointDesc(JavaScriptBreakpointDesc prototype) {
		this.resource = prototype.resource;
		this.lineNumber = prototype.lineNumber;
		this.condition = prototype.condition;
		this.conditionSuspend = prototype.conditionSuspend;
		this.hitCount = prototype.hitCount;
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

	public JavaScriptBreakpointDesc setCondition(String condition) {
		JavaScriptBreakpointDesc copy = new JavaScriptBreakpointDesc(this);
		copy.condition = condition;
		return copy;
	}
	
	public JavaScriptBreakpointDesc setConditionSuspend(String conditionSuspend) {
		JavaScriptBreakpointDesc copy = new JavaScriptBreakpointDesc(this);
		copy.conditionSuspend = conditionSuspend;
		return copy;
	}
	
	public String toString() {
		String hitCountStr = hitCount > 0 ? " Hit count: " + Integer.toString(hitCount) : "";
		String suspendStr = SUSPEND_ON_TRUE.equals(conditionSuspend) ? " (suspend on true)" : " (suspend on change)";
		String conditionStr = condition != null ? " Condition: " + condition : "" + suspendStr;
		return resource.getFullPath().toPortableString() + ": " + lineNumber + hitCountStr + conditionStr;
	}

}
