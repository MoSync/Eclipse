package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.EventQueue;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.EventSet;
import org.json.simple.JSONObject;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.live.JSODDServer;

public class ReloadEventQueue implements EventQueue {

	public final static String CUSTOM_EVENT = "custom-event";

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
			Pair<String, Object> event = timeout > 0 ? internalQueue.poll(
					timeout, TimeUnit.MILLISECONDS) : internalQueue.take();
			if (event.first == null) {
				internalQueue.clear();
			} else {
				return handleEvent(event.first, event.second);
			}
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
		return null;
	}

	private EventSet handleEvent(String commandName, Object commandObj) {
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Event {0}", commandName);
		}
		ReloadEventSet eventSet = new ReloadEventSet(vm);
		ReloadVirtualMachine targetVM = extractVM(commandObj);
		if (targetVM == this.vm) {
			List bpRequests = requests.breakpointRequests();
			List stepRequests = requests.stepRequests();
			List suspendRequests = requests.suspendRequests();
			List exceptionRequests = requests.exceptionRequests();
			List scriptLoadRequests = requests.scriptLoadRequests();
			List debuggerStatementRequests = requests.debuggerStatementRequests();

			if ("report-breakpoint".equals(commandName)) {
				// Breakpoint!
				JSONObject command = (JSONObject) commandObj;
				ReloadThreadReference thread = (ReloadThreadReference) vm
						.allThreads().get(0);
				String fileName = (String) command.get("file");
				Long line = (Long) command.get("line");
				boolean isSuspendedByDebugger = command.containsKey("suspended")
						&& (Boolean) command.get("suspended");
				boolean isStepping = isSuspendedByDebugger || command.containsKey("step")
						&& (Boolean) command.get("step");
				boolean isException = "exception".equals(command.get("type"));
				boolean isScriptLoad = "load".equals(command.get("type"));
				boolean isDebuggerStatement = "debugger".equals(command.get("type"));

				if (fileName != null && line != null) {
					IFile file = ResourcesPlugin.getWorkspace().getRoot()
							.getFile(new Path(fileName));
					Location location = new SimpleLocation(vm, file,
							line.intValue());
					IProject project = file.getProject();
					IJavaScriptLineBreakpoint bp = JSODDSupport.findBreakPoint(
							new Path(fileName), line.intValue());
					for (Object bpRequestObj : bpRequests) {
						ReloadBreakpointRequest bpRequest = (ReloadBreakpointRequest) bpRequestObj;
						Location bpLocation = bpRequest.location();
						if (!isStepping && !isException && sameLocation(bpLocation, bp)) {
							eventSet.add(new ReloadBreakpointEvent(vm, thread,
									bpLocation, bpRequest));
						}
					}
					for (Object stepRequestObj : stepRequests) {
						ReloadStepRequest stepRequest = (ReloadStepRequest) stepRequestObj;
						eventSet.add(new ReloadStepEvent(vm, thread, location,
								stepRequest));
					}
					if (isSuspendedByDebugger) {
						for (Object suspendRequestObj : suspendRequests) {
							ReloadSuspendRequest suspendRequest = (ReloadSuspendRequest) suspendRequestObj;
							eventSet.add(new ReloadSuspendEvent(vm, thread,
									location, suspendRequest));
						}
					} else if (isException) {
						for (Object exceptionRequestObj : exceptionRequests) {
							ReloadExceptionRequest exceptionRequest = (ReloadExceptionRequest) exceptionRequestObj;
							String message = (String) command.get("message");
							eventSet.add(new ReloadExceptionEvent(vm, thread, location, message, exceptionRequest));
						}
					} else if (isScriptLoad) {
						for (Object scriptLoadRequestObj : scriptLoadRequests) {
							// For some reason, the project should not be in this path!?
							ReloadScriptLoadRequest scriptLoadRequest = (ReloadScriptLoadRequest) scriptLoadRequestObj;
							SimpleScriptReference script = new SimpleScriptReference(vm, file.getProjectRelativePath());
							eventSet.add(new ReloadScriptEvent(targetVM, thread, script, scriptLoadRequest));
						}
					} else if (isDebuggerStatement) {
						for (Object debuggerStatementRequestObj : debuggerStatementRequests) {
							// For some reason, the project should not be in this path!?
							ReloadDebuggerStatementRequest debuggerStatementRequest = (ReloadDebuggerStatementRequest) debuggerStatementRequestObj;
							eventSet.add(new ReloadDebuggerStatementEvent(targetVM, thread, location, debuggerStatementRequest));
						}

					}
				}
				if (!eventSet.isEmpty()) {
					thread.markSuspended(!isStepping && !isException && !isSuspendedByDebugger);
				}
			} else if (CUSTOM_EVENT.equals(commandName)) {
				ReloadEvent event = (ReloadEvent) commandObj;
				eventSet.add(event);
			}
		}
		return eventSet;
	}

	private ReloadVirtualMachine extractVM(Object commandObj) {
		if (commandObj instanceof JSONObject) {
			Integer sessionId = JSODDServer
					.extractSessionId((JSONObject) commandObj);
			if (sessionId != null) {
				return Html5Plugin.getDefault().getReloadServer()
						.getVM(sessionId);
			}
		} else if (commandObj instanceof ReloadEvent) {
			return (ReloadVirtualMachine) ((ReloadEvent) commandObj)
					.virtualMachine();
		}
		return null;
	}

	private boolean sameLocation(Location location, IJavaScriptLineBreakpoint bp) {
		if (bp == null || !(location instanceof SimpleLocation)) {
			return false;
		}
		try {
			if (bp.getLineNumber() == location.lineNumber()) {
				IPath path1 = ((SimpleScriptReference) location
						.scriptReference()).getFile().getFullPath();
				IPath path2 = new Path(bp.getScriptPath());
				return Util.equals(path1, path2);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void received(String command, Object data) {
		internalQueue.offer(new Pair<String, Object>(command, data));
	}

	public void close() {
		internalQueue.offer(new Pair<String, Object>(null, null));
		//internalQueue.clear();
	}

}
