package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.StringValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadStringValue extends ReloadValue implements StringValue {

	private final String string;

	public ReloadStringValue(ReloadVirtualMachine vm, String string) {
		super(vm);
		this.string = string;
	}

	@Override
	public String valueString() {
		return string;
	}

	@Override
	public String value() {
		return string;
	}

}
