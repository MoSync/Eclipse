package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.wst.jsdt.debug.core.jsdi.StackFrame;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.StepRequest;

import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.debug.jsdt.requests.ReloadStepRequest;
import com.mobilesorcery.sdk.html5.live.JSODDServer;

public class ReloadThreadReference implements ThreadReference {

	private final ReloadVirtualMachine vm;
	private final int status;
	private boolean isAtBreakpoint;
	private boolean suspended;
	private List frames = null;
	private int stepType;
	private String location;
	private int sessionId;
	private JSODDServer server;

	public ReloadThreadReference(ReloadVirtualMachine vm) {
		this.vm = vm;
		this.status = THREAD_STATUS_RUNNING;
		this.server = Html5Plugin.getDefault().getReloadServer();
	}

	@Override
	public VirtualMachine virtualMachine() {
		return vm;
	}

	@Override
	public int frameCount() {
		return frames().size();
	}

	@Override
	public StackFrame frame(int index) {
		return (StackFrame) frames().get(index);
	}

	@Override
	public List frames() {
		if (!isSuspended() || status == THREAD_STATUS_ZOMBIE) {
			return Collections.EMPTY_LIST;
		}
		if (frames != null) {
			return frames;
		}

		return Collections.EMPTY_LIST;
	}

	@Override
	public void interrupt() {
		// IMPL
	}

	@Override
	public void resume() {
		if (!isSuspended()) {
			return;
		}
		markSuspended(false,  false);
		switch (stepType) {
		case ReloadStepRequest.NO_STEPPING:
			server.resume(sessionId);
			break;
		default:
			server.step(sessionId, stepType);
		}
	}

	@Override
	public void suspend() {
		if (isSuspended()) {
			return;
		}
		markSuspended(false);
		server.suspend(sessionId);
	}

	@Override
	public int status() {
		return status;
	}

	@Override
	public boolean isAtBreakpoint() {
		return isAtBreakpoint;
	}

	@Override
	public boolean isSuspended() {
		return suspended;
	}

	@Override
	public String name() {
		String formattedLocation = location == null ? "" : new Path(location).lastSegment();
		return formattedLocation;
	}

	public void terminate() {
		server.terminate(sessionId, vm.getMainThread() == this);
	}
	
	public void markSuspended(boolean isAtBreakpoint) {
		markSuspended(true, isAtBreakpoint);
	}
	
	public void markSuspended(boolean suspended, boolean isAtBreakpoint) {
		this.isAtBreakpoint = isAtBreakpoint;
		this.suspended = suspended;
	}

	public void setFrames(StackFrame[] frames) {
		this.frames = Arrays.asList(frames);
	}

	public void setStepType(int stepType) {
		this.stepType = stepType;
	}

	public void setCurrentLocation(String location) {
		this.location = location;
	}

	public int getSessionId() {
		return sessionId;
	}
	
	public int setSessionId(int newSessionId) {
		int previousSessionId = this.sessionId;
		this.sessionId = newSessionId;
		return previousSessionId;
	}
	
	/**
	 * Evaluates an expression in the current scope.
	 * 
	 * @param expression
	 *            The JavaScript expression to evaluate
	 * @return The result of the evaluation
	 * @throws InterruptedException
	 *             If the waiting thread was interrupted, for example by a
	 *             terminate request.
	 * @throws TimeoutException
	 *             If the client failed to respond within a specified timeout.
	 */
	public Object evaluate(String expression) throws InterruptedException,
			TimeoutException {
		return evaluate(expression, null);
	}

	/**
	 * Evaluates an expression at a specified stack depth
	 * 
	 * @param expression
	 *            The JavaScript expression to evaluate
	 * @param stackDepth
	 *            The stackdepth to perform the evaluation, or {@code null} to
	 *            use the current scope.
	 * @return The result of the evaluation
	 * @throws InterruptedException
	 *             If the waiting thread was interrupted, for example by a
	 *             terminate request.
	 * @throws TimeoutException
	 *             If the client failed to respond within a specified timeout.
	 */
	public Object evaluate(String expression, Integer stackDepth)
			throws InterruptedException, TimeoutException {
		return server.evaluate(sessionId, expression, stackDepth);
	}

	public String toString() {
		return name() + " #" + sessionId;
	}

}
