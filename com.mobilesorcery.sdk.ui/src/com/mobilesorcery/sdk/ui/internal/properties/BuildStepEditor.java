package com.mobilesorcery.sdk.ui.internal.properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;

public abstract class BuildStepEditor extends Dialog {

	protected MoSyncProject project;
	protected IBuildStepFactory factory;

	protected BuildStepEditor(Shell parentShell) {
		super(parentShell);
	}

	public void setProject(MoSyncProject project) {
		this.project = project;
	}

	public void setBuildStepFactory(IBuildStepFactory factory) {
		this.factory = factory;
	}

}
