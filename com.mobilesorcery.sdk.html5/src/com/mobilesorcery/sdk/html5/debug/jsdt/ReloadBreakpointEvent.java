package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.BreakpointEvent;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequest;

public class ReloadBreakpointEvent extends ReloadLocatableEvent implements BreakpointEvent {

	ReloadBreakpointEvent(ReloadVirtualMachine vm, ThreadReference thread,
			Location location, EventRequest request) {
		super(vm, thread, location, request);
	}

}
