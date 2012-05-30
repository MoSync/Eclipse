package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.Value;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public abstract class ReloadValue implements Value {

	public static final Object UNDEFINED = new Object() {
		public String toString() {
			return "undefined";
		}
	};
	
	private final ReloadVirtualMachine vm;

	protected ReloadValue(ReloadVirtualMachine vm) {
		this.vm = vm;
	}

	@Override
	public VirtualMachine virtualMachine() {
		return vm;
	}

}
