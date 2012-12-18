package com.mobilesorcery.sdk.ui;

import org.eclipse.jface.dialogs.Dialog;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;

public interface IBuildStepEditor {

	int OK = Dialog.OK;
	int CANCEL = Dialog.CANCEL;

	String EXTENSION_ID = "com.mobilesorcery.sdk.ui.buildstepeditor";

	void setProject(MoSyncProject project);
	
	void setBuildStepFactory(IBuildStepFactory factory);
	
	int edit();

	boolean canEdit();

}
