package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.wst.jsdt.debug.core.jsdi.StackFrame;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

public class ReloadThreadReference implements ThreadReference {

	private final ReloadVirtualMachine vm;
	private final int status;
	private boolean isAtBreakpoint;
	private boolean suspended;
	private List frames = null;
	private int stepType;

	public ReloadThreadReference(ReloadVirtualMachine vm) {
		this.vm = vm;
		this.status = THREAD_STATUS_RUNNING;
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
		// SEND CONTINUE!
		vm.resume();
		this.suspended = false;
	}

	@Override
	public void suspend() {
		this.suspended = true;
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
		return "main";
	}

	public void suspend(boolean isAtBreakpoint) {
		this.isAtBreakpoint = isAtBreakpoint;
		suspend();
	}

	public void setFrames(StackFrame[] frames) {
		System.err.println("GOT STACK FRAMES!" + Arrays.asList(frames));
		this.frames = Arrays.asList(frames);
	}

	public void setStepType(int stepType) {
		this.stepType = stepType;
	}
}
