package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.NullValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadNullValue extends ReloadValue implements NullValue {

	public ReloadNullValue(ReloadVirtualMachine vm) {
		super(vm);
	}

	@Override
	public String valueString() {
		return "Null";
	}
}
