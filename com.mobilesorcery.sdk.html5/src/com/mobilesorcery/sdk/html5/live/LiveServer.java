package com.mobilesorcery.sdk.html5.live;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
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
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDDebugTarget;
import com.mobilesorcery.sdk.html5.debug.JSThread;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadVirtualMachine;

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
		Object data;

		public DebuggerMessage(Object data) {
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
			return id + ": " + data;
		}
	}

	interface IMessageListener {
		public void received(int id, Object data);
	}

	static class InternalQueues {

		private static final int INCOMING_QUEUE = 0;
		private static final int DEBUG_QUEUE = 1;
		private static final Object POISON = new Object();

		// TODO: Slow refactoring to make this class useful

		private final HashMap<Integer, LinkedBlockingQueue<DebuggerMessage>> debugQueues = new HashMap<Integer, LinkedBlockingQueue<DebuggerMessage>>();
		private final HashMap<Integer, Thread> debugWaitingThreads = new HashMap<Integer, Thread>();
		private final HashMap<Integer, LinkedBlockingQueue<DebuggerMessage>> incomingQueues = new HashMap<Integer, LinkedBlockingQueue<DebuggerMessage>>();
		private final HashMap<Integer, Thread> incomingWaitingThreads = new HashMap<Integer, Thread>();

		private Object queueLock = new Object();

		public LinkedBlockingQueue<DebuggerMessage> getQueue(int queueType,
				int sessionId) {
			return getQueues(queueType).get(sessionId);
		}

		public Map<Integer, LinkedBlockingQueue<DebuggerMessage>> getQueues(
				int queueType) {
			HashMap<Integer, LinkedBlockingQueue<DebuggerMessage>> queues = queueType == INCOMING_QUEUE ? incomingQueues
					: debugQueues;
			return queues;
		}

		public DebuggerMessage take(int queueType, int sessionId)
				throws InterruptedException {
			LinkedBlockingQueue<DebuggerMessage> queue = getQueue(queueType,
					sessionId);
			DebuggerMessage result = queue == null ? null : queue.take();
			if (result != null) {
				while (!result.setProcessed()) {
					result = queue.take();
				}
			}
			if (result.data == POISON) {
				throw new InterruptedException();
			}
			return result;
		}

		public void offer(int queueType, int sessionId, DebuggerMessage msg) {
			getQueue(queueType, sessionId).offer(msg);
		}

		public void broadcast(DebuggerMessage msg) {
			synchronized (queueLock) {
				broadcast(INCOMING_QUEUE, msg);
				broadcast(DEBUG_QUEUE, msg);
			}
		}

		private void broadcast(int queueType, DebuggerMessage msg) {
			Map<Integer, LinkedBlockingQueue<DebuggerMessage>> queues = getQueues(queueType);
			for (Integer sessionId : queues.keySet()) {
				offer(queueType, sessionId, msg);
			}
		}

		public void newSession(int sessionId) {
			synchronized (queueLock) {
				debugQueues.put(sessionId,
						new LinkedBlockingQueue<DebuggerMessage>());
				incomingQueues.put(sessionId,
						new LinkedBlockingQueue<DebuggerMessage>());
			}
		}

		public void killSession(int sessionId) {
			synchronized (queueLock) {
				LinkedBlockingQueue<DebuggerMessage> debugQueue = debugQueues
						.remove(sessionId);
				LinkedBlockingQueue<DebuggerMessage> incomingQueue = incomingQueues
						.remove(sessionId);
				debugQueue.offer(new DebuggerMessage(POISON));
				incomingQueue.offer(new DebuggerMessage(POISON));
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

	private static final Object RESUME = new Object();

	private static final long TIMEOUT_IN_SECONDS = 5;

	public static final int NO_SESSION = -1;

	private class LiveServerHandler extends AbstractHandler {

		private static final String SESSION_ID_ATTR = "sessionId";
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

			String remote = req.getRemoteAddr();
			int port = req.getRemotePort();
			System.err.println(remote + ", " + port);
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
					CoreMoSyncPlugin.trace("SEND: {0}", result);
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
				int sessionId = vmsByHost.get(req.getRemoteAddr()).getCurrentSessionId();
				result = pushCommandsToClient(InternalQueues.INCOMING_QUEUE,
						Integer.toString(sessionId), preflight);
			} else if ("/mobile/breakpoint".equals(target)) {
				// RACE CONDITION WILL OCCUR HERE!
				if (!preflight) {
					JSONObject command = parseCommand(req);
					if (listener != null) {
						listener.received(getCommand(command), command);
					}
					String sessionId =  "" + command.get(SESSION_ID_ATTR);
					result = pushCommandsToClient(InternalQueues.DEBUG_QUEUE,
							sessionId, preflight);
				}
			}
			return result;
		}

		private JSONObject error(String msg) {
			JSONObject result = new JSONObject();
			result.put("errorMsg", msg);
			return result;
		}

		private JSONObject pushCommandsToClient(int queueType,
				String sessionId, boolean preflight) {
			if (preflight) {
				return null;
			}

			if (sessionId == null) {
				return error("Session not initialized");
			}

			int session = Integer.parseInt(sessionId);

			JSONObject result = null;

			try {
				ArrayList bps = new ArrayList();
				DebuggerMessage queuedElement = queues.take(queueType, session);

				Object queuedObject = queuedElement == null ? null : queuedElement.data;
				if (queuedObject instanceof InternalLineBreakpoint) {
					InternalLineBreakpoint bp = (InternalLineBreakpoint) queuedObject;
					result = createBreakpointJSON(new Object[] { bp },
							bp.enabled);
				} else if (queuedObject == RESUME) {
					result = newCommand("breakpoint-continue");
				}
				if (queuedObject instanceof String) {
					result = newCommand("eval");
					result.put("data", queuedObject);
					result.put("id", queuedElement.getMessageId());
				}
			} catch (InterruptedException e) {
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin
							.trace("Dropped connection (often temporarily).");
				}
			}
			return result;
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
				System.err.println("RECEIVED JSON COMMAND: " + json);
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
					int newSessionId = newSessionId();
					jsonBps.put(SESSION_ID_ATTR, newSessionId);
					resetVM(req, newSessionId);
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
						String input = "" + command.get("input");
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
	private ILiveServerListener listener;
	private CopyOnWriteArrayList<ILineHandler> consoleListeners = new CopyOnWriteArrayList<ILineHandler>();
	private AtomicInteger sessionId = new AtomicInteger(1);
	private IdentityHashMap<Object, Object> refs = new IdentityHashMap<Object, Object>();
	private ArrayList<ReloadVirtualMachine> unassignedVMs = new ArrayList<ReloadVirtualMachine>();
	private HashMap<String, ReloadVirtualMachine> vmsByHost = new HashMap<String, ReloadVirtualMachine>();

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
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				InetAddress host = InetAddress.getLocalHost();
				byte[] ipAddr = host.getAddress();
				String hostName = host.getHostName();
				CoreMoSyncPlugin.trace("Started live server at {0}:{1} ({2})",
						Util.toBase16(ipAddr), Integer.toString(getPort()),
						hostName);
			}
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	public synchronized void resetVM(HttpServletRequest req, int newSessionId) {
		String remoteIp = req.getRemoteAddr();
		ReloadVirtualMachine vm = vmsByHost.get(remoteIp);
		if (vm == null) {
			if (!unassignedVMs.isEmpty()) {
				vm = unassignedVMs.remove(0);
			}
		} else {
			int oldSessionId = vm.getCurrentSessionId();
			queues.killSession(oldSessionId);
		}
		vm.setCurrentSessionId(newSessionId);
		queues.newSession(newSessionId);
		vmsByHost.put(remoteIp, vm);
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Assigned session {0} to address {1}", newSessionId, remoteIp);
		}
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
				clearMessageListener(id);
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
		System.err.format("EVALUATED %d AND GOT %s", id, result[0]);
		return result[0];
	}

	public void setListener(ILiveServerListener listener) {
		this.listener = listener;
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
		queues.broadcast(new DebuggerMessage(new InternalLineBreakpoint(
				enabled, file, line)));
	}

	public void setBreakpoint(IJavaScriptLineBreakpoint bp) {
		queues.broadcast(new DebuggerMessage(bp));
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
		queues.offer(InternalQueues.DEBUG_QUEUE, sessionId,
				new DebuggerMessage(RESUME));
	}

	public String evaluate(int sessionId, String expression)
			throws InterruptedException, TimeoutException {
		DebuggerMessage queuedExpression = new DebuggerMessage(expression);
		queues.offer(InternalQueues.DEBUG_QUEUE, sessionId, queuedExpression);
		String result = awaitEvalResult(queuedExpression.getMessageId(), 3,
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
