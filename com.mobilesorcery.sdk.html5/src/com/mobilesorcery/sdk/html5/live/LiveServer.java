package com.mobilesorcery.sdk.html5.live;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.StepRequest;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.thread.QueuedThreadPool;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDDebugTarget;
import com.mobilesorcery.sdk.html5.debug.JSThread;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.live.LiveServer.DebuggerMessage;

public class LiveServer {

	private class InternalLineBreakpoint {

		boolean enabled;
		IFile file;
		int line;

		public InternalLineBreakpoint(boolean enabled,
				IFile file, int line) {
			this.enabled = enabled;
			this.file = file;
			this.line = line;
		}

	}

	static class DebuggerMessage {
		static AtomicInteger idCounter = new AtomicInteger(0);
		int messageId = idCounter.incrementAndGet();
		AtomicBoolean processed = new AtomicBoolean(false);
		int type;
		Object data;

		public DebuggerMessage(int type) {
			this(type, null);
		}

		public DebuggerMessage(int type, Object data) {
			this.type = type;
			this.data = data;
		}

		public boolean setProcessed() {
			return !processed.getAndSet(true);
		}

		public int getMessageId() {
			return messageId;
		}

		@Override
		public String toString() {
			String id = String.format((processed.get() ? "(#%d)" : "#%d"),
					messageId);
			return id + ": " + type + ", " + data;
		}
	}

	interface IMessageListener {
		public void received(int id, Object data);
	}

	static class InternalQueues {

		private static final int PING_INTERVAL = 15000;

		private static final int POISON = -1;

		// TODO: Slow refactoring to make this class useful
		private final HashMap<Integer, LinkedBlockingQueue<DebuggerMessage>> consumers = new HashMap<Integer, LinkedBlockingQueue<DebuggerMessage>>();

		private final HashMap<Integer, Long> takeTimestamps = new HashMap<Integer, Long>();

		private final Object queueLock = new Object();

		private Timer pinger;

		private DebuggerMessage poison() {
			return new DebuggerMessage(POISON);
		}

		private DebuggerMessage ping() {
			return new DebuggerMessage(PING);
		}

		public DebuggerMessage take(int sessionId)
				throws InterruptedException {
			LinkedBlockingQueue<DebuggerMessage> consumer = null;
			synchronized (queueLock) {
				consumer = consumers.get(sessionId);
				LinkedBlockingQueue<DebuggerMessage> newConsumer = new LinkedBlockingQueue<DebuggerMessage>(1024);
				if (consumer != null) {
					consumer.drainTo(newConsumer);
					consumer.offer(poison());
				}
				consumer = newConsumer;
				consumers.put(sessionId, consumer);
			}

			DebuggerMessage result = consumer.take();

			if (result != null && result.type == POISON) {
				throw new InterruptedException();
			}
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace("TAKE: Session id {0}: {1}", sessionId, result);
			}
			return result;
		}


/*		private synchronized void setWaiting(int queueType, int sessionId, boolean isWaiting) {
			int delta = isWaiting ? +1 : -1;
			Pair<Integer, Integer> key = new Pair<Integer, Integer>(queueType, sessionId);
			AtomicInteger waitCount = waitingCounts.get(key);
			if (waitCount == null) {
				waitCount = new AtomicInteger(0);
				waitingCounts.put(key, waitCount);
			}
			waitCount.addAndGet(delta);
			if (waitCount.intValue() == 0) {
				waitingCounts.remove(key);
			}
		}

		private int getWaitCount(int queueType, int sessionId) {
			AtomicInteger result = waitingCounts.get(new Pair<Integer, Integer>(queueType, sessionId));
			return result == null ? 0 : result.intValue();
		}*/

		/**
		 * Sends interrupt to all waiting threads.
		 * @param queueType
		 * @param sessionId
		 */
		/*public void sendInterruptSignal(int queueType, int sessionId) {
			// All kind of deadlocks can happen here -- but this is NOT a general class...
			int waitCount = getWaitCount(queueType, sessionId);
			CoreMoSyncPlugin.trace("Send interrupt to {0} (session id {2}, queue type {1})", waitCount, queueType, sessionId);
			for (int i = 0; i < waitCount; i++) {
				getQueue(queueType, sessionId).offer(new DebuggerMessage(POISON));
			}
		}*/

		public void offer(int sessionId, DebuggerMessage msg) {
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace("{2}Ê- OFFER: Session id {0}: {1}", sessionId, msg, new Date().toString());
				CoreMoSyncPlugin.trace("CONSUMERS: {0}", consumers);
			}
			synchronized (queueLock) {
				LinkedBlockingQueue<DebuggerMessage> consumer = consumers.get(sessionId);
				if (consumer != null) {
					takeTimestamps.put(sessionId, System.currentTimeMillis());
					consumer.offer(msg);
				}
			}
		}

		/*public void broadcast(DebuggerMessage msg) {
			synchronized (queueLock) {
				broadcast(msg);
				broadcast(msg);
			}
		}*/

		private void broadcast(DebuggerMessage msg) {
			synchronized (queueLock) {
				for (Integer sessionId : consumers.keySet()) {
					offer(sessionId, msg);
				}
			}
		}

		public void killSession(int sessionId) {
			synchronized (queueLock) {
				//setWaiting(INCOMING_QUEUE, sessionId, false);
				//setWaiting(DEBUG_QUEUE, sessionId, false);

				LinkedBlockingQueue<DebuggerMessage> sessionQueue = consumers.remove(sessionId);
				if (sessionQueue != null) {
					sessionQueue.offer(poison());
				}
				/*LinkedBlockingQueue<DebuggerMessage> debugQueue = debugQueues.remove(sessionId);
				LinkedBlockingQueue<DebuggerMessage> incomingQueue = incomingQueues
						.remove(sessionId);
				debugQueue.offer(new DebuggerMessage(POISON));
				incomingQueue.offer(new DebuggerMessage(POISON));*/
				//safeTake(DEBUG_QUEUE, sessionId);
				//safeTake(INCOMING_QUEUE, sessionId);
			}
		}

		public void killAllSessions() {
			synchronized (queueLock) {
				for (Integer sessionId : consumers.keySet()) {
					killSession(sessionId);
				}
			}
		}

		@Override
		public String toString() {
			StringBuffer result = new StringBuffer();
			synchronized (queueLock) {
				for (Integer sessionId : consumers.keySet()) {
					result.append(consumers.get(sessionId));
					result.append("\n");
				}
			}
			return result.toString();
		}

		public void startPingDeamon() {
			pinger = new Timer();
			pinger.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					pingAll();
				} }, PING_INTERVAL, 2000);
		}

		public void stopPingDeamon() {
			if (pinger != null) {
				pinger.cancel();
			}
			pinger = null;
		}

		protected void pingAll() {
			synchronized (queueLock) {
				for (Integer sessionId : consumers.keySet()) {
					Long timeOfLastPing = takeTimestamps.get(sessionId);
					boolean needsPing = timeOfLastPing == null || System.currentTimeMillis() - timeOfLastPing > PING_INTERVAL;
					if (needsPing) {
						offer(sessionId, ping());
					}
				}
			}
		}
	}

	// TODO: Too many queues. JETTY Continuations?
	// private LinkedBlockingQueue<DebuggerMessage> debugQueue = new
	// LinkedBlockingQueue<DebuggerMessage>();
	// private final LinkedBlockingQueue<DebuggerMessage> incomingQueue = new
	// LinkedBlockingQueue<DebuggerMessage>();
	private final InternalQueues queues = new InternalQueues();

	private final LinkedBlockingQueue<Object> evalInputQueue = new LinkedBlockingQueue<Object>();
	private final LinkedBlockingQueue<Object> evalResultQueue = new LinkedBlockingQueue<Object>();

	// TODO: Only one listener per message id? I guess so.
	private final HashMap<Integer, IMessageListener> messageListeners = new HashMap<Integer, LiveServer.IMessageListener>();

	private static final Charset UTF8 = Charset.forName("UTF8");

	private static final long TIMEOUT_IN_SECONDS = 5;

	public static final int NO_SESSION = -1;

	private static final int BREAKPOINT = 1;

	private static final int RESUME = 2;

	private static final int STEP = 3;

	private static final int EVAL = 1000;

	private static final int PING = 2000;

	public static final String TIMEOUT_ATTR = "live.timeout";

	public static final int DEFAULT_TIMEOUT = 5;

	private class LiveServerHandler extends AbstractHandler {

		private static final String SESSION_ID_ATTR = "sessionId";

		private static final String SUSPEND_ATTR = "suspend";

		private final HashMap<Object, Thread> waitThreads = new HashMap<Object, Thread>();

		@Override
		public void handle(String target, HttpServletRequest req,
				HttpServletResponse res, int dispatch) throws IOException {
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace(
						"{3}: STARTED {0} REQUEST {1} ON THREAD {2}", req
								.getMethod(), target, Thread.currentThread()
								.getName(), new Date().toString());
			}

			boolean preflight = "OPTIONS".equals(req.getMethod());

			// FIXME
			JSODDDebugTarget debug = JSODDDebugTarget.getLast();

			// Preflight.
			configureForPreflight(req, res);

			// COMMANDS
			JSONObject result = handleCommand(target, req, res, preflight);
			if (result == null) {
				result = waitForClient(target, req, res, preflight);
			}

			if (result != null) {
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin.trace("SEND ({0}): {1}", target, result);
				}
				String output = result.toJSONString();
				res.setStatus(HttpServletResponse.SC_OK);
				res.setContentType("application/json;charset=utf-8");
				res.setContentLength(output.getBytes(UTF8).length);
				res.getWriter().print(result);
				res.getWriter().flush();
				res.getWriter().close();
			}

			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace(
						"{3}: FINISHED {0} REQUEST {1} ON THREAD {2}", req
								.getMethod(), target, Thread.currentThread()
								.getName(), new Date().toString());
			}
		}

		private JSONObject waitForClient(String target, HttpServletRequest req,
				HttpServletResponse res, boolean preflight) {
			JSONObject result = null;
			if (target.startsWith("/mobile/incoming")) {
				ReloadVirtualMachine vm = vmsByHost.get(req.getRemoteAddr());
				int sessionId = vm == null ? NO_SESSION : vm.getCurrentSessionId();
				result = pushCommandsToClient(sessionId, preflight);
			} else if ("/mobile/breakpoint".equals(target)) {
				// RACE CONDITION WILL OCCUR HERE!
				if (!preflight) {
					JSONObject command = parseCommand(req);
					Object sessionIdObj = command.get(SESSION_ID_ATTR);
					Integer sessionId = sessionIdObj == null ? null : Integer.parseInt(sessionIdObj.toString());
					notifyListeners(getCommand(command), command);
					result = pushCommandsToClient(sessionId, preflight);
				} else {
					return new JSONObject();
				}
			}
			return result;
		}

		private JSONObject error(String msg) {
			JSONObject result = new JSONObject();
			result.put("errorMsg", msg);
			return result;
		}

		private JSONObject pushCommandsToClient(Integer session, boolean preflight) {
			if (preflight) {
				return new JSONObject();
			}

			if (session == null) {
				return error("Session not initialized");
			}

			// We use a zero-length breakpoint list as 'ping'
			JSONObject result = createBreakpointJSON(new Object[0], true);

			try {
				ArrayList bps = new ArrayList();
				DebuggerMessage queuedElement = queues.take(session);

				Object queuedObject = queuedElement == null ? null : queuedElement.data;
				int queuedType = queuedElement == null ? -1 : queuedElement.type;
				if (queuedType == BREAKPOINT) {
					InternalLineBreakpoint bp = (InternalLineBreakpoint) queuedObject;
					result = createBreakpointJSON(new Object[] { bp },
							bp.enabled);
				} else if (queuedType == RESUME) {
					result = newCommand("breakpoint-continue");
				} else if (queuedType == STEP) {
					result = newCommand(getStepCommand((Integer) queuedElement.data));
				} else if (queuedType == EVAL) {
					result = newCommand("eval");
					result.put("data", queuedObject);
					result.put("id", queuedElement.getMessageId());
				} else if (queuedType == PING) {
					// Just return the ping created above!
				}
			} catch (InterruptedException e) {
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin
							.trace("Dropped connection (often temporarily).");
				}
			}
			return result;
		}

		private String getStepCommand(int stepType) {
			switch (stepType) {
			case StepRequest.STEP_INTO:
	        	return "break-on-next";
			case StepRequest.STEP_OUT:
				return "breakpoint-step-out";
			case StepRequest.STEP_OVER:
				return "breakpoint-step-over";
			default:
				return "breakpoint-continue";
			}
		}

		private void assignNewWaitThread() {
			Thread previousThread = waitThreads.get("");
			if (previousThread != Thread.currentThread()) {
				if (previousThread != null) {
					previousThread.interrupt();
				}
				waitThreads.put("", Thread.currentThread());
			}

		}

		private DebuggerMessage nextMessage(
				LinkedBlockingQueue<DebuggerMessage> queue)
				throws InterruptedException {
			DebuggerMessage result = queue.take();
			return result;
		}

		private void configureForPreflight(HttpServletRequest req,
				HttpServletResponse res) {
			res.setStatus(HttpServletResponse.SC_OK);
			res.setContentLength(0);
			res.setHeader("Access-Control-Allow-Origin", "*");
			res.setHeader("Access-Control-Allow-Methods",
					req.getHeader("Access-Control-Request-Method"));
			res.setHeader("Access-Control-Allow-Headers",
					req.getHeader("Access-Control-Request-Headers"));
		}

		private JSONObject parseCommand(HttpServletRequest req) {
			try {
				JSONObject json = (JSONObject) new JSONParser()
						.parse(new InputStreamReader(req.getInputStream()));
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin.trace("RECEIVED JSON COMMAND: {0}", json);
				}
				return json;
			} catch (Exception e) {
				// Ignore.
				e.printStackTrace();
			}
			return null;
		}

		private JSONObject handleCommand(String target, HttpServletRequest req,
				HttpServletResponse res, boolean preflight) {
			if ("/mobile/init".equals(target)) {
				// Just push the breakpoints!
				IBreakpoint[] bps = DebugPlugin.getDefault()
						.getBreakpointManager()
						.getBreakpoints(JavaScriptDebugModel.MODEL_ID);
				JSONObject jsonBps = createBreakpointJSON(bps, true);
				if (!preflight) {
					JSONObject command = parseCommand(req);
					int newSessionId = newSessionId();
					jsonBps.put(SESSION_ID_ATTR, newSessionId);
					ReloadVirtualMachine vm = resetVM(req, newSessionId);
					vm.setCurrentLocation((String) command.get("location"));
				}
				return jsonBps;
			} else if ("/mobile/console".equals(target)) {
				if (!preflight) {
					JSONObject command = parseCommand(req);
					if ("print-message".equals(getCommand(command))) {
						String level = "" + command.get("type");
						String msg = "" + command.get("message");
						if (CoreMoSyncPlugin.getDefault().isDebugging()) {
							CoreMoSyncPlugin.trace("{0}: {1}", level, msg);
						}
						for (ILineHandler consoleListener : consoleListeners) {
							consoleListener.newLine(msg);
						}
					} else if ("print-eval-result".equals(getCommand(command))) {
						String result = "" + command.get("result");
						int id = ((Long) command.get("id")).intValue();
						newEvalResult(id, result);
					}
				}
				return new JSONObject();
			}
			return null;
		}

		private String getCommand(JSONObject command) {
			return (String) command.get("command");
		}

		private JSONObject createBreakpointJSON(Object[] bps, boolean enabled) {
			JSONObject command = new JSONObject();
			command.put("command", enabled ? "set-breakpoints"
					: "clear-breakpoints");
			JSONArray jsonBps = new JSONArray();
			for (Object bp : bps) {
				if (bp instanceof IJavaScriptLineBreakpoint) {
					try {
						IJavaScriptLineBreakpoint lineBp = (IJavaScriptLineBreakpoint) bp;
						if (lineBp.getMarker() != null
								&& lineBp.getMarker().exists()) {
							int lineNo = lineBp.getLineNumber();
							IResource resource = lineBp.getMarker().getResource();
							String expr = lineBp.getCondition();
							if (resource.getType() == IResource.FILE) {
								bp = new InternalLineBreakpoint(enabled,
									(IFile) resource, lineNo);
							}
						}
					} catch (Exception e) {
						// TODO!
						e.printStackTrace();
					}

				}

				if (bp instanceof InternalLineBreakpoint) {
					InternalLineBreakpoint lineBp = (InternalLineBreakpoint) bp;
					JSONArray jsonBp = new JSONArray();
					IFile file = lineBp.file;
					int lineNo = lineBp.line;
					jsonBp.add(file.getFullPath().toPortableString());
					jsonBp.add(lineNo);
					jsonBps.add(jsonBp);
				}
			}
			command.put("data", jsonBps);
			System.err.println("CREATING BREAKPOINTS FOR CLIENT: " + command);
			return command;
		}

		private JSONObject newCommand(String command) {
			JSONObject result = new JSONObject();
			result.put("command", command);
			return result;
		}

	}

	private final Object mutex = new Object();
	private Server server;
	private final CopyOnWriteArrayList<ILiveServerListener> listeners = new CopyOnWriteArrayList<ILiveServerListener>();
	private final CopyOnWriteArrayList<ILineHandler> consoleListeners = new CopyOnWriteArrayList<ILineHandler>();
	private final AtomicInteger sessionId = new AtomicInteger(1);
	private final IdentityHashMap<Object, Object> refs = new IdentityHashMap<Object, Object>();
	private final ArrayList<ReloadVirtualMachine> unassignedVMs = new ArrayList<ReloadVirtualMachine>();
	private final HashMap<String, ReloadVirtualMachine> vmsByHost = new HashMap<String, ReloadVirtualMachine>();

	public synchronized void startServer(Object ref) throws CoreException {
		refs.put(ref, true);
		if (server != null) {
			return;
		}
		try {
			server = new Server(getPort());
			server.setThreadPool(new QueuedThreadPool(5));
			server.setHandler(new LiveServerHandler());
			Connector connector = new SelectChannelConnector();
			connector.setPort(getPort());
			connector.setMaxIdleTime(120000);
			server.setConnectors(new Connector[] { connector });
			server.start();
			queues.startPingDeamon();
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				InetAddress host = InetAddress.getLocalHost();
				String hostName = host.getHostName();
				String hostAddr = host.getHostAddress();
				CoreMoSyncPlugin.trace("Started live server at {0}:{1} ({2})",
						hostAddr, Integer.toString(getPort()),
						hostName);
			}
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	public synchronized ReloadVirtualMachine resetVM(HttpServletRequest req, int newSessionId) {
		String remoteIp = req.getRemoteAddr();
		ReloadVirtualMachine vm = vmsByHost.get(remoteIp);
		if (vm == null) {
			if (!unassignedVMs.isEmpty()) {
				vm = unassignedVMs.remove(0);
			}
		} else {
			int oldSessionId = vm.getCurrentSessionId();
		}
		vm.reset(newSessionId);

		vmsByHost.put(remoteIp, vm);
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Assigned session {0} to address {1}", newSessionId, remoteIp);
		}
		return vm;
	}

	public int newSessionId() {
		return sessionId.incrementAndGet();
	}

	private synchronized void setMessageListener(int id,
			IMessageListener listener) {
		this.messageListeners.put(id, listener);
	}

	private synchronized void clearMessageListener(int id) {
		this.messageListeners.remove(id);
	}

	public void newEvalResult(int id, String result) {
		synchronized (messageListeners) {
			IMessageListener listener = messageListeners.get(id);
			if (listener != null) {
				listener.received(id, result);
			}
		}
	}

	private String awaitEvalResult(int id, int timeout, TimeUnit unit)
			throws InterruptedException, TimeoutException {
		final CountDownLatch cd = new CountDownLatch(1);
		final String[] result = new String[1];
		IMessageListener listener = new IMessageListener() {
			@Override
			public void received(int id, Object data) {
				result[0] = (String) data;
				cd.countDown();
			}
		};

		setMessageListener(id, listener);
		if (!cd.await(timeout, unit)) {
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace("Debug eval timeout (#{0})", id);
			}
			throw new TimeoutException();
		}
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("EVALUATED {0} AND GOT {1}", id, result[0]);
		}
		clearMessageListener(id);
		return result[0];
	}

	public void addListener(ILiveServerListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(ILiveServerListener listener) {
		this.listeners.remove(listener);
	}

	private void notifyListeners(String commandName, JSONObject command) {
		for (ILiveServerListener listener : listeners) {
			listener.received(commandName, command);
		}
	}

	public IStackFrame[] extractStackFrames(JSODDDebugTarget debugTarget,
			JSThread thread, JSONArray frames) {
		IStackFrame[] result = new IStackFrame[frames.size()];
		for (int i = 0; i < frames.size(); i++) {
			String name = (String) frames.get(i);
			result[i] = new JSStackFrame(debugTarget, thread, name, i);
		}
		return result;
	}

	public static IJavaScriptLineBreakpoint findBreakPoint(IPath file,
			long line) {
		IBreakpoint[] bps = DebugPlugin.getDefault().getBreakpointManager()
				.getBreakpoints(JavaScriptDebugModel.MODEL_ID);
		for (IBreakpoint bp : bps) {
			if (bp instanceof IJavaScriptLineBreakpoint) {
				IJavaScriptLineBreakpoint lineBp = (IJavaScriptLineBreakpoint) bp;
				try {
					if (line == lineBp.getLineNumber()
							&& Util.equals(file,
									new Path(lineBp.getScriptPath()))) {
						return lineBp;
					}
				} catch (CoreException e) {
					// Just IGNORE!
				}
			}
		}
		return null;
	}

	private static IProject getProject(IJavaScriptLineBreakpoint bp) {
		IMarker marker = bp.getMarker();
		if (marker != null) {
			return marker.getResource().getProject();
		}
		return null;
	}

	public synchronized void stopServer(Object ref) throws CoreException {
		refs.remove(ref);
		if (refs.isEmpty()) {
			unassignedVMs.clear();
			vmsByHost.clear();
			queues.stopPingDeamon();
			queues.killAllSessions();
			if (server != null) {
				try {
					server.stop();
				} catch (Exception e) {
					throw new CoreException(new Status(IStatus.ERROR,
							Html5Plugin.PLUGIN_ID, e.getMessage(), e));
				}
			}
			server = null;
		}
	}

	public void setLineBreakpoint(boolean enabled, IFile file, int line) {
		queues.broadcast(new DebuggerMessage(BREAKPOINT, new InternalLineBreakpoint(
				enabled, file, line)));
	}

	public void setBreakpoint(IJavaScriptLineBreakpoint bp) {
		queues.broadcast(new DebuggerMessage(BREAKPOINT, bp));
	}

	public void removeBreakpoint(IJavaScriptLineBreakpoint breakpoint) {

	}

	private int getPort() {
		return 8511;
	}

	public void setDirty(Set<IProject> dirtyProjects) {

	}

	public boolean isSuspended() {
		return false;
	}

	public void resume(int sessionId) {
		queues.offer(sessionId,
				new DebuggerMessage(RESUME));
	}

	public void step(int sessionId, int stepType) {
		queues.offer(sessionId,
				new DebuggerMessage(STEP, stepType));
	}

	public void reset(int sessionId) {
		queues.killSession(sessionId);
	}

	public String evaluate(int sessionId, String expression)
			throws InterruptedException, TimeoutException {
		DebuggerMessage queuedExpression = new DebuggerMessage(EVAL, expression);
		queues.offer(sessionId, queuedExpression);
		String result = awaitEvalResult(queuedExpression.getMessageId(), 5,
				TimeUnit.SECONDS);
		return result;
	}

	public void addConsoleListener(ILineHandler handler) {
		consoleListeners.add(handler);
	}

	public void removeConsoleListener(ILineHandler handler) {
		consoleListeners.remove(handler);
	}

	public synchronized void registerVM(ReloadVirtualMachine vm) {
		unassignedVMs.add(vm);
	}

}
