package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.Property;
import org.eclipse.wst.jsdt.debug.core.jsdi.Value;

public class ReloadProperty extends ReloadMirror implements Property {

	private final String name;
	private final ReloadStackFrame frame;
	private final String symbolToEvaluate;

	ReloadProperty(ReloadVirtualMachine vm, ReloadStackFrame frame, String symbolToEvaluate, String name) {
		super(vm);
		this.frame = frame;
		this.symbolToEvaluate = symbolToEvaluate;
		this.name = name;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Value value() {
		return frame.getValue(symbolToEvaluate);
	}

}
