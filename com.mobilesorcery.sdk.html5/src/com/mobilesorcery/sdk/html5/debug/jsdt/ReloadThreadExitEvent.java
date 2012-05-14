package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.ThreadExitEvent;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadThreadExitEvent extends ReloadLocatableEvent implements ThreadExitEvent {

	public ReloadThreadExitEvent(ReloadVirtualMachine vm, ThreadReference thread,
			Location location, EventRequest request) {
		super(vm, thread, null, request);
	}

}
