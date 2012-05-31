package com.mobilesorcery.sdk.html5.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptDebugTarget;

import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadHandler extends AbstractHandler {

	public ReloadHandler() {
		//Html5Plugin.getDefault().getReloadServer().addSessionListener(this);
	}
	
	public void dispose() {
		
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ArrayList<ReloadVirtualMachine> vms = new ArrayList<ReloadVirtualMachine>();
		extractVMs(HandlerUtil.getCurrentSelection(event), vms);
		for (ReloadVirtualMachine vm : vms) {
			vm.reload();
		}
		return null;
	}
	
	public static boolean extractVMs(Object receiver, List<ReloadVirtualMachine> result) {
		if (receiver instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) receiver;
			List potentialVMs = selection.toList();
			for (Object potentialVM : potentialVMs) {
				extractVMs(potentialVM, result);
			}
			return !potentialVMs.isEmpty();
		}
		if (receiver instanceof ILaunch) {
			ILaunch launch = (ILaunch) receiver;
			receiver = launch.getDebugTarget();
		}
		if (receiver instanceof IThread) {
			IThread thread = (IThread) receiver;
			receiver = thread.getDebugTarget();
		}
		if (receiver instanceof IStackFrame) {
			IStackFrame stackFrame = (IStackFrame) receiver;
			stackFrame.getDebugTarget();
		}
		if (receiver instanceof IAdaptable) {
			IAdaptable adapt = (IAdaptable) receiver;
			receiver = adapt.getAdapter(IDebugTarget.class);
		}
		if (receiver instanceof IJavaScriptDebugTarget) {
			IJavaScriptDebugTarget debugTarget = (IJavaScriptDebugTarget) receiver;
			List<ReloadVirtualMachine> vms = Html5Plugin.getDefault().getReloadServer().getVMs(false);
			for (ReloadVirtualMachine vm : vms) {
				if (debugTarget == vm.getJavaScriptDebugTarget()) {
					if (result != null) {
						result.add(vm);
					}
					return true;
				}
			}
		}
		return false;
	}

}
