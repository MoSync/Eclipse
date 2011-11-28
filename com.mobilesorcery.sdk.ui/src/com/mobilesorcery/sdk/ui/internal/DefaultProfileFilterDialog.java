package com.mobilesorcery.sdk.ui.internal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.DefaultProfileFilterComposite;
import com.mobilesorcery.sdk.ui.UIUtils;

public class DefaultProfileFilterDialog extends Dialog {

	private MoSyncProject project;
	private DefaultProfileFilterComposite main;

	public DefaultProfileFilterDialog(Shell shell) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	public void setProject(MoSyncProject project) {
		this.project = project;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		main = new DefaultProfileFilterComposite(parent, SWT.NONE);
		main.setProject(project);
		main.setLayoutData(new GridData(GridData.FILL_BOTH));
		return main;
	}

	@Override
	public void configureShell(Shell newShell) {
		super.configureShell(newShell);
		Rectangle screenSize = Display.getCurrent().getPrimaryMonitor().getBounds();
		newShell.setSize(screenSize.width / 2, screenSize.height / 2);
		UIUtils.centerShell(newShell);
	}

	@Override
	public void okPressed() {
		main.updateProject();
		super.okPressed();
	}
}
