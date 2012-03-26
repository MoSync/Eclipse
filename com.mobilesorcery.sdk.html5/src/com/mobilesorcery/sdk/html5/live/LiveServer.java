package com.mobilesorcery.sdk.html5.live;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer.ConditionObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.wst.jsdt.core.ast.IConditionalExpression;
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
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSThread;
import com.mobilesorcery.sdk.html5.debug.JSODDDebugTarget;

public class LiveServer {

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
			String id = String.format((processed.get() ? "(#%d)" : "#%d"), messageId);
			return id + ": " + data;
		}
	}

	interface IMessageListener {
		public void received(int id, Object data);
	}

	// TODO: Too many queues. JETTY Continuations?
	private final LinkedBlockingQueue<DebuggerMessage> debugQueue = new LinkedBlockingQueue<DebuggerMessage>();
	private final LinkedBlockingQueue<DebuggerMessage> incomingQueue = new LinkedBlockingQueue<DebuggerMessage>();

	private final LinkedBlockingQueue<Object> evalInputQueue = new LinkedBlockingQueue<Object>();
	private final LinkedBlockingQueue<Object> evalResultQueue = new LinkedBlockingQueue<Object>();

	// TODO: Only one listener per message id? I guess so.
	private final HashMap<Integer, IMessageListener> messageListeners = new HashMap<Integer, LiveServer.IMessageListener>();

	private static final Charset UTF8 = Charset.forName("UTF8");

	private static final String INCOMING_QUEUE = "incoming";
	private static final String DEBUG_QUEUE = "debug";
	private static final String ALL_QUEUES = "all";

	private static final Object RESUME = new Object();

	private static final long TIMEOUT_IN_SECONDS = 5;


	private class LiveServerHandler extends AbstractHandler {

		private final HashMap<Object, Thread> waitThreads = new HashMap<Object, Thread>();

		@Override
		public void handle(String target, HttpServletRequest req,
				HttpServletResponse res, int dispatch) throws IOException {
			// TODO: Rely on idle time?
			System.err.println(MessageFormat.format("{3}: STARTED {0} REQUEST {1} ON THREAD {2}", req.getMethod(), target, Thread.currentThread().getName(), new Date().toString()));

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
		    	System.err.println("SEND " + result);
		    	String output = result.toJSONString();
		    	res.setStatus(HttpServletResponse.SC_OK);
		    	res.setContentType("application/json;charset=utf-8");
		        res.setContentLength(output.getBytes(UTF8).length);
		        res.getWriter().print(result);
				res.getWriter().flush();
				res.getWriter().close();
		    }

			System.err.println(MessageFormat.format("{3}: FINISHED {0} REQUEST {1} ON THREAD {2}", req.getMethod(), target, Thread.currentThread().getName(), new Date().toString()));
		}

		private JSONObject waitForClient(String target, HttpServletRequest req,
				HttpServletResponse res, boolean preflight) {
			JSONObject result = null;
		    LinkedBlockingQueue<DebuggerMessage> queue = null;
		    if ("/mobile/incoming".equals(target)) {
		    	queue = incomingQueue;
		    	assignNewWaitThread(DEBUG_QUEUE);
		    	result = pushCommandsToClient(queue, preflight);
			} else if ("/mobile/breakpoint".equals(target)) {
				queue = debugQueue;
				// RACE CONDITION WILL OCCUR HERE!
				if (!preflight) {
					JSONObject command = parseCommand(req);
					if (listener != null) {
						listener.received(getCommand(command), command);
					}
					result = pushCommandsToClient(queue, preflight);
				}
			}
		    return result;
		}

		private JSONObject pushCommandsToClient(LinkedBlockingQueue<DebuggerMessage> queue, boolean preflight) {
		    JSONObject result = null;
			if (!preflight && queue != null) {
				try {
					ArrayList bps = new ArrayList();
					DebuggerMessage queuedElement = nextMessage(queue);
					while (!queuedElement.setProcessed()) {
						queuedElement = nextMessage(queue);
					}
					Object queuedObject = queuedElement.data;
					if (queuedObject instanceof IJavaScriptLineBreakpoint) {
						result = createBreakpointJSON(new IBreakpoint[] { (IBreakpoint) queuedObject });
					} else if (queuedObject == RESUME) {
						result = newCommand("breakpoint-continue");
					} if (queuedObject instanceof String) {
						result = newCommand("eval");
						result.put("data", queuedObject);
						result.put("id", queuedElement.getMessageId());
					}
				} catch (InterruptedException e) {
					if (CoreMoSyncPlugin.getDefault().isDebugging()) {
						CoreMoSyncPlugin.trace("Dropped connection (often temporarily).");
					}
				}
			}
			return result;
		}

		private void assignNewWaitThread(Object key) {
			Thread previousThread = waitThreads.get(key);
			if (previousThread != Thread.currentThread()) {
				if (previousThread != null) {
					previousThread.interrupt();
				}
				waitThreads.put(key, Thread.currentThread());
			}

		}

		private DebuggerMessage nextMessage(LinkedBlockingQueue<DebuggerMessage> queue) throws InterruptedException {
			DebuggerMessage result = queue.take();
			return result;
		}

		private void configureForPreflight(HttpServletRequest req,
				HttpServletResponse res) {
			res.setStatus(HttpServletResponse.SC_OK);
			res.setContentLength(0);
			res.setHeader("Access-Control-Allow-Origin", "*");
			res.setHeader("Access-Control-Allow-Methods", req.getHeader("Access-Control-Request-Method"));
		    res.setHeader("Access-Control-Allow-Headers", req.getHeader("Access-Control-Request-Headers"));
		}

		private JSONObject parseCommand(HttpServletRequest req) {
			try {
				JSONObject json = (JSONObject) new JSONParser().parse(new InputStreamReader(req.getInputStream()));
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
				IBreakpoint[] bps = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(JavaScriptDebugModel.MODEL_ID);
				JSONObject jsonBps = createBreakpointJSON(bps);
				return jsonBps;
			} else if ("/mobile/console".equals(target)) {
				if (!preflight) {
					JSONObject command = parseCommand(req);
					if ("print-message".equals(getCommand(command))) {
						String level = "" + command.get("type");
						System.err.println(level + ": " + command.get("message"));
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

		private JSONObject createBreakpointJSON(IBreakpoint[] bps) {
			JSONObject command = new JSONObject();
			command.put("command", "set-breakpoints");
			JSONArray jsonBps = new JSONArray();
			for (IBreakpoint bp : bps) {
				if (bp instanceof IJavaScriptLineBreakpoint) {
					JSONArray jsonBp = new JSONArray(); // TODO: Do not do it like this!
					IJavaScriptLineBreakpoint lineBp = (IJavaScriptLineBreakpoint) bp;
					try {
						if (lineBp.getMarker() != null && lineBp.getMarker().exists()) {
							int lineNo = lineBp.getLineNumber();
							String path = lineBp.getScriptPath();
							String expr = lineBp.getCondition();
							//jsonBp.put("file", 0); // TODO!
							//jsonBp.put("line", lineNo);
							//jsonBp.put("expr", expr);
							jsonBp.add(0);
							jsonBp.add(lineNo);
							jsonBps.add(jsonBp);
						}
					} catch (Exception e) {
						// TODO!
						e.printStackTrace();
					}
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

	public void startServer() throws Exception {
		if (server != null) {
			return;
		}
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
			CoreMoSyncPlugin.trace("Started live server at {0}:{1} ({2})", Util.toBase16(ipAddr), Integer.toString(getPort()), hostName);
		}
	}

	private synchronized void setMessageListener(int id, IMessageListener listener) {
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

	private String awaitEvalResult(int id, int timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
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

	public IStackFrame[] extractStackFrames(JSODDDebugTarget debugTarget, JSThread thread, JSONArray frames) {
		IStackFrame[] result = new IStackFrame[frames.size()];
		for (int i = 0; i < frames.size(); i++) {
			String name = (String) frames.get(i);
			result[i] = new JSStackFrame(debugTarget, thread, name, i);
		}
		return result;
	}

	public static IJavaScriptLineBreakpoint findBreakPoint(long fileId, long line) {
		IBreakpoint[] bps = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(JavaScriptDebugModel.MODEL_ID);
		// TODO: Optimize!?
		for (IBreakpoint bp : bps) {
			if (bp instanceof IJavaScriptLineBreakpoint) {
				IJavaScriptLineBreakpoint lineBp = (IJavaScriptLineBreakpoint) bp;
				IPath file = Html5Plugin.getDefault().getReloadManager().getFile(fileId);
				try {
					if (line == lineBp.getLineNumber() && Util.equals(file, new Path(lineBp.getScriptPath()))) {
						return lineBp;
					}
				} catch (CoreException e) {
					// Just IGNORE!
				}
			}
		}
		return null;
	}

	public void stopServer() throws Exception {
		server.stop();
		server = null;
	}

	public void setBreakpoint(IJavaScriptLineBreakpoint bp) {
		incomingQueue.offer(new DebuggerMessage(bp));
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

	public void resume() {
		//incomingQueue.offer(new DebuggerMessage(RESUME));
		debugQueue.offer(new DebuggerMessage(RESUME));
	}

	public String evaluate(String expression) throws InterruptedException, TimeoutException {
		DebuggerMessage queuedExpression = new DebuggerMessage(expression);
		debugQueue.offer(queuedExpression);
		//incomingQueue.offer(queuedExpression);
		String result = awaitEvalResult(queuedExpression.getMessageId(), 3, TimeUnit.SECONDS);
		return result;
	}

}
