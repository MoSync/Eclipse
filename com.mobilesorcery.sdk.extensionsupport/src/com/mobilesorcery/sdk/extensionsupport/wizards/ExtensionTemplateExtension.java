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
package com.mobilesorcery.sdk.extensionsupport.wizards;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.templates.IProjectTemplateExtensionDelegate;
import com.mobilesorcery.sdk.extensionsupport.ExtensionSupportPlugin;

public class ExtensionTemplateExtension implements IProjectTemplateExtensionDelegate {

	@Override
	public void configureProject(MoSyncProject project) throws CoreException {
		ExtensionSupportPlugin.getDefault().addExtensionBuildsteps(project);
		project.setProperty(MoSyncBuilder.PROJECT_TYPE, MoSyncBuilder.PROJECT_TYPE_LIBRARY);
	}

}
