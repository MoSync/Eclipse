package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.Value;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public abstract class ReloadValue implements Value {

	private final ReloadVirtualMachine vm;

	protected ReloadValue(ReloadVirtualMachine vm) {
		this.vm = vm;
	}

	@Override
	public VirtualMachine virtualMachine() {
		return vm;
	}

}
