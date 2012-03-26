package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.StackFrame;
import org.eclipse.wst.jsdt.debug.core.jsdi.Value;
import org.eclipse.wst.jsdt.debug.core.jsdi.Variable;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

public class ReloadVariable extends ReloadProperty implements Variable {

	public ReloadVariable(ReloadVirtualMachine vm,
			ReloadStackFrame frame, String symbolToEvaluate, String name) {
		super(vm, frame, symbolToEvaluate, name);
	}

	@Override
	public boolean isArgument() {
		return false;
	}

	@Override
	public boolean isVisible(StackFrame frame) {
		// TODO: ???
		return true;
	}

}
