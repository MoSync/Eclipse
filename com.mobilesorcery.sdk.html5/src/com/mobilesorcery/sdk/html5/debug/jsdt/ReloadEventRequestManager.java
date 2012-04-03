package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.BreakpointRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.DebuggerStatementRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequestManager;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ExceptionRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ResumeRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ScriptLoadRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.StepRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.SuspendRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ThreadEnterRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.ThreadExitRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.VMDeathRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.VMDisconnectRequest;


public class ReloadEventRequestManager implements EventRequestManager {

	private final ReloadVirtualMachine vm;
	private final ArrayList<EventRequest> requests = new ArrayList<EventRequest>();

	public ReloadEventRequestManager(ReloadVirtualMachine vm) {
		this.vm = vm;
	}

	@Override
	public BreakpointRequest createBreakpointRequest(Location location) {
		return addRequest(new ReloadBreakpointRequest(vm, location));
	}

	private <T extends EventRequest> T addRequest(T request) {
		requests.add(request);
		return request;
	}

	private List filterRequests(Class requestType) {
		ArrayList<EventRequest> result = new ArrayList<EventRequest>();
		for (EventRequest request : requests) {
			if (requestType.isAssignableFrom(request.getClass())) {
				result.add(request);
			}
		}
		return result;
	}

	@Override
	public List breakpointRequests() {
		return filterRequests(BreakpointRequest.class);
	}

	@Override
	public DebuggerStatementRequest createDebuggerStatementRequest() {
		return addRequest(new ReloadDebuggerStatementRequest(vm));
	}

	@Override
	public List debuggerStatementRequests() {
		return filterRequests(DebuggerStatementRequest.class);
	}

	@Override
	public ExceptionRequest createExceptionRequest() {
		return addRequest(new ReloadExceptionRequest(vm));
	}

	@Override
	public List exceptionRequests() {
		return filterRequests(ExceptionRequest.class);
	}

	@Override
	public ScriptLoadRequest createScriptLoadRequest() {
		return addRequest(new ReloadScriptLoadRequest(vm));
	}

	@Override
	public List scriptLoadRequests() {
		return filterRequests(ScriptLoadRequest.class);
	}

	@Override
	public StepRequest createStepRequest(ThreadReference thread, int step) {
		return addRequest(new ReloadStepRequest(vm, thread, step));
	}

	@Override
	public List stepRequests() {
		return filterRequests(StepRequest.class);
	}

	@Override
	public SuspendRequest createSuspendRequest(ThreadReference thread) {
		return addRequest(new ReloadSuspendRequest(vm, thread));
	}

	@Override
	public List suspendRequests() {
		return filterRequests(SuspendRequest.class);
	}

	@Override
	public ResumeRequest createResumeRequest(ThreadReference thread) {
		return addRequest(new ReloadResumeRequest(vm, thread));
	}

	@Override
	public List resumeRequests() {
		return filterRequests(ResumeRequest.class);
	}

	@Override
	public ThreadEnterRequest createThreadEnterRequest() {
		return addRequest(new ReloadThreadEnterRequest(vm));
	}

	@Override
	public List threadEnterRequests() {
		return filterRequests(ThreadEnterRequest.class);
	}

	@Override
	public ThreadExitRequest createThreadExitRequest() {
		return addRequest(new ReloadThreadExitRequest(vm));
	}

	@Override
	public List threadExitRequests() {
		return filterRequests(ThreadExitRequest.class);
	}

	@Override
	public void deleteEventRequest(EventRequest eventRequest) {
		this.requests.remove(eventRequest);
	}

	@Override
	public void deleteEventRequest(List eventRequests) {
		this.requests.removeAll(eventRequests);
	}

	@Override
	public VMDeathRequest createVMDeathRequest() {
		return addRequest(new ReloadVMDeathRequest(vm));
	}

	@Override
	public List vmDeathRequests() {
		return filterRequests(VMDeathRequest.class);
	}

	@Override
	public VMDisconnectRequest createVMDisconnectRequest() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List vmDisconnectRequests() {
		// TODO Auto-generated method stub
		return null;
	}

}
