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

import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.templates.IProjectTemplateExtensionDelegate;
import com.mobilesorcery.sdk.extensionsupport.ExtensionSupportPlugin;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;

public class ExtensionTemplateExtension implements IProjectTemplateExtensionDelegate {

	@Override
	public void configureProject(MoSyncProject project) throws CoreException {
		ExtensionSupportPlugin.getDefault().addExtensionBuildsteps(project);
		project.setProperty(MoSyncBuilder.PROJECT_TYPE, MoSyncBuilder.PROJECT_TYPE_EXTENSION);
		
		// Add extension filter
		DeviceCapabilitiesFilter oldFilter = DeviceCapabilitiesFilter
				.extractFilterFromProject(project);
		HashSet<String> newCapabilities = new HashSet<String>(
				oldFilter.getRequiredCapabilities());
		newCapabilities.add(ExtensionSupportPlugin.EXTENSIONS_CAPABILITY);
		DeviceCapabilitiesFilter newFilter = DeviceCapabilitiesFilter.create(
				newCapabilities.toArray(new String[0]), new String[0]);
		DeviceCapabilitiesFilter.setFilter(project, newFilter);
		
		// By default, we support all platforms
		IVendor[] platforms = MoSyncTool.getDefault().getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE).
				getVendors(DeviceCapabilitiesFilter.create(new String[] { ExtensionSupportPlugin.EXTENSIONS_CAPABILITY } ,new String[0]));
		String[] platformNames = new String[platforms.length];
		int i = 0;
		for (IVendor platform : platforms) {
			platformNames[i] = platform.getName();
			i++;
		}
		PropertyUtil.setStrings(project, ExtensionSupportPlugin.SUPPORTED_PLATFORMS_PROP, platformNames);
	}

}
