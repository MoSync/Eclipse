package com.mobilesorcery.sdk.html5.debug.jsdt;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadArrayProperty extends ReloadProperty {

	ReloadArrayProperty(ReloadVirtualMachine vm, ReloadStackFrame frame,
			ReloadProperty parent, String ixPropertyName) {
		super(vm, frame, parent, ixPropertyName);
	}
	
	public String getSymbolToEvaluate() {
		return parent.getSymbolToEvaluate() + "[" + name + "]";
	}

}
