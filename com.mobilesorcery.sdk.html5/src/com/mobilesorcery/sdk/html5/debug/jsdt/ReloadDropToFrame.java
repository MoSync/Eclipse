package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IDropToFrameHandler;
import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.debug.core.model.IDropToFrame;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptStackFrame;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptThread;

public class ReloadDropToFrame implements IDropToFrame {

	private final IJavaScriptStackFrame frame;

	public ReloadDropToFrame(IJavaScriptStackFrame frame) {
		this.frame = frame;
	}

	@Override
	public boolean canDropToFrame() {
		return frame.isSuspended();
	}

	@Override
	public void dropToFrame() throws DebugException {
		System.err.println("AHA!");
	}

}
