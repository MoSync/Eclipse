package com.mobilesorcery.sdk.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;

public abstract class BuildStepEditor extends Dialog implements IBuildStepEditor {

	protected MoSyncProject project;
	protected IBuildStepFactory factory;

	protected BuildStepEditor() {
		super(PlatformUI.getWorkbench().getModalDialogShellProvider());
	}

	public void setProject(MoSyncProject project) {
		this.project = project;
	}

	public void setBuildStepFactory(IBuildStepFactory factory) {
		this.factory = factory;
	}
	
	public int edit() {
		return open();
	}
	
	public boolean canEdit() {
		return true;
	}
}
