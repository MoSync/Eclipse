package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.BooleanValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;


public class ReloadBooleanValue extends ReloadValue implements BooleanValue {

	private final boolean bool;

	public ReloadBooleanValue(ReloadVirtualMachine vm, boolean bool) {
		super(vm);
		this.bool = bool;
	}

	@Override
	public int intValue() {
		return bool ? 1 : 0;
	}

	@Override
	public double doubleValue() {
		return intValue();
	}

	@Override
	public boolean booleanValue() {
		return bool;
	}

	@Override
	public String stringValue() {
		return Boolean.toString(bool);
	}

	@Override
	public String valueString() {
		return stringValue();
	}

	@Override
	public VirtualMachine virtualMachine() {
		return super.virtualMachine();
	}

}
