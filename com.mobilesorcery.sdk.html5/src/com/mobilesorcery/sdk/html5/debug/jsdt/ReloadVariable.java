package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.StackFrame;
import org.eclipse.wst.jsdt.debug.core.jsdi.Value;
import org.eclipse.wst.jsdt.debug.core.jsdi.Variable;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadVariable extends ReloadProperty implements Variable {

	public ReloadVariable(ReloadVirtualMachine vm,
			ReloadStackFrame frame, String name) {
		super(vm, frame, null, name);
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
