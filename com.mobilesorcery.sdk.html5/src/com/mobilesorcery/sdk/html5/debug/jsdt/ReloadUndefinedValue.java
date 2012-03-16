package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.UndefinedValue;

public class ReloadUndefinedValue extends ReloadValue implements UndefinedValue {

	public ReloadUndefinedValue(ReloadVirtualMachine vm) {
		super(vm);
	}

	@Override
	public String valueString() {
		return "Undefined";
	}

}
