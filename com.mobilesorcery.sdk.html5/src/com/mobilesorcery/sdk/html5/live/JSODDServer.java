package com.mobilesorcery.sdk.html5.live;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLoadBreakpoint;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.StepRequest;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptDebugTarget;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;
import org.eclipse.wst.jsdt.debug.internal.core.breakpoints.JavaScriptExceptionBreakpoint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.RedefineException;
import com.mobilesorcery.sdk.html5.debug.RedefinitionResult;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.debug.hotreplace.ProjectRedefinable;
import com.mobilesorcery.sdk.html5.debug.jsdt.JavaScriptBreakpointDesc;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadRedefiner;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadValue;
import com.mobilesorcery.sdk.html5.live.JSODDServer.InternalQueues.ITimeoutListener;
import com.mobilesorcery.sdk.html5.ui.AskForRedefineResolutionDialog;
import com.mobilesorcery.sdk.ui.UIUtils;

public class JSODDServer implements IResourceChangeListener {

	static class DebuggerMessage {
		public static final Comparator<DebuggerMessage> COMPARATOR = new Comparator<DebuggerMessage>() {
			@Override
			public int compare(DebuggerMessage o1, DebuggerMessage o2) {
				int result = o1.type - o2.type;
				if (result == 0) {
					return o2.messageId - o1.messageId;
				}
				return result;
			}
			
		};
		static AtomicInteger idCounter = new AtomicInteger(0);
		int messageId = idCounter.incrementAndGet();
		AtomicBoolean processed = new AtomicBoolean(false);
		int type;
		Object data;
		private long timestamp;

		public DebuggerMessage(int type) {
			this(type, null);
		}

		public DebuggerMessage(int type, Object data) {
			this.type = type;
			this.data = data;
		}

		void setOfferTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		public long getOfferTimestamp() {
			return timestamp;
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

		interface ITimeoutListener {
			public void timeoutOccurred(int sessionId);
		}

		private static final int PING_INTERVAL = 8000;

		private static final int POISON = -1;

		// TODO: Slow refactoring to make this class useful
		private final HashMap<Integer, PriorityBlockingQueue<DebuggerMessage>> consumers = new HashMap<Integer, PriorityBlockingQueue<DebuggerMessage>>();

		private final HashMap<Integer, IMessageListener> messageListeners = new HashMap<Integer, IMessageListener>();

		private ITimeoutListener timeoutListener = null;

		private final HashMap<Integer, Long> lastHeartbeats = new HashMap<Integer, Long>();

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
			PriorityBlockingQueue<DebuggerMessage> consumer = null;
			synchronized (queueLock) {
				consumer = consumers.get(sessionId);
				PriorityBlockingQueue<DebuggerMessage> newConsumer = new PriorityBlockingQueue<DebuggerMessage>(
						1024, DebuggerMessage.COMPARATOR);
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
				// takeTimestamps.remove(sessionId);
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
					CoreMoSyncPlugin.trace("{2} - OFFER: Session id {0}: {1}",
							sessionId, msg, new Date().toString());
					CoreMoSyncPlugin.trace("CONSUMERS: {0}", consumers);
				}
				PriorityBlockingQueue<DebuggerMessage> consumer = consumers
						.get(sessionId);
				if (consumer != null) {
					msg.setOfferTimestamp(System.currentTimeMillis());
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
						CoreMoSyncPlugin.trace("{2}s message timeout (#{0}, #{1})",
								sessionId, msg.getMessageId(), timeout);
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

		public void setTimeoutListener(ITimeoutListener timeoutListener) {
			this.timeoutListener = timeoutListener;
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
			IMessageListener listener;
			synchronized (queueLock) {
				listener = messageListeners.get(id);
			}
			if (listener != null) {
				listener.received(id, result);
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
			Set<Integer> messageListenerIds = new TreeSet<Integer>();
			synchronized (queueLock) {
				PriorityBlockingQueue<DebuggerMessage> sessionQueue = consumers
						.remove(sessionId);
				takeTimestamps.remove(sessionId);
				if (sessionQueue != null) {
					sessionQueue.offer(poison());
				}

				
				messageListenerIds.addAll(messageListeners.keySet());
			}
			
			for (Integer messageListenerId : messageListenerIds) {
				IMessageListener listener = messageListeners
						.get(messageListenerId);
				if (listener != null && listener.getSessionId() == sessionId) {
					setResult(messageListenerId, null);
					clearMessageListener(messageListenerId);
				}
			}
		}

		public void killAllSessions() {
			Set<Integer> consumerIds = new TreeSet<Integer>();
			synchronized (queueLock) {
				 consumerIds.addAll(consumers.keySet());
			}
			for (Integer sessionId : consumerIds) {
				killSession(sessionId);
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
			}, PING_INTERVAL, PING_INTERVAL);
		}

		public void stopPingDeamon() {
			if (pinger != null) {
				pinger.cancel();
			}
			pinger = null;
		}

		protected void pingAll() {
			// This ping is there to make sure that long polling does not
			// timeout
			// on the client side.
			// And if the client is disconnected/does not respond we need to
			// handle that too.
			HashMap<Integer, PriorityBlockingQueue<DebuggerMessage>> consumersCopy = new HashMap<Integer, PriorityBlockingQueue<DebuggerMessage>>();
			synchronized (queueLock) {
				 consumersCopy.putAll(consumers);
			}
			for (Integer sessionId : consumersCopy.keySet()) {
				Long timeOfLastTake = takeTimestamps.get(sessionId);
				boolean isWaitingForPing = pendingPings.contains(sessionId);
				long now = System.currentTimeMillis();
				boolean needsPing = !isWaitingForPing
						&& timeOfLastTake != null
						&& now - timeOfLastTake > PING_INTERVAL;
				Long lastHeartbeat = lastHeartbeats.get(sessionId);
				boolean timeoutOccured = lastHeartbeat != null
						&& now - lastHeartbeat > 2 * PING_INTERVAL;
				if (timeoutOccured && timeoutListener != null) {
					killSession(sessionId);
					timeoutListener.timeoutOccurred(sessionId);
				}
				if (needsPing) {
					if (CoreMoSyncPlugin.getDefault().isDebugging()) {
						CoreMoSyncPlugin.trace("Ping will be sent to {0}",
								sessionId);
					}
					pendingPings.add(sessionId);
					ping(sessionId);
				}
			}
		}
		
		public void ping(int sessionId) {
			offer(sessionId, ping());
		}

		public void heartbeat(int sessionId) {
			lastHeartbeats.put(sessionId, System.currentTimeMillis());
		}
	}

	private final InternalQueues queues = new InternalQueues();

	private static final Charset UTF8 = Charset.forName("UTF8");

	public static final int NO_SESSION = -1;

	private static final int PING = 0;

	private static final int EVAL = 5;

	private static final int REDEFINE = 7;

	private static final int RELOAD = 8;

	private static final int BREAKPOINT = 10;

	private static final int REFRESH_BREAKPOINTS = 12;

	private static final int RESUME = 20;

	private static final int DROP_TO_FRAME = 25;

	private static final int STEP = 30;

	private static final int SUSPEND = 200;

	private static final int DISCONNECT = 500;

	public static final int TERMINATE = 12000;
	
	public static final int TIMEOUT = -800;

	public static final String TIMEOUT_ATTR = "live.timeout";

	public static final int DEFAULT_TIMEOUT = 5;

	public static final String SESSION_ID_ATTR = "sessionId";

	private class JSODDServerHandler extends AbstractHandler {

		private final HashMap<Object, Thread> waitThreads = new HashMap<Object, Thread>();

		@Override
		public void handle(String target, Request baseRequest,
				HttpServletRequest req, HttpServletResponse res)
				throws IOException {
			try {
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin.trace(
							"{3}: STARTED {0} REQUEST {1} ON THREAD {2}", req
									.getMethod(), target, Thread
									.currentThread().getName(), new Date()
									.toString());
				}

				boolean preflight = "OPTIONS".equals(req.getMethod());

				// Heartbeat.
				ReloadVirtualMachine vm = getVM(req.getRemoteAddr());
				if (vm != null && vm.getCurrentSessionId() != NO_SESSION) {
					queues.heartbeat(vm.getCurrentSessionId());
				}

				// Preflight.
				configureForPreflight(req, res);

				// COMMANDS
				JSONObject command = preflight
						|| !targetMatches(target, "/mobile/") || targetMatches(target, "/mobile/incoming") ? null
						: parseCommand(req);
				Object result = handleFetch(target, req, res);
				if (result == null) {
					result = handleCommand(target, command, req, res,
						preflight);
				}
				if (result == null) {
					result = waitForClient(target, command, req, res, preflight);
				}

				if (result != null) {
					if (CoreMoSyncPlugin.getDefault().isDebugging()) {
						CoreMoSyncPlugin.trace("SEND ({0}): {1}", target,
								result);
					}
					writeResponse(result, res);
				}

				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin.trace(
							"{3}: FINISHED {0} REQUEST {1} ON THREAD {2}", req
									.getMethod(), target, Thread
									.currentThread().getName(), new Date()
									.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e);
			}
		}

		private void writeResponse(Object obj, HttpServletResponse res) throws CoreException, IOException {
			int length = 0;
			int status = HttpServletResponse.SC_OK;
			InputStream contents = null;
			String fallbackContentType = null;
			
			if (obj instanceof JSONObject) {
				obj = ((JSONObject) obj).toJSONString();
				fallbackContentType = "application/json;charset=utf-8";
			}
			if (obj instanceof String) {
				byte[] data = ((String) obj).getBytes(UTF8);
				contents = new ByteArrayInputStream(data);
				length = data.length;
			} else if (obj instanceof IFile) {
				// We put large files in memory too.
				IFile file = (IFile) obj;
				if (!file.exists()) {
					// Then we fake an empty file
					contents = new ByteArrayInputStream(new byte[0]);
					length = 0;
				} else {
					contents = file.getContents(true);
					length = (int) file.getLocation().toFile().length();
				}
			} else {
				String errorMsg;
				fallbackContentType = "text/plain";
				if (obj instanceof Exception) {
					errorMsg = ((Exception) obj).getMessage();
					status = HttpServletResponse.SC_NOT_FOUND;
				} else {
					errorMsg = "Internal error: wrong type of response object: " + obj;
					status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
				}
				contents = new ByteArrayInputStream(errorMsg.getBytes());
				length = errorMsg.getBytes().length;
			}
			
			res.setStatus(status);
			if (fallbackContentType != null) {
				res.setContentType(fallbackContentType);
			}
			res.setContentLength(length);
			ServletOutputStream output = res.getOutputStream();
			Util.transfer(contents, output);
			output.flush();
			Util.safeClose(output);
			Util.safeClose(contents);

		}

		private JSONObject waitForClient(String target, JSONObject command,
				HttpServletRequest req, HttpServletResponse res,
				boolean preflight) {
			JSONObject result = null;
			if (targetMatches(target, "/mobile/incoming")) {
				ReloadVirtualMachine vm = getVM(req.getRemoteAddr());
				int sessionId = vm == null ? NO_SESSION : vm
						.getCurrentSessionId();
				result = pushCommandsToClient(sessionId, preflight);
			} else if (targetMatches(target, "/mobile/breakpoint")) {
				// RACE CONDITION WILL OCCUR HERE!
				if (!preflight) {
					Integer sessionId = extractSessionId(command);
					notifyCommandListeners(getCommand(command), command);
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
			JSONObject result = createBreakpointJSON(new Object[0], true, false);

			try {
				DebuggerMessage queuedElement = queues.take(session);

				Object queuedObject = queuedElement == null ? null
						: queuedElement.data;
				int queuedType = queuedElement == null ? -1
						: queuedElement.type;
				if (queuedType == BREAKPOINT) {
					Pair<Boolean, Object> bp = (Pair<Boolean, Object>) queuedObject;
					result = createBreakpointJSON(new Object[] { bp.second },
							bp.first, false);
				} else if (queuedType == RESUME) {
					result = newCommand("breakpoint-continue");
				} else if (queuedType == STEP) {
					result = newCommand(getStepCommand((Integer) queuedElement.data));
				} else if (queuedType == RELOAD) {
					String command = queuedObject == null ? "reload" : "update";
					result = newCommand(command);
					if (queuedObject != null) {
						IFile resource = (IFile) queuedObject;
						result.put("resource", Html5Plugin.getDefault().getLocalPath(resource)
								.toOSString());
					}
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
				} else if (queuedType == REFRESH_BREAKPOINTS) {
					IBreakpoint[] bps = DebugPlugin.getDefault()
							.getBreakpointManager()
							.getBreakpoints(JavaScriptDebugModel.MODEL_ID);
					result = createBreakpointJSON(bps, true, true);
				} else if (queuedType == REDEFINE) {
					Pair<String, String> data = (Pair<String, String>) queuedObject;
					result = newCommand("update-function");
					JSONArray functions = new JSONArray();
					JSONObject function = new JSONObject();
					function.put("key", data.first);
					function.put("definition", data.second);
					functions.add(function);
					result.put("functions", functions);
				} else if (queuedType == TERMINATE) {
					result = newCommand("terminate");
				} else if (queuedType == DISCONNECT) {
					result = newCommand("disconnect");
				} else if (queuedType == PING) {
					// Just return the ping created above!
				}
				result.put("id", queuedElement.getMessageId());
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

		private Object handleFetch(String target, HttpServletRequest req, HttpServletResponse res) throws IOException {
			if (targetMatches(target, "/mobile/")) {
				return null;
			}
			
			Object source = null;
			String resource = null;
			IProject project = null;
			ReloadVirtualMachine vm = getVM(req.getRemoteAddr());
			
			// Hm, the baseurl may be either relative to the url or to the full path...
			// Anyway, we must init the vm at first fetch, and at that point
			// we also need the project name. Subsequent requests may or may not
			// have the fetch/%PROJECT_NAME% prefix.
			if (targetMatches(target, "/fetch/")) {
				String[] parts = target.substring("/fetch/".length()).split(
						"/", 2);
				if (parts.length == 2) {
					String projectName = parts[0];
					resource = parts[1];
					project = ResourcesPlugin.getWorkspace().getRoot()
							.getProject(projectName);
					if (project != null) {
						if (vm == null) {
							// The session id will be assigned soon.
							resetVM(req, MoSyncProject.create(project), -1);
						}
					}
				}
			} else {
				if (vm != null) {
					resource = target;
					project = vm.getProject();	
				}
				
			}
			
			if (resource == null) {
				return new IOException("Could not find resource " + target);
			}
			source = doFetch(project, resource);
			
			// No caching!
			res.setHeader("Pragma", "no-cache");
			res.setHeader("Cache-Control", "no-cache");
			res.setHeader("Access-Control-Allow-Origin", "*");
			res.setContentType(guessContentTypeFromName(resource));
			return source;
		}
		
		private String guessContentTypeFromName(String name) {
			String contentType = URLConnection.guessContentTypeFromName(name);
			File file = new File(name);
			if ("js".equals(Util.getExtension(file))) {
				contentType = "text/javascript";
			} else if ("css".equals(Util.getExtension(file))) {
				contentType = "text/css";
			} else if ("html".equals(Util.getExtension(file))) {
				contentType = "text/html";
			}
			if (contentType == null) {
				contentType = "text/plain";
			}
			return contentType;
		}
		
		private Object doFetch(IProject project, String localPath) throws IOException {
			JSODDSupport jsoddSupport = Html5Plugin.getDefault()
					.getJSODDSupport(project);
			IFile file = project.getFile(Html5Plugin
					.getHTML5Folder(project).append(localPath));
			if (jsoddSupport.requiresFullBuild()) {
				return new IOException(MessageFormat.format("Project not built. Please build project {0}.", project.getName()));
			}
			String source = jsoddSupport.getInstrumentedSource(file);
			if (source == null) {
				return file;
			}
			return source;
		}

		private Object handleCommand(String target, JSONObject command,
				HttpServletRequest req, HttpServletResponse res,
				boolean preflight) {
			if (targetMatches(target, "/mobile/init")) {
				// Just push the breakpoints!
				IBreakpoint[] bps = DebugPlugin.getDefault()
						.getBreakpointManager()
						.getBreakpoints(JavaScriptDebugModel.MODEL_ID);
				JSONObject jsonBps = createBreakpointJSON(bps, true, true);
				if (command != null) {
					int newSessionId = newSessionId();
					jsonBps.put(SESSION_ID_ATTR, newSessionId);
					String projectName = (String) command.get("project");
					MoSyncProject project = MoSyncProject
							.create(ResourcesPlugin.getPlugin().getWorkspace()
									.getRoot().getProject(projectName));
					if (project != null) {
						ReloadVirtualMachine vm = resetVM(req, project,
								newSessionId);
						vm.setCurrentLocation((String) command.get("location"));
					}
				}
				return jsonBps;
			} else if (targetMatches(target, "/mobile/console")) {
				if (command != null) {
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
						if (!command.containsKey("result")) {
							result = ReloadValue.UNDEFINED;
						}
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

		private JSONObject createBreakpointJSON(Object[] bps, boolean enabled,
				boolean reset) {
			JSONObject command = new JSONObject();
			command.put("command", enabled ? "set-breakpoints"
					: "clear-breakpoints");
			if (reset) {
				command.put("reset", true);
			}
			JSONArray jsonBps = new JSONArray();
			for (Object bp : bps) {
				try {
					if (bp instanceof IJavaScriptLineBreakpoint) {
						bp = toInternalFormat((IJavaScriptLineBreakpoint) bp);
					}
					if (bp instanceof JavaScriptBreakpointDesc) {
						JavaScriptBreakpointDesc lineBp = (JavaScriptBreakpointDesc) bp;
						lineBp = syncBreakpoint(lineBp);
						
						int lineNo = bp instanceof IJavaScriptLoadBreakpoint ? -1
								: lineBp.getLineNumber();
						IResource resource = lineBp.getResource();
						String file = resource.getType() == IResource.ROOT ? "*"
								: resource.getFullPath().toPortableString();
						String condition = lineBp.getCondition();
						int hitCount = lineBp.getHitCount();
						
						
						JSONObject jsonBp = new JSONObject();
						jsonBp.put("file", file);
						JSODDSupport jsoddSupport = resource.getType() == IResource.FILE ? Html5Plugin
								.getDefault().getJSODDSupport(
										resource.getProject()) : null;
						int instrumentedLine = jsoddSupport == null ? lineNo
								: jsoddSupport.findClosestBreakpointLine(
										resource.getFullPath(), lineNo);
						if (instrumentedLine >= 0) {
							lineNo = instrumentedLine;
						}
						jsonBp.put("line", lineNo);
						if (!Util.isEmpty(condition)) {
							jsonBp.put("condition", condition);
							jsonBp.put("conditionSuspend",
									lineBp.getConditionSuspend());
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

		private JavaScriptBreakpointDesc syncBreakpoint(JavaScriptBreakpointDesc lineBp) {
			IResource resource = lineBp.getResource();
			if (resource != null) {
				IPath path = resource.getFullPath();
				int lineNo = lineBp.getLineNumber();
				IJavaScriptLineBreakpoint underlyingBp = JSODDSupport.findBreakPoint(path, lineNo);
				if (underlyingBp != null) {
					try {
						String condition = underlyingBp.isConditionEnabled() ? lineBp.getCondition() : null;
						String suspendStrategy = underlyingBp.isConditionSuspendOnTrue() ? 
								JavaScriptBreakpointDesc.SUSPEND_ON_TRUE :
								JavaScriptBreakpointDesc.SUSPEND_ON_CHANGE;
						lineBp = lineBp.setCondition(condition);
						lineBp = lineBp.setConditionSuspend(suspendStrategy);
					} catch (CoreException e) {
						CoreMoSyncPlugin.getDefault().log(e);
					}
				}
			}
			return lineBp;
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
				String conditionSuspend = lineBp.isConditionSuspendOnTrue() ? JavaScriptBreakpointDesc.SUSPEND_ON_TRUE
						: JavaScriptBreakpointDesc.SUSPEND_ON_CHANGE;
				return new JavaScriptBreakpointDesc(resource, lineNumber,
						condition, conditionSuspend, hitCount);
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
	private final CopyOnWriteArrayList<ILiveServerListener> lifecycleListeners = new CopyOnWriteArrayList<ILiveServerListener>();
	private final CopyOnWriteArrayList<ILiveServerCommandListener> commandListeners = new CopyOnWriteArrayList<ILiveServerCommandListener>();
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
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
					IResourceChangeEvent.POST_CHANGE);
			server = new Server(getPort());
			server.setThreadPool(new ExecutorThreadPool(5, 128, 120));
			server.setHandler(new JSODDServerHandler());
			Connector connector = new SelectChannelConnector();
			connector.setPort(getPort());
			connector.setMaxIdleTime(120000);
			server.setConnectors(new Connector[] { connector });
			server.start();
			queues.startPingDeamon();
			queues.setTimeoutListener(new ITimeoutListener() {
				@Override
				public void timeoutOccurred(int sessionId) {
					ReloadVirtualMachine vm = getVM(sessionId);
					if (vm != null) {
						IJavaScriptDebugTarget debugTarget = vm
								.getJavaScriptDebugTarget();
						if (debugTarget != null) {
							try {
								debugTarget.terminate();
							} catch (DebugException e) {
								CoreMoSyncPlugin.getDefault().log(e);
							}
						}
						notifyTerminateListeners(vm);
					}
				}
			});
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

	public synchronized List<ReloadVirtualMachine> getVMs(
			boolean includeUnassigned) {
		ArrayList<ReloadVirtualMachine> result = new ArrayList<ReloadVirtualMachine>();
		for (ReloadVirtualMachine vm : vmsByHost.values()) {
			if (includeUnassigned || !unassignedVMs.contains(vm)) {
				result.add(vm);
			}
		}
		return result;
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
		boolean reset = vm != null;
		if (!reset) {
			if (!unassignedVMs.isEmpty()) {
				vm = unassignedVMs.remove(0);
			}
		} else {
			int oldSessionId = vm.getCurrentSessionId();
		}
		// TODO: What if vm == null here?
		
		vm.reset(newSessionId, project, remoteIp);

		vmsByHost.put(remoteIp, vm);
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Assigned session {0} to address {1}",
					newSessionId, remoteIp);
		}
		
		notifyInitListeners(vm, reset);
		
		return vm;
	}

	public int newSessionId() {
		int traceMask = CoreMoSyncPlugin.getDefault().isDebugging() ? 0xffff
				: 0;
		return traceMask + sessionId.incrementAndGet();
	}

	private Object awaitEvalResult(int sessionId, String expression,
			Integer stackDepth, int timeout) throws InterruptedException,
			TimeoutException {
		DebuggerMessage queuedExpression = new DebuggerMessage(EVAL,
				new Pair<String, Integer>(expression, stackDepth));
		return queues.await(sessionId, queuedExpression, timeout);
	}

	public void addListener(ILiveServerListener listener) {
		this.lifecycleListeners.add(listener);
	}

	public void removeListener(ILiveServerListener listener) {
		this.lifecycleListeners.remove(listener);
	}
	
	public void addListener(ILiveServerCommandListener listener) {
		this.commandListeners.add(listener);
	}

	public void removeListener(ILiveServerCommandListener listener) {
		this.commandListeners.remove(listener);
	}

	private void notifyCommandListeners(String commandName, JSONObject command) {
		// TODO: Send directly to the proper VM instead!!
		for (ILiveServerCommandListener listener : commandListeners) {
			listener.received(commandName, command);
		}
	}
	
	private void notifyInitListeners(ReloadVirtualMachine vm, boolean reset) {
		for (ILiveServerListener listener : lifecycleListeners) {
			listener.inited(vm, reset);
		}
	}

	private void notifyTerminateListeners(ReloadVirtualMachine vm) {
		for (ILiveServerListener listener : lifecycleListeners) {
			listener.timeout(vm);
		}
	}

	public synchronized void stopServer(Object ref) throws CoreException {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				IResourceChangeEvent.POST_CHANGE);
		refs.remove(ref);
		if (refs.isEmpty()) {
			unassignedVMs.clear();
			vmsByHost.clear();
			queues.setTimeoutListener(null);
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

	private int getPort() throws IOException {
		return Html5Plugin.getDefault().getServerURL().getPort();
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
			queues.await(sessionId, new DebuggerMessage(SUSPEND),
					Integer.MAX_VALUE);
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	public void step(int sessionId, int stepType) {
		queues.offer(sessionId, new DebuggerMessage(STEP, stepType));
	}

	public void dropToFrame(int sessionId, int stackDepth) {
		queues.offer(sessionId, new DebuggerMessage(DROP_TO_FRAME, stackDepth));
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

	public void update(int sessionId, IFile resource) {
		queues.offer(sessionId, new DebuggerMessage(RELOAD, resource));
	}

	public void reload(int sessionId) {
		queues.offer(sessionId, new DebuggerMessage(RELOAD, null));
	}

	public void updateFunction(int sessionId, String key, String source) {
		queues.offer(sessionId, new DebuggerMessage(REDEFINE,
				new Pair<String, String>(key, source)));
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
				queues.killSession(sessionId);
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

	public void refreshBreakpoints(int sessionId) {
		queues.offer(sessionId, new DebuggerMessage(REFRESH_BREAKPOINTS));
	}

	private int getTimeout(int sessionId) {
		// TODO: Use launch config/prefs. Hm. Prefs easier. And user-friendlier
		// :)
		return Html5Plugin.getDefault().getTimeout();
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
		if (Html5Plugin.getDefault().getSourceChangeStrategy() == Html5Plugin.DO_NOTHING) {
			// Just return!
			return;
		}

		List<ReloadVirtualMachine> vms = getVMs(false);
		final HashMap<IProject, ProjectRedefinable> replacements = new HashMap<IProject, ProjectRedefinable>();
		for (ReloadVirtualMachine vm : vms) {
			IProject project = vm.getProject();
			if (!replacements.containsKey(project)) {
				ProjectRedefinable replacement = vm.getBaseline().shallowCopy();
				replacements.put(project, replacement);
			}
		}

		IResourceDelta delta = event.getDelta();
		
		final boolean[] requiredRewrite = new boolean[] { false };

		// This code seems to be repeated elsewhere; refactor! TODO!
		if (delta != null) {
			try {
				delta.accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta delta)
							throws CoreException {
						IResource resource = delta.getResource();
						if (resource != null
								&& (delta.getFlags() & IResourceDelta.CONTENT) != 0) {
							IProject project = resource.getProject();
							if (project != null
									&& resource.getType() == IResource.FILE
									&& !MoSyncBuilder.isInOutput(project, resource)) {
								ProjectRedefinable replacement = replacements
										.get(project);
								if (replacement == null) {
									return false;
								}
								requiredRewrite[0] = true;
								JSODDSupport jsoddSupport = Html5Plugin
										.getDefault().getJSODDSupport(project);
								if (delta.getKind() == IResourceDelta.REMOVED) {
									jsoddSupport.delete(resource.getFullPath(),
											replacement);
								} else {
									jsoddSupport.rewrite(
											resource.getFullPath(), null,
											replacement);
								}
							}
						}
						return true;
					}
				});
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		if (!requiredRewrite[0]) {
			return;
		}
		
		int failedRedefineResolution = 0;
		for (ReloadVirtualMachine vm : vms) {
			IProject project = vm.getProject();
			ProjectRedefinable replacement = replacements.get(project);
			boolean forceReload = Html5Plugin.getDefault()
					.getSourceChangeStrategy() == Html5Plugin.RELOAD;
			ReloadRedefiner redefiner = new ReloadRedefiner(vm, forceReload);
			ProjectRedefinable baseline = vm.getBaseline();
			boolean updateBaseline = false;
			try {
				if (baseline == null) {
					throw new RedefineException(
							RedefinitionResult
									.unrecoverable("Client out of sync"));
				}
				baseline.redefine(replacement, redefiner);
				redefiner.commit(forceReload);
				updateBaseline = true;
			} catch (RedefineException e) {
				if (failedRedefineResolution == 0) {
					failedRedefineResolution = askForRedefineResolution(e);
				}
				switch (failedRedefineResolution) {
				case RedefinitionResult.RELOAD:
					try {
						redefiner.commit(true);
						updateBaseline = true;
					} catch (RedefineException nestedException) {
						// Ignore.
					}
					break;
				case RedefinitionResult.TERMINATE:
					try {
						vm.getJavaScriptDebugTarget().terminate();
					} catch (DebugException debugException) {
						CoreMoSyncPlugin.getDefault().log(debugException);
					}
					break;
				default:
					// Continue/cancel; do nothing.
				}
			}
			if (updateBaseline) {
				vm.setBaseline(replacement);
			}
		}
	}

	private int askForRedefineResolution(final RedefineException e) {
		final int[] reloadStrategy = new int[] { Html5Plugin.getDefault()
				.getReloadStrategy() };
		if (reloadStrategy[0] == RedefinitionResult.UNDETERMINED) {
			Display d = PlatformUI.getWorkbench().getDisplay();
			UIUtils.onUiThread(d, new Runnable() {
				@Override
				public void run() {
					Shell shell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					reloadStrategy[0] = AskForRedefineResolutionDialog.open(
							shell, e);
				}
			}, false);
		}
		return reloadStrategy[0];
	}

	public Set<Integer> getSessions() {
		// MOVE TO QUEUES.
		synchronized (queues.queueLock) {
			return new HashSet<Integer>(this.queues.consumers.keySet());
		}
	}

}
