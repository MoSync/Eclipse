package com.mobilesorcery.sdk.ui.internal.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.ui.MoSyncCommandHandler;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.PlatformSelectionComposite;

public class ChangeActiveProfileHandler extends MoSyncCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		MoSyncProject project = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(window);
		if (!isValid(project)) {
			MessageDialog.openWarning(shell, "No project", "No valid project selected");
		} else {
			PlatformSelectionComposite psc = new PlatformSelectionComposite(shell, getStyle());
			psc.setProject(project);
			psc.show(SWT.NONE);
		}
		return null;
	}

	protected boolean isValid(MoSyncProject project) {
		return project != null;
	}

	protected int getMode() {
		return MoSyncTool.DEFAULT_PROFILE_TYPE;
	}

	protected int getStyle() {
		return SWT.SEARCH | SWT.BACKGROUND;
	}
}
