package com.mobilesorcery.sdk.html5.debug;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.wst.jsdt.debug.core.jsdi.BooleanValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.NullValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.NumberValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.ScriptReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.StringValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.UndefinedValue;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.event.EventQueue;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.EventRequestManager;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptDebugTarget;
import org.eclipse.wst.jsdt.debug.internal.core.model.JavaScriptThread;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.hotreplace.FileRedefinable;
import com.mobilesorcery.sdk.html5.debug.hotreplace.FunctionRedefinable;
import com.mobilesorcery.sdk.html5.debug.hotreplace.ProjectRedefinable;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadBooleanValue;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadDropToFrame;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadEventQueue;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadEventRequestManager;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadNullValue;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadNumberValue;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadStackFrame;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadStringValue;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadThreadReference;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadUndefinedValue;
import com.mobilesorcery.sdk.html5.debug.jsdt.SimpleScriptReference;
import com.mobilesorcery.sdk.html5.live.ILiveServerCommandListener;
import com.mobilesorcery.sdk.html5.live.JSODDServer;

public class ReloadVirtualMachine implements VirtualMachine,
		ILiveServerCommandListener {

	private final JSODDServer server;
	private HashMap<String, ReloadThreadReference> threads = new HashMap<String, ReloadThreadReference>();
	private final ReloadEventRequestManager requestMgr;
	private ReloadEventQueue eventQueue;
	private final NullValue nullValue;
	private final ReloadUndefinedValue undefValue;
	private IProject project;
	private boolean isTerminated = false;
	private ProjectRedefinable baseline;
	private ILaunch launch;
	private IJavaScriptDebugTarget debugTarget;
	private String remoteAddr;
	private HashMap<Class, Boolean> redefineSupport = new HashMap<Class, Boolean>();
	private boolean breakOnException;
	private ReloadThreadReference mainThread;
	private int vmId;

	public ReloadVirtualMachine(int port) throws Exception {
		// TODO: PORT
		server = Html5Plugin.getDefault().getReloadServer();

		// By default we support function redefines.
		redefineSupport.put(FunctionRedefinable.class, Boolean.TRUE);
		redefineSupport.put(FileRedefinable.class, Html5Plugin.getDefault().shouldFetchRemotely());

		requestMgr = new ReloadEventRequestManager(this);
		eventQueue = new ReloadEventQueue(this, requestMgr);

		nullValue = new ReloadNullValue(this);
		undefValue = new ReloadUndefinedValue(this);

		server.addListener(this);
		server.startServer(this);
		server.registerVM(this);
	}

	private void resetEventQueue() {
		if (eventQueue != null) {
			eventQueue.close();
			List exitRequests = requestMgr.threadExitRequests();
			for (Object exitRequest : exitRequests) {
				// Don't reactivate this yet!
				// eventQueue.received(ReloadEventQueue.CUSTOM_EVENT, new
				// ReloadThreadExitEvent(this, mainThread, null, (EventRequest)
				// exitRequest));
			}
		}
	}

	@Override
	public void resume() {
		for (ReloadThreadReference thread : threads.values()) {
			thread.resume();
		}
	}


	@Override
	public void suspend() {
		for (ReloadThreadReference thread : threads.values()) {
			thread.suspend();
		}
	}

	public void reset(int vmId, MoSyncProject project, String remoteAddr) {
		this.vmId = vmId;
		for (ReloadThreadReference thread : threads.values()) {
			server.reset(thread.getSessionId());
		}
		resetEventQueue();
		this.project = project.getWrappedProject();
		this.remoteAddr = remoteAddr;
	}
	
	public int getId() {
		return vmId;
	}

	@Override
	public synchronized void terminate() {
		try {
			for (ReloadThreadReference thread : threads.values()) {
				int sessionId = thread.getSessionId();
				server.terminate(thread.getSessionId(), sessionId == getMainThreadId());	
			}
			server.removeListener(this);
			server.stopServer(this);
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		} finally {
			eventQueue.close();
			isTerminated = true;
		}
	}

	@Override
	public String name() {
		return project == null ? "On-Device Debug" :  project.getName();
	}

	@Override
	public String description() {
		return "TODO";
	}

	@Override
	public String version() {
		String versionInfo = MoSyncTool.getDefault()
				.getVersionInfo(MoSyncTool.BINARY_VERSION);
		if (versionInfo.toLowerCase().startsWith("version")) {
			versionInfo = versionInfo.substring("version".length()).trim();
		}
		return versionInfo;
	}

	@Override
	public List allThreads() {
		return new ArrayList(threads.values());
	}

	@Override
	public List allScripts() {
		ArrayList<ScriptReference> result = new ArrayList<ScriptReference>();
		// Before the project has been initialized, we just send all the scripts
		// in the workspace...
		ArrayList<IProject> projects = new ArrayList<IProject>();
		if (project != null) {
			projects.add(project);
		} else {
			projects.addAll(Arrays.asList(ResourcesPlugin.getWorkspace()
					.getRoot().getProjects()));
		}

		for (IProject project : projects) {
			JSODDSupport jsoddSupport = Html5Plugin.getDefault()
					.getJSODDSupport(project);
			if (jsoddSupport != null) {
				Set<IPath> allFiles = jsoddSupport.getAllFiles();
				for (IPath file : allFiles) {
					SimpleScriptReference ref = new SimpleScriptReference(this,
							file);
					result.add(ref);
				}
			}
		}
		return result;
	}

	public IProject getProject() {
		return project;
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

	/**
	 * Clears and resets all breakpoints on target.
	 */
	public void refreshBreakpoints() {
		// TODO: Clear per file instead.
		for (ReloadThreadReference thread : threads.values()) {
			server.refreshBreakpoints(thread.getSessionId());
		}
	}

	/**
	 * Issues a reload request to the client.
	 * 
	 * @param resourcePath
	 *            The resource to reload. A {@code null} value will cause a full
	 *            reload of the app.
	 * @param resourcePath
	 *            The resource to upload, relative to the project's HTML5
	 *            location
	 * @param reloadHint
	 *            If applicable; whether to reload the page
	 * @return {@code true} If this virtual machine accepted the file for
	 *         updating.
	 */
	public boolean update(IFile resource) {
		boolean doUpdate = resource != null
				&& resource.getProject().equals(project);
		if (doUpdate) {
			server.update(getMainThreadId(), resource);
		}
		return doUpdate;
	}

	public void reload() {
		server.reload(getMainThreadId());
		try {
			debugTarget.resume();
		} catch (DebugException e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}
	
	private int getMainThreadId() {
		ReloadThreadReference mainThread = getMainThread();
		return mainThread == null ? JSODDServer.NO_SESSION : mainThread.getSessionId();
	}

	public ReloadThreadReference getMainThread() {
		return mainThread;
	}

	/**
	 * Updates a function reference on the client.
	 * 
	 * @param key
	 * @param source
	 */
	public void updateFunction(String key, String source) {
		for (ReloadThreadReference thread : threads.values()) {
			server.updateFunction(thread.getSessionId(), key, source);
		}
	}

	@Override
	public void received(int sessionId, String command, JSONObject json) {
		// TODID -- filtering is done in the eventqueue. For now.
		ReloadThreadReference thread = getThread(sessionId);

		boolean isClientSuspend = Boolean.parseBoolean(""
				+ json.get("suspended"));
		if (thread == null || thread.isSuspended() && !isClientSuspend) {
			return;
		}
		
		syncThread(thread);
		
		thread.markSuspended(true);
		JSONArray array = (JSONArray) json.get("stack");
		ReloadStackFrame[] frames = new ReloadStackFrame[array.size()];
		for (int i = 0; i < array.size(); i++) {
			ReloadStackFrame frame = new ReloadStackFrame(this, thread, json, i);
			// Stack traces are reported in the reverse order.
			frames[array.size() - 1 - i] = frame;
		}
		if (frames.length == 0) {
			frames = new ReloadStackFrame[1];
			frames[0] = new ReloadStackFrame(this, thread, json, -1);
		}
		thread.setFrames(frames);
		// suspend();
		eventQueue.received(command, json);
	}

	private void syncThread(ReloadThreadReference thread) {
		// Another miscommunication thing with JSDT;
		// sometimes the threadenterevent does not
		// propagate to the debug target, probably
		// due to a concurrency mishap.
		// Please note that we now open up a very small
		// possibility for race conditions but that is
		// preferable in this case.
		IThread[] dtThreads;
		try {
			dtThreads = debugTarget.getThreads();
		} catch (DebugException e) {
			// Just return!
			return;
		}
		
		for (IThread dtThread : dtThreads) {
			JavaScriptThread jsThread = (JavaScriptThread) dtThread;
			if (jsThread.matches(thread)) {
				return;
			}
		}
		
		eventQueue.received(ReloadEventQueue.THREAD_ENTER, thread);
	}

	public LocalVariableScope getLocalVariableScope(ScriptReference ref,
			int line) {
		// TODO: Faster?
		if (ref instanceof SimpleScriptReference) {
			IFile file = ((SimpleScriptReference) ref).getFile();
			JSODDSupport jsoddSupport = Html5Plugin.getDefault()
					.getJSODDSupport(file.getProject());
			LocalVariableScope scope = jsoddSupport.getScope(file, line);
			if (scope != null) {
				return scope;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return MessageFormat.format("JavaScript On-Device Debug VM #{0}, main thread session {1}, all threads: {2}", vmId, getMainThread(), allThreads());
	}

	public void setCurrentLocation(String location) {
		mainThread.setCurrentLocation(location);
	}

	public boolean isTerminated() {
		return isTerminated;
	}

	public ReloadThreadReference mainThread() {
		return mainThread;
	}

	public void dropToFrame(int dropToFrame) throws DebugException {
		IThread[] threads = getJavaScriptDebugTarget().getThreads();
		for (int i = 0; i < threads.length; i++) {
			IThread thread = threads[i];
			ReloadDropToFrame.dropToFrame(thread, dropToFrame);
		}
	}

	public IJavaScriptDebugTarget getJavaScriptDebugTarget() {
		return debugTarget;
	}

	public ProjectRedefinable getBaseline() {
		if (baseline == null) {
			JSODDSupport jsoddSupport = Html5Plugin.getDefault()
					.getJSODDSupport(project);
			if (jsoddSupport != null) {
				setBaseline(jsoddSupport.getBaseline());
			}
		}
		return baseline;
	}

	public void setBaseline(ProjectRedefinable baseline) {
		this.baseline = baseline;
	}

	public void setLaunch(ILaunch launch) {
		this.launch = launch;
	}

	public void setDebugTarget(IJavaScriptDebugTarget debugTarget) {
		this.debugTarget = debugTarget;
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public boolean canRedefine(IRedefinable redefinable) {
		synchronized (redefineSupport) {
			for (Map.Entry<Class, Boolean> supportsThis : redefineSupport.entrySet()) {
				if (redefinable != null
						&& supportsThis.getValue()
						&& supportsThis.getKey().isAssignableFrom(
								redefinable.getClass())) {
					return true;
				}
			}
		}
		return false;
	}

	public void setRedefineSupport(Class redefinables, boolean hasSupport) {
		redefineSupport.put(redefinables, hasSupport);
	}

	public void setBreakOnException(boolean breakOnException) {
		this.breakOnException = breakOnException;
		// Just refresh all breakpoints; this one does not happen often...
		for (ReloadThreadReference thread : threads.values()) {
			server.refreshBreakpoints(thread.getSessionId());
		}
	}

	public boolean getBreakOnException() {
		return this.breakOnException;
	}

	public ReloadThreadReference getThread(String threadId) {
		ReloadThreadReference thread = threads.get(threadId);
		return thread;
	}
	
	public ReloadThreadReference getThread(int sessionId) {
		for (ReloadThreadReference thread : threads.values()) {
			if (thread.getSessionId() == sessionId) {
				return thread;
			}
		}
		return null;
	}

	public ReloadThreadReference resetThread(String threadId) {
		ReloadThreadReference thread = getThread(threadId);
		if (thread == null) {
			thread = new ReloadThreadReference(this);
			thread.setSessionId(server.newUniqueId());
			thread.setCurrentLocation(threadId);
			threads.put(threadId, thread);
			
			if (mainThread == null) {
				mainThread = thread;
			}
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				boolean isMain = getMainThread() == thread;
				String mainStr = isMain ? "" : "MAIN ";
				CoreMoSyncPlugin.trace("Assigned id {0} to {2}thread {1}",
						thread.getSessionId(), thread.name(), mainStr);
			}
			eventQueue.received(ReloadEventQueue.THREAD_ENTER, thread);
		} else {
			resetThread(thread);
			thread.markSuspended(false, false);
		}
		return thread;
	}
	
	private void resetThread(ReloadThreadReference thread) {
		server.reset(thread.setSessionId(server.newUniqueId()));
	}

	public void killThread(int threadSessionId) {
		String removeThis = null;
		for (Map.Entry<String, ReloadThreadReference> thread : threads.entrySet()) {
			int id = thread.getValue().getSessionId();
			if (id == threadSessionId) {
				removeThis = thread.getKey();
			}
		}
		ReloadThreadReference thread = threads.remove(removeThis);
		if (thread == mainThread) {
			mainThread = (ReloadThreadReference) (threads.values().isEmpty() ? null : threads.values().toArray()[0]);
		}
		if (thread == null) {
			return;
		}
		thread.terminate();
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Killed thread {0}", thread.getSessionId());
		}
	}

	public Object evaluate(String input) throws InterruptedException, TimeoutException {
		return getMainThread().evaluate(input);
	}

	public void setProject(IProject project) {
		this.project = project;
	}
}
