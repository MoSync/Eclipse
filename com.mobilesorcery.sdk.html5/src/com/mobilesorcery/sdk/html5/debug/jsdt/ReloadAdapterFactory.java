package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.commands.IDropToFrameHandler;
import org.eclipse.debug.core.model.IDropToFrame;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.core.commands.DropToFrameCommand;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementEditor;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptDebugTarget;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptStackFrame;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptThread;
import org.eclipse.wst.jsdt.debug.internal.core.model.JavaScriptVariable;

public class ReloadAdapterFactory implements IAdapterFactory {

	public ReloadAdapterFactory() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (IDropToFrameHandler.class.equals(adapterType)) {
			return getHandler(adaptableObject);
		}
		if (IDropToFrame.class.equals(adapterType)) {
			return getDropToFrame(adaptableObject);
		}

		return null;
	}

	public Object getDropToFrame(Object adaptableObject) {
		if (adaptableObject instanceof IJavaScriptThread) {
			IJavaScriptThread thread = (IJavaScriptThread) adaptableObject;
			try {
				IStackFrame[] frames = thread.getStackFrames();
				if (frames.length > 0) {
					return new ReloadDropToFrame((IJavaScriptStackFrame) frames[0]);
				}
			} catch (Exception e) {
				// Ignore.
			}
		}
		if (adaptableObject instanceof IJavaScriptStackFrame) {
			IJavaScriptStackFrame frame = (IJavaScriptStackFrame) adaptableObject;
			return new ReloadDropToFrame(frame);
		}

		return null;
	}

	public Object getHandler(Object adaptableObject) {
		if (adaptableObject instanceof IJavaScriptThread || adaptableObject instanceof IJavaScriptStackFrame) {
			return new DropToFrameCommand();
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[] { IDropToFrameHandler.class };
	}

}
