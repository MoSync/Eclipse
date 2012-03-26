package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.LocatableEvent;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequest;

public class ReloadLocatableEvent extends ReloadEvent implements LocatableEvent {

	private final ThreadReference thread;
	private final Location location;

	ReloadLocatableEvent(ReloadVirtualMachine vm, ThreadReference thread, Location location, EventRequest request) {
		super(vm, request);
		this.thread = thread;
		this.location = location;
	}

	@Override
	public Location location() {
		return location;
	}

	@Override
	public ThreadReference thread() {
		return thread;
	}

}
