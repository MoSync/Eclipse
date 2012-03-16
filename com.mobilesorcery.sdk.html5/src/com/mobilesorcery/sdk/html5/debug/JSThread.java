package com.mobilesorcery.sdk.html5.debug;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;
import org.eclipse.wst.jsdt.debug.internal.core.JavaScriptDebugPlugin;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

public class JSThread extends DebugElement implements IThread {

	private boolean suspended;
	private IBreakpoint[] breakpoints;
	private IStackFrame[] frames;

	public JSThread(JSODDDebugTarget target) {
		super(target);
	}

	@Override
	public String getModelIdentifier() {
		return JavaScriptDebugModel.MODEL_ID;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (IThread.class == adapter) {
			return this;
		}
		if (IStackFrame.class == adapter) {
			try {
				return getTopStackFrame();
			} catch (DebugException e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
		}
		return super.getAdapter(adapter);
	}

	@Override
	public boolean canResume() {
		return !isTerminated() && isSuspended();
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
		suspended = false;
	}

	@Override
	public void suspend() throws DebugException {
		suspended = true;
	}

	public void setBreakpoint(IBreakpoint breakpoint) {
		this.breakpoints = new IBreakpoint[] { breakpoint };
	}

	public void setStackFrames(IStackFrame[] frames) {
		this.frames = frames;
	}

	@Override
	public boolean canStepInto() {
		return false;
	}

	@Override
	public boolean canStepOver() {
		return false;
	}

	@Override
	public boolean canStepReturn() {
		return false;
	}

	@Override
	public boolean isStepping() {
		return false;
	}

	@Override
	public void stepInto() throws DebugException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stepOver() throws DebugException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stepReturn() throws DebugException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean canTerminate() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

	@Override
	public void terminate() throws DebugException {

	}

	@Override
	public IStackFrame[] getStackFrames() throws DebugException {
		initializeFrames();
		return frames;
	}

	private void initializeFrames() {
	}

	@Override
	public boolean hasStackFrames() throws DebugException {
		return getStackFrames().length > 0;
	}

	@Override
	public int getPriority() throws DebugException {
		return 0;
	}

	@Override
	public IStackFrame getTopStackFrame() throws DebugException {
		IStackFrame[] frames = getStackFrames();
		return frames.length == 0 ? null : frames[0];
	}

	@Override
	public String getName() throws DebugException {
		return "main";
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		return breakpoints;
	}

}
