package com.mobilesorcery.sdk.html5.debug.jsdt.events;

import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.ThreadEnterEvent;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadThreadEnterEvent extends ReloadLocatableEvent implements ThreadEnterEvent {

	public ReloadThreadEnterEvent(ReloadVirtualMachine vm, ThreadReference thread,
			Location location, EventRequest request) {
		super(vm, thread, location, request);
	}

}
