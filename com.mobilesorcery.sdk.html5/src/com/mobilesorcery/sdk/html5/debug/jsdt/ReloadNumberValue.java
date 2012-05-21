package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.NumberValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadNumberValue extends ReloadValue implements NumberValue {

	private final Number number;

	public ReloadNumberValue(ReloadVirtualMachine vm,
			Number number) {
		super(vm);
		this.number = number;
	}

	@Override
	public int intValue() {
		return number.intValue();
	}

	@Override
	public double doubleValue() {
		return number.doubleValue();
	}

	@Override
	public boolean booleanValue() {
		return intValue() > 0;
	}

	@Override
	public String stringValue() {
		return JavaScriptDebugModel.numberToString(number);
	}

	@Override
	public String valueString() {
		return stringValue();
	}

	@Override
	public Number value() {
		return number;
	}

	@Override
	public boolean isNaN() {
		return number == null;
	}

}
