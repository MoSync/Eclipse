package com.mobilesorcery.sdk.html5.debug.jsdt.events;

import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.Event;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadEvent implements Event {

	private final ReloadVirtualMachine vm;
	private final EventRequest request;

	ReloadEvent(ReloadVirtualMachine vm, EventRequest request) {
		this.vm = vm;
		this.request = request;
	}

	@Override
	public EventRequest request() {
		return request;
	}

	@Override
	public VirtualMachine virtualMachine() {
		return vm;
	}
	
	public String toString() {
		return getClass().getSimpleName() + " #" + vm.getId();
	}
}
