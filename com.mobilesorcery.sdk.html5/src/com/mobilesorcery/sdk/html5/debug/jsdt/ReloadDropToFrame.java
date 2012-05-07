package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDropToFrame;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptStackFrame;

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
		int stackDepth = getStackDepth();
		// Reverse numbering here!
		int frameToDropTo = frame.getThread().getStackFrames().length - stackDepth - 1;
		// HACK: We use 'evaluate' to pass on our own twisted expressions.
		frame.evaluate(ReloadStackFrame.createDropToFrameExpression(frameToDropTo, null));
		frame.stepInto();
	}

	private int getStackDepth() {
		IThread thread = frame.getThread();
		IStackFrame[] allFrames = new IStackFrame[0];
		try {
			allFrames = thread.getStackFrames();
		} catch (DebugException e) {
			// Ignore!?
			e.printStackTrace();
		}
		for (int i = 0; i < allFrames.length; i++) {
			if (allFrames[i] == frame) {
				return i;
			}
		}
		return 0;
	}

}
