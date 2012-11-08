package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.wst.jsdt.debug.core.jsdi.StackFrame;
import org.eclipse.wst.jsdt.debug.core.jsdi.ThreadReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.core.jsdi.request.StepRequest;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadThreadReference implements ThreadReference {

	private final ReloadVirtualMachine vm;
	private final int status;
	private boolean isAtBreakpoint;
	private boolean suspended;
	private List frames = null;
	private int stepType;
	private String location;

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
		this.suspended = false;
		switch (stepType) {
		case ReloadStepRequest.NO_STEPPING:
			vm.resume();
			break;
		default:
			vm.step(stepType);
		}
	}

	@Override
	public void suspend() {
		if (isSuspended()) {
			return;
		}
		markSuspended(false);
		vm.suspend(true);
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
		String formattedLocation = location == null ? "" : " - " + new Path(location).lastSegment();
		return MessageFormat.format("main", formattedLocation);
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
}
