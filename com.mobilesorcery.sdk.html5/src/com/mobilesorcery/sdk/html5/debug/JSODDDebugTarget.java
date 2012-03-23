package com.mobilesorcery.sdk.html5.debug;

import java.util.ArrayList;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;
import org.json.simple.JSONObject;

import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadProcess;

public class JSODDDebugTarget extends DebugElement implements IDebugTarget, IDebugEventSetListener {

	private static JSODDDebugTarget last;
	private final ILaunch launch;
	private final ReloadProcess process;
	private final ArrayList<IThread> threads = new ArrayList<IThread>();

	private boolean suspended;

	public JSODDDebugTarget(ILaunch launch, ReloadProcess process) {
		super(null);
		this.launch = launch;
		this.process = process;
		// MOVE
		DebugPlugin dp = DebugPlugin.getDefault();
		dp.getBreakpointManager().addBreakpointListener(this);
		dp.addDebugEventListener(this);
		threads.add(new JSThread(this));
		last = this;
	}

	/**
	 * @deprecated Just for now
	 * @return
	 */
	@Deprecated
	public static JSODDDebugTarget getLast() {
		// TODO: MOVEMOVEMOVE!!!
		return last;
	}

	@Override
	public String getModelIdentifier() {
		return JavaScriptDebugModel.MODEL_ID;
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return this;
	}

	@Override
	public ILaunch getLaunch() {
		return launch;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == JSODDDebugTarget.class) {
			return this;
		}
		return super.getAdapter(adapter);
	}

	@Override
	public boolean canTerminate() {
		return process.canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return process.isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		// MOVE
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		DebugPlugin.getDefault().removeDebugEventListener(this);

		process.terminate();
	}

	@Override
	public boolean canResume() {
		return isSuspended() && !isTerminated();
	}

	@Override
	public boolean canSuspend() {
		return !isSuspended() && !isTerminated();
	}

	@Override
	public boolean isSuspended() {
		return suspended;
	}

	@Override
	public void resume() throws DebugException {
		if (canResume()) {
			suspended = false;
			throw new UnsupportedOperationException("TODO!");
			//process.getReloadServer().resume();
		}
	}

	@Override
	public void suspend() {
		if (canSuspend()) {
			this.suspended = true;
		}
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		if (breakpoint instanceof IJavaScriptLineBreakpoint) {
			process.getReloadServer().setBreakpoint((IJavaScriptLineBreakpoint) breakpoint);
		}
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (breakpoint instanceof IJavaScriptLineBreakpoint) {
			process.getReloadServer().removeBreakpoint((IJavaScriptLineBreakpoint) breakpoint);
		}
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		//
	}

	@Override
	public boolean canDisconnect() {
		return canTerminate();
	}

	@Override
	public void disconnect() throws DebugException {
		terminate();
	}

	@Override
	public boolean isDisconnected() {
		return isTerminated();
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return false;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length)
			throws DebugException {
		notSupported("Memory block retrieval not supported", null);
		return null;
	}

	@Override
	public IProcess getProcess() {
		return process;
	}

	@Override
	public IThread[] getThreads() throws DebugException {
		return threads.toArray(new IThread[threads.size()]);
	}

	@Override
	public boolean hasThreads() throws DebugException {
		return !threads.isEmpty();
	}

	@Override
	public String getName() throws DebugException {
		return "TODO";
	}

	@Override
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		return breakpoint instanceof IJavaScriptLineBreakpoint;
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			switch (event.getKind()) {
			case DebugEvent.SUSPEND:
				if (event.getSource() instanceof JSThread) {
					Pair<IBreakpoint, IStackFrame[]> state = (Pair<IBreakpoint, IStackFrame[]>) event.getData();
					((JSThread) event.getSource()).setBreakpoint(state.first);
					((JSThread) event.getSource()).setStackFrames(state.second);
				}
			}
		}
	}

}
