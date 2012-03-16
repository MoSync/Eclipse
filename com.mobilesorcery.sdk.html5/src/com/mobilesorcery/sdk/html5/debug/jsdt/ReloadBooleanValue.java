package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.BooleanValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;


public class ReloadBooleanValue extends ReloadValue implements BooleanValue {

	private final boolean bool;

	protected ReloadBooleanValue(ReloadVirtualMachine vm, boolean bool) {
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachine virtualMachine() {
		// TODO Auto-generated method stub
		return null;
	}

}
