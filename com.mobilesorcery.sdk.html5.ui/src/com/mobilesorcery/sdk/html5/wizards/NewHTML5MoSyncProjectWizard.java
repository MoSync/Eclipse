/*  Copyright (C) 2011 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
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
