package com.mobilesorcery.sdk.html5.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.INewWizard;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.wizards.internal.NewMoSyncProjectWizard;

public class NewHTML5MoSyncProjectWizard extends NewMoSyncProjectWizard implements INewWizard {

	@Override
	public void configureProject(MoSyncProject project) throws CoreException {
		Html5Plugin.getDefault().addHTML5Support(project);
	}

	@Override
	protected boolean isSupported(String templateType) {
		return Util.equals(Html5Plugin.HTML5_TEMPLATE_TYPE, templateType);
	}

}
