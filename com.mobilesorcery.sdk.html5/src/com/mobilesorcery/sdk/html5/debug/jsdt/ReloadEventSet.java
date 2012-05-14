package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.HashSet;

import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.EventSet;
import org.eclipse.wst.jsdt.debug.internal.rhino.jsdi.ThreadReferenceImpl;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadEventSet extends HashSet implements EventSet {

	private final ReloadVirtualMachine vm;
	private ReloadThreadReference thread;

	public ReloadEventSet(ReloadVirtualMachine vm) {
		this.vm = vm;
	}

	@Override
	public VirtualMachine virtualMachine() {
		return vm;
	}

	@Override
	public void resume() {
		if (thread == null) {
			vm.resume();
		}
		else {
			thread.resume();
		}
	}

	@Override
	public boolean suspended() {
		return true;
	}

	public void setThread(ReloadThreadReference thread) {
		this.thread = thread;
	}

}
