package com.mobilesorcery.sdk.html5.live;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;

import com.mobilesorcery.sdk.html5.debug.JSThread;
import com.mobilesorcery.sdk.html5.debug.JSODDDebugTarget;

public class JSStackFrame implements IStackFrame {

	private final JSODDDebugTarget debugTarget;
	private final JSThread thread;
	private final String name;

	public JSStackFrame(JSODDDebugTarget debugTarget, JSThread thread, String name, int i) {
		this.debugTarget = debugTarget;
		this.thread = thread;
		this.name = name;
	}

	@Override
	public String getModelIdentifier() {
		return JavaScriptDebugModel.MODEL_ID;
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return debugTarget;
	}

	@Override
	public ILaunch getLaunch() {
		return debugTarget.getLaunch();
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IStackFrame.class) {
			return this;
		} else if (adapter == IDebugTarget.class) {
			return debugTarget;
		} else if (adapter == ILaunch.class) {
			return getLaunch();
		}
		return null;
	}

	@Override
	public boolean canStepInto() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canStepOver() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canStepReturn() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStepping() {
		// TODO Auto-generated method stub
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
	public boolean canResume() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canSuspend() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSuspended() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void resume() throws DebugException {
		debugTarget.resume();
	}

	@Override
	public void suspend() throws DebugException {
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
		// Not supported
	}

	@Override
	public IThread getThread() {
		return thread;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return null;
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return getVariables().length > 0;
	}

	@Override
	public int getLineNumber() throws DebugException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCharStart() throws DebugException {
		return -1;
	}

	@Override
	public int getCharEnd() throws DebugException {
		return -1;
	}

	@Override
	public String getName() throws DebugException {
		return name;
	}

	@Override
	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return null;
	}

	@Override
	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

}
