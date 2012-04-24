package com.mobilesorcery.sdk.html5.live;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLoadBreakpoint;
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
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.jsdt.JavaScriptBreakpointDesc;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadVirtualMachine;

public class LiveServer implements IResourceChangeListener {

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

	static class InternalQueues {

		interface IMessageListener {
			public void received(int messageId, Object data);

			public int getSessionId();
		}

		private static final int PING_INTERVAL = 15000;

		private static final int POISON = -1;

		// TODO: Slow refactoring to make this class useful
		private final HashMap<Integer, LinkedBlockingQueue<DebuggerMessage>> consumers = new HashMap<Integer, LinkedBlockingQueue<DebuggerMessage>>();

		// TODO: Only one listener per message id? I guess so.
		private final HashMap<Integer, IMessageListener> messageListeners = new HashMap<Integer, IMessageListener>();

		private final HashMap<Integer, Long> takeTimestamps = new HashMap<Integer, Long>();

		private final HashSet<Integer> pendingPings = new HashSet<Integer>();

		private final Object queueLock = new Object();

		private Timer pinger;

		private DebuggerMessage poison() {
			return new DebuggerMessage(POISON);
		}

		private DebuggerMessage ping() {
			return new DebuggerMessage(PING);
		}

		public DebuggerMessage take(int sessionId) throws InterruptedException {
			LinkedBlockingQueue<DebuggerMessage> consumer = null;
			synchronized (queueLock) {
				consumer = consumers.get(sessionId);
				LinkedBlockingQueue<DebuggerMessage> newConsumer = new LinkedBlockingQueue<DebuggerMessage>(
						1024);
				if (consumer != null) {
					consumer.drainTo(newConsumer);
					consumer.offer(poison());
				}
				consumer = newConsumer;
				consumers.put(sessionId, consumer);
				takeTimestamps.put(sessionId, System.currentTimeMillis());
			}

			DebuggerMessage result = consumer.take();

			synchronized (queueLock) {
				takeTimestamps.remove(sessionId);
			}

			if (result.type == PING) {
				pendingPings.remove(sessionId);
			}

			if (result != null && result.type == POISON) {
				throw new InterruptedException();
			}
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace("TAKE: Session id {0}: {1}", sessionId,
						result);
			}
			return result;
		}

		public void offer(int sessionId, DebuggerMessage msg) {
			synchronized (queueLock) {
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin.trace("{2}Ê- OFFER: Session id {0}: {1}",
							sessionId, msg, new Date().toString());
					CoreMoSyncPlugin.trace("CONSUMERS: {0}", consumers);
				}
				LinkedBlockingQueue<DebuggerMessage> consumer = consumers
						.get(sessionId);
				if (consumer != null) {
					consumer.offer(msg);
				}
			}
		}

		private Object await(final int sessionId, DebuggerMessage msg,
				int timeout) throws InterruptedException, TimeoutException {
			final CountDownLatch cd = new CountDownLatch(1);
			final Object[] result = new Object[1];
			IMessageListener listener = new IMessageListener() {
				@Override
				public void received(int messageId, Object data) {
					result[0] = data;
					cd.countDown();
				}

				@Override
				public int getSessionId() {
					return sessionId;
				}
			};

			setMessageListener(sessionId, msg.getMessageId(), listener);

			offer(sessionId, msg);
			try {
				if (!cd.await(timeout, TimeUnit.SECONDS)) {
					if (CoreMoSyncPlugin.getDefault().isDebugging()) {
						CoreMoSyncPlugin.trace("Message timeout (#{0})",
								sessionId);
					}
					throw new TimeoutException();
				}
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin.trace(
							"WAITED FOR SESSION {0} AND GOT {1}", sessionId,
							result[0]);
				}
			} finally {
				clearMessageListener(sessionId);
			}
			return result[0];
		}

		private synchronized void setMessageListener(int sessionId, int id,
				IMessageListener listener) {
			synchronized (queueLock) {
				this.messageListeners.put(id, listener);
			}
		}

		private synchronized void clearMessageListener(int id) {
			synchronized (queueLock) {
				this.messageListeners.remove(id);
			}
		}

		public void setResult(int id, Object result) {
			synchronized (messageListeners) {
				IMessageListener listener = messageListeners.get(id);
				if (listener != null) {
					listener.received(id, result);
				}
			}
		}

		private void broadcast(DebuggerMessage msg) {
			synchronized (queueLock) {
				for (Integer sessionId : consumers.keySet()) {
					offer(sessionId, msg);
				}
			}
		}

		public void killSession(int sessionId) {
			synchronized (queueLock) {
				LinkedBlockingQueue<DebuggerMessage> sessionQueue = consumers
						.remove(sessionId);
				if (sessionQueue != null) {
					sessionQueue.offer(poison());
				}

				Set<Integer> messageListenerIds = new TreeSet<Integer>(
						messageListeners.keySet());
				for (Integer messageListenerId : messageListenerIds) {
					IMessageListener listener = messageListeners
							.get(messageListenerId);
					if (listener.getSessionId() == sessionId) {
						setResult(messageListenerId, null);
						clearMessageListener(messageListenerId);
					}
				}
			}
		}

		public void killAllSessions() {
			synchronized (queueLock) {
				Set<Integer> consumerIds = new TreeSet<Integer>(
						consumers.keySet());
				for (Integer sessionId : consumerIds) {
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
				}
			}, PING_INTERVAL, 2000);
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
					Long timeOfLastTake = takeTimestamps.get(sessionId);
					boolean isWaitingForPing = pendingPings.contains(sessionId);
					boolean needsPing = !isWaitingForPing && timeOfLastTake != null
							&& System.currentTimeMillis() - timeOfLastTake > PING_INTERVAL;
					if (needsPing) {
						pendingPings.add(sessionId);
						offer(sessionId, ping());
					}
				}
			}
		}
	}

	private final InternalQueues queues = new InternalQueues();

	private static final Charset UTF8 = Charset.forName("UTF8");

	public static final int NO_SESSION = -1;

	private static final int PING = 0;

	private static final int EVAL = 5;

	private static final int BREAKPOINT = 10;

	private static final int RESUME = 20;

	private static final int STEP = 30;

	private static final int SUSPEND = 200;

	private static final int DISCONNECT = 500;

	private static final int TERMINATE = 12000;

	public static final String TIMEOUT_ATTR = "live.timeout";

	public static final int DEFAULT_TIMEOUT = 5;

	public static final String SESSION_ID_ATTR = "sessionId";

	private class LiveServerHandler extends AbstractHandler {

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
			if (targetMatches(target, "/mobile/incoming")) {
				ReloadVirtualMachine vm = getVM(req.getRemoteAddr());
				int sessionId = vm == null ? NO_SESSION : vm
						.getCurrentSessionId();
				result = pushCommandsToClient(sessionId, preflight);
			} else if (targetMatches(target, "/mobile/breakpoint")) {
				// RACE CONDITION WILL OCCUR HERE!
				if (!preflight) {
					JSONObject command = parseCommand(req);
					Integer sessionId = extractSessionId(command);
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

		private JSONObject pushCommandsToClient(Integer session,
				boolean preflight) {
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

				Object queuedObject = queuedElement == null ? null
						: queuedElement.data;
				int queuedType = queuedElement == null ? -1
						: queuedElement.type;
				if (queuedType == BREAKPOINT) {
					Pair<Boolean, Object> bp = (Pair<Boolean, Object>) queuedObject;
					result = createBreakpointJSON(new Object[] { bp.second },
							bp.first);
				} else if (queuedType == RESUME) {
					result = newCommand("breakpoint-continue");
				} else if (queuedType == STEP) {
					result = newCommand(getStepCommand((Integer) queuedElement.data));
				} else if (queuedType == SUSPEND) {
					result = newCommand("suspend");
				} else if (queuedType == EVAL) {
					result = newCommand("eval");
					Pair<String, Integer> data = (Pair<String, Integer>) queuedObject;
					String expression = data.first;
					Integer stackDepth = data.second;
					result.put("data", expression);
					if (stackDepth != null) {
						result.put("stackDepth", stackDepth);
					} else {
						result.put("noStack", true);
					}
				} else if (queuedType == TERMINATE) {
					result = newCommand("terminate");
				} else if (queuedType == DISCONNECT) {
					result = newCommand("disconnect");
				} else if (queuedType == PING) {
					// Just return the ping created above!
				}
				result.put("id", queuedElement.getMessageId());
			} catch (InterruptedException e) {
				e.printStackTrace();
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
			if (targetMatches(target, "/mobile/init")) {
				// Just push the breakpoints!
				IBreakpoint[] bps = DebugPlugin.getDefault()
						.getBreakpointManager()
						.getBreakpoints(JavaScriptDebugModel.MODEL_ID);
				JSONObject jsonBps = createBreakpointJSON(bps, true);
				if (!preflight) {
					JSONObject command = parseCommand(req);
					int newSessionId = newSessionId();
					jsonBps.put(SESSION_ID_ATTR, newSessionId);
					String projectName = (String) command.get("project");
					MoSyncProject project = MoSyncProject.create(ResourcesPlugin.getPlugin().getWorkspace().getRoot().getProject(projectName));
					if (project != null) {
						ReloadVirtualMachine vm = resetVM(req, project, newSessionId);
						vm.setCurrentLocation((String) command.get("location"));
					}
				}
				return jsonBps;
			} else if (targetMatches(target, "/mobile/console")) {
				if (!preflight) {
					JSONObject command = parseCommand(req);
					if ("print-message".equals(getCommand(command))) {
						String level = "" + command.get("type");
						String msg = "" + command.get("message");
						if (CoreMoSyncPlugin.getDefault().isDebugging()) {
							CoreMoSyncPlugin.trace("{0}: {1}", level, msg);
						}
						for (ILineHandler consoleListener : consoleListeners) {
							consoleListener.newLine(level + "|" + msg);
						}
					} else if ("print-eval-result".equals(getCommand(command))) {
						Object result = command.get("result");
						int id = ((Long) command.get("id")).intValue();
						queues.setResult(id, result);
					}
				}
				return new JSONObject();
			}
			return null;
		}

		private boolean targetMatches(String target, String requestType) {
			return target != null && target.startsWith(requestType);
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
				try {
					if (bp instanceof IJavaScriptLineBreakpoint) {
						bp = toInternalFormat((IJavaScriptLineBreakpoint) bp);
					}
					if (bp instanceof JavaScriptBreakpointDesc) {
						JavaScriptBreakpointDesc lineBp = (JavaScriptBreakpointDesc) bp;
						int lineNo = bp instanceof IJavaScriptLoadBreakpoint ? -1
								: lineBp.getLineNumber();
						IResource resource = lineBp.getResource();
						String file = resource.getType() == IResource.ROOT ? "*" : resource.getFullPath().toPortableString();
						String condition = lineBp.getCondition();
						int hitCount = lineBp.getHitCount();

						JSONObject jsonBp = new JSONObject();
						jsonBp.put("file", file);
						jsonBp.put("line", lineNo);
						if (!Util.isEmpty(condition)) {
							jsonBp.put("condition", condition);
							jsonBp.put("conditionSuspend", lineBp.getConditionSuspend());
						}
						if (hitCount > 0) {
							jsonBp.put("hitcount", hitCount);
						}
						jsonBps.add(jsonBp);
					}
				} catch (Exception e) {
					CoreMoSyncPlugin.getDefault().log(e);
				}
			}
			command.put("data", jsonBps);
			return command;
		}

		private JavaScriptBreakpointDesc toInternalFormat(
				IJavaScriptLineBreakpoint lineBp) throws CoreException {
			boolean isLoadBp = lineBp instanceof IJavaScriptLoadBreakpoint;

			if (lineBp.getMarker() != null && lineBp.getMarker().exists()) {
				int lineNumber = lineBp instanceof IJavaScriptLoadBreakpoint ? -1
						: lineBp.getLineNumber();
				IResource resource = lineBp.getMarker().getResource();
				String condition = lineBp.getCondition();
				int hitCount = lineBp.getHitCount();
				String conditionSuspend = lineBp.isConditionSuspendOnTrue() ?
						JavaScriptBreakpointDesc.SUSPEND_ON_TRUE : JavaScriptBreakpointDesc.SUSPEND_ON_CHANGE;
				return new JavaScriptBreakpointDesc(resource, lineNumber,
						condition, conditionSuspend,hitCount);
			}
			return null;
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
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
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
						hostAddr, Integer.toString(getPort()), hostName);
			}
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	public static Integer extractSessionId(JSONObject command) {
		Object sessionIdObj = command.get(SESSION_ID_ATTR);
		Integer sessionId = sessionIdObj == null ? null : Integer
				.parseInt(sessionIdObj.toString());
		return sessionId;
	}

	public synchronized ReloadVirtualMachine getVM(int sessionId) {
		if (sessionId != NO_SESSION) {
			for (ReloadVirtualMachine vm : vmsByHost.values()) {
				if (vm.getCurrentSessionId() == sessionId) {
					return vm;
				}
			}
		}
		return null;
	}

	private ReloadVirtualMachine getVM(String remoteAddr) {
		ReloadVirtualMachine vm = vmsByHost.get(remoteAddr);
		if (vm != null && vm.isTerminated()) {
			vmsByHost.remove(remoteAddr);
			return null;
		}
		return vm;
	}

	public synchronized ReloadVirtualMachine resetVM(HttpServletRequest req,
			MoSyncProject project, int newSessionId) {
		String remoteIp = req.getRemoteAddr();
		ReloadVirtualMachine vm = getVM(remoteIp);
		if (vm == null) {
			if (!unassignedVMs.isEmpty()) {
				vm = unassignedVMs.remove(0);
			}
		} else {
			int oldSessionId = vm.getCurrentSessionId();
		}
		vm.reset(newSessionId, project);

		vmsByHost.put(remoteIp, vm);
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Assigned session {0} to address {1}",
					newSessionId, remoteIp);
		}
		return vm;
	}

	public int newSessionId() {
		return sessionId.incrementAndGet();
	}

	private Object awaitEvalResult(int sessionId, String expression,
			Integer stackDepth, int timeout) throws InterruptedException,
			TimeoutException {
		DebuggerMessage queuedExpression = new DebuggerMessage(EVAL,
				new Pair<String, Integer>(expression, stackDepth));
		return queues.await(sessionId, queuedExpression, timeout);
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

	public static IJavaScriptLineBreakpoint findBreakPoint(IPath file, long line) {
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

	public synchronized void stopServer(Object ref) throws CoreException {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
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

	public void setLineBreakpoint(boolean enabled, JavaScriptBreakpointDesc bp) {
		queues.broadcast(new DebuggerMessage(BREAKPOINT,
				new Pair<Boolean, Object>(enabled, bp)));
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
		queues.offer(sessionId, new DebuggerMessage(RESUME));
	}

	public void suspend(int sessionId) {
		try {
			queues.await(sessionId, new DebuggerMessage(SUSPEND), Integer.MAX_VALUE);
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	public void step(int sessionId, int stepType) {
		queues.offer(sessionId, new DebuggerMessage(STEP, stepType));
	}

	public void reset(int sessionId) {
		queues.killSession(sessionId);
	}

	public Object evaluate(int sessionId, String expression, Integer stackDepth)
			throws InterruptedException, TimeoutException {
		Object result = awaitEvalResult(sessionId, expression, stackDepth,
				getTimeout(sessionId));
		return result;
	}

	public void terminate(int sessionId) {
		try {
			ReloadVirtualMachine vm = getVM(sessionId);
			if (vm != null && !vm.isTerminated()) {
				DebuggerMessage msg = new DebuggerMessage(TERMINATE);
				queues.offer(sessionId, msg);
				// Just to make sure the terminate request is sent
				// before the server is killed.
				Thread.sleep(1000 * getTimeout(sessionId));
			}
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	public void disconnect(int sessionId) {
		try {
			ReloadVirtualMachine vm = getVM(sessionId);
			if (vm != null && !vm.isTerminated()) {
				DebuggerMessage msg = new DebuggerMessage(DISCONNECT);
				queues.offer(sessionId, msg);
			}
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	private int getTimeout(int sessionId) {
		// TODO: Use launch config/prefs. Hm. Prefs easier. And user-friendlier
		// :)
		return 3;
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

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		final HashSet<IProject> projectsToReload = new HashSet<IProject>();
		if (delta != null) {
			try {
				delta.accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						IResource resource = delta.getResource();
						if (resource != null && (delta.getFlags() & IResourceDelta.CONTENT) != 0) {
							IProject project = resource.getProject();
							JSODDSupport jsoddSupport = Html5Plugin.getDefault().getJSODDSupport(project);
							jsoddSupport.rewrite(resource.getFullPath(), null);
							if (project != null && resource.getType() == IResource.FILE) {
								projectsToReload.add(resource.getProject());
							}
							//try {
								if (resource.getType() == IResource.FILE) {
									//op.rewrite(resource.getFullPath());
								}
							/*} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}*/
						}
						return true;
					}
				});
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

}
