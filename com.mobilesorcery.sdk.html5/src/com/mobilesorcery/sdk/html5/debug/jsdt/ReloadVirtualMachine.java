package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.jsdi.BooleanValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.NullValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.NumberValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.ScriptReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.StringValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.UndefinedValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.Value;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.EventQueue;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.ThreadExitEvent;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequest;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequestManager;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.LocalVariableScope;
import com.mobilesorcery.sdk.html5.live.ILiveServerListener;
import com.mobilesorcery.sdk.html5.live.LiveServer;

public class ReloadVirtualMachine implements VirtualMachine, ILiveServerListener {

	private final LiveServer server;
	private final List threads = new ArrayList<ThreadReference>();
	private final ReloadEventRequestManager requestMgr;
	private final ReloadEventQueue eventQueue;
	private final NullValue nullValue;
	private final ReloadUndefinedValue undefValue;
	private int currentSessionId = LiveServer.NO_SESSION;
	private IProject project;
	private final ReloadThreadReference mainThread;

	public ReloadVirtualMachine(int port) throws Exception {
		// TODO: PORT
		server = Html5Plugin.getDefault().getReloadServer();
		// JUST ONE MAIN THREAD
		mainThread = new ReloadThreadReference(this);
		threads.add(mainThread);
		requestMgr = new ReloadEventRequestManager(this);
		eventQueue = new ReloadEventQueue(this, requestMgr);
		nullValue = new ReloadNullValue(this);
		undefValue = new ReloadUndefinedValue(this);

		server.addListener(this);
		server.startServer(this);
		server.registerVM(this);
	}

	public void setCurrentSessionId(int sessionId) {
		this.currentSessionId = sessionId;
	}

	@Override
	public void resume() {
		server.resume(currentSessionId);
	}

	public void step(int stepType) {
		server.step(currentSessionId, stepType);
	}

	@Override
	public void suspend() {
		// NOT IMPL
	}

	public void reset(int newSessionId) {
		if (currentSessionId != LiveServer.NO_SESSION) {
			server.reset(currentSessionId);
			List exitRequests = requestMgr.threadExitRequests();
			for (Object exitRequest : exitRequests) {
				eventQueue.received(ReloadEventQueue.CUSTOM_EVENT, new ReloadThreadExitEvent(this, mainThread, null, (EventRequest) exitRequest));
			}
		}
		setCurrentSessionId(newSessionId);
	}

	@Override
	public void terminate() {
		try {
			server.removeListener(this);
			server.stopServer(this);
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	@Override
	public String name() {
		return "Reload";
	}

	@Override
	public String description() {
		return "TODO";
	}

	@Override
	public String version() {
		return MoSyncTool.getDefault().getVersionInfo(MoSyncTool.BINARY_VERSION);
	}

	@Override
	public List allThreads() {
		return threads;
	}

	@Override
	public List allScripts() {
		IBreakpoint[] bps = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(JavaScriptDebugModel.MODEL_ID);
		HashSet<IFile> scriptPaths = new HashSet<IFile>();
		for (IBreakpoint bp : bps) {
			if (bp instanceof IJavaScriptLineBreakpoint) {
				IJavaScriptLineBreakpoint lineBp = (IJavaScriptLineBreakpoint) bp;
				IResource resource = lineBp.getMarker().getResource();
				if (resource.getType() == IResource.FILE) {
					scriptPaths.add((IFile) resource);
				}
			}
		}
		List<SimpleScriptReference> refs = new ArrayList<SimpleScriptReference>();
		for (IFile scriptPath : scriptPaths) {
			SimpleScriptReference ref = new SimpleScriptReference(this, scriptPath);
			refs.add(ref);
		}
		return refs;
	}

	@Override
	public void dispose() {
		terminate();
	}

	@Override
	public UndefinedValue mirrorOfUndefined() {
		return undefValue;
	}

	@Override
	public NullValue mirrorOfNull() {
		return nullValue;
	}

	@Override
	public BooleanValue mirrorOf(boolean bool) {
		return new ReloadBooleanValue(this, bool);
	}

	@Override
	public NumberValue mirrorOf(Number number) {
		return new ReloadNumberValue(this, number);
	}

	@Override
	public StringValue mirrorOf(String string) {
		return new ReloadStringValue(this, string);
	}

	@Override
	public EventRequestManager eventRequestManager() {
		return requestMgr;
	}

	@Override
	public EventQueue eventQueue() {
		return eventQueue;
	}

	public String evaluate(String expression) throws InterruptedException, TimeoutException {
		return server.evaluate(currentSessionId, expression);
	}

	@Override
	public void received(String command, JSONObject json) {
		// TODO!!! Session id - now all will suspend.

		// MAIN THREAD
		ReloadThreadReference thread = (ReloadThreadReference) threads.get(0);
		if (thread.isSuspended()) {
			return;
		}
		thread.suspend(true);
		JSONArray array = (JSONArray) json.get("stack");
		ReloadStackFrame[] frames = new ReloadStackFrame[array.size()];
		for (int i = 0; i < array.size(); i++) {
			ReloadStackFrame frame = new ReloadStackFrame(this, json, i);
			frames[i] = frame;
		}
		if (frames.length == 0) {
			frames = new ReloadStackFrame[1];
			frames[0] = new ReloadStackFrame(this, json, -1);
		}
		thread.setFrames(frames);
		suspend();
		eventQueue.received(command, json);
	}

	public LocalVariableScope getLocalVariableScope(Location location) {
		// TODO: Faster?
		ScriptReference ref = location.scriptReference();
		if (ref instanceof SimpleScriptReference) {
			IFile file = ((SimpleScriptReference) ref).getFile();
			JSODDSupport jsoddSupport = Html5Plugin.getDefault().getJSODDSupport(file.getProject());
			LocalVariableScope scope = jsoddSupport.getScope(file, location.lineNumber());
			if (scope != null) {
				return scope;
			}
		}
		return null;
	}

	public int getCurrentSessionId() {
		return currentSessionId;
	}

	@Override
	public String toString() {
		return "JavaScript On-Device Debug VM, session id #" + getCurrentSessionId();
	}

	public void setCurrentLocation(String location) {
		mainThread.setCurrentLocation(location);
	}

}
