package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.Mirror;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadMirror implements Mirror {

	protected final ReloadVirtualMachine vm;

	ReloadMirror(ReloadVirtualMachine vm) {
		this.vm = vm;
	}

	@Override
	public VirtualMachine virtualMachine() {
		return vm;
	}

}
