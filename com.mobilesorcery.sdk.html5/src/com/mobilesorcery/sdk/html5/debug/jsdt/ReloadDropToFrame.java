package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDropToFrame;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
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
		dropToFrame(frame);
	}
	
	public static void dropToFrame(IThread thread, int frameToDropTo) throws DebugException {
		if (thread instanceof IJavaScriptThread) {
			// Reverse numbering here!
			IJavaScriptStackFrame frame = (IJavaScriptStackFrame) thread.getStackFrames()[frameToDropTo];
			// HACK: We use 'evaluate' to pass on our own twisted expressions.
			frame.evaluate(ReloadStackFrame.createDropToFrameExpression(frameToDropTo, null));
			frame.stepInto();
		}
	}
	
	public static void dropToFrame(IJavaScriptStackFrame frame) throws DebugException {
		int stackDepth = getStackDepth(frame);
		int frameToDropTo = frame.getThread().getStackFrames().length - stackDepth - 1;
		dropToFrame(frame.getThread(), frameToDropTo);
	}

	private static int getStackDepth(IJavaScriptStackFrame frame) {
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
