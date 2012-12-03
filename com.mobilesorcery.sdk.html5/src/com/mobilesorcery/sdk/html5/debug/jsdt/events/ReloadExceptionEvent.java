package com.mobilesorcery.sdk.html5.debug.jsdt.events;

import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.ExceptionEvent;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ExceptionRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadExceptionEvent extends ReloadLocatableEvent implements ExceptionEvent {

	private final String message;

	public ReloadExceptionEvent(ReloadVirtualMachine vm, ThreadReference thread, Location location, String message, ExceptionRequest request) {
		super(vm, thread, location, request);
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
