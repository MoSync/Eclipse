package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.net.URI;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.EventQueue;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.EventSet;
import org.json.simple.JSONObject;

import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.live.LiveServer;

public class ReloadEventQueue implements EventQueue {

	private final ReloadVirtualMachine vm;
	private final ReloadEventRequestManager requests;

	private final LinkedBlockingQueue<Pair<String, Object>> internalQueue = new LinkedBlockingQueue<Pair<String, Object>>();

	public ReloadEventQueue(ReloadVirtualMachine vm,
			ReloadEventRequestManager requests) {
		this.vm = vm;
		this.requests = requests;
	}

	@Override
	public EventSet remove() {
		return remove(-1);
	}

	@Override
	public EventSet remove(int timeout) {
		try {
			// TODO: EVENTS SHOULD BE PURE JSON!?
			Pair<String, Object> event = timeout > 0 ? internalQueue.poll(timeout, TimeUnit.MILLISECONDS) : internalQueue.take();
			return handleEvent(event.first, (JSONObject) event.second);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private EventSet handleEvent(String commandName, JSONObject command) {
		ReloadEventSet eventSet = new ReloadEventSet(vm);
		List bpRequests = requests.breakpointRequests();

		if ("report-breakpoint".equals(commandName)) {
			// Breakpoint!
			ReloadThreadReference thread = (ReloadThreadReference) vm.allThreads().get(0);
			Long fileId = (Long) command.get("file");
			Long line = (Long) command.get("line");
			if (fileId != null && line != null) {
				IJavaScriptLineBreakpoint bp = LiveServer.findBreakPoint(fileId, line);
				for (Object bpRequestObj : bpRequests) {
					ReloadBreakpointRequest bpRequest = (ReloadBreakpointRequest) bpRequestObj;
					Location location = bpRequest.location();
					if (sameLocation(location, bp)) {
						eventSet.add(new ReloadBreakpointEvent(vm, thread,
								location, bpRequest));
					}
				}
			}
			if (!eventSet.isEmpty()) {
				thread.suspend(true);
			}
		}
		return eventSet;
	}

	private boolean sameLocation(Location location, IJavaScriptLineBreakpoint bp) {
		if (bp == null) {
			return false;
		}
		try {
			if (bp.getLineNumber() == location.lineNumber()) {
				URI uri1 = location.scriptReference().sourceURI();
				URI uri2 = new Path(bp.getScriptPath()).toFile().toURI();
				return Util.equals(uri1, uri2);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void received(String command) {

	}

	public void received(String command, Object data) {
		internalQueue.offer(new Pair<String, Object>(command, data));
	}
}
