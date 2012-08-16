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


	public void startServer() throws Exception {
		throw new Exception("3.1.1 does not support this");
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
