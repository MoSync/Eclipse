/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.IPackagerDelegate;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;

public class PackagerProxy implements IPackager {

	public final static String RUNTIME_PATTERN = "pattern";
	public final static String PLATFORM_PATTERN = "platformPattern";
    public final static String CLASS = "implementation";

    private final IConfigurationElement element;
    private IPackagerDelegate delegate;
	private final String id;
	private final String platform;

    public PackagerProxy(IConfigurationElement element) {
        this.element = element;
        this.id = element.getAttribute("id");
        this.platform = element.getAttribute("platform");
    }

    private void initDelegate() throws CoreException {
        if (delegate == null) {
            delegate = (IPackagerDelegate) element.createExecutableExtension(CLASS);
        }
    }

    @Override
	public void createPackage(MoSyncProject project, IBuildSession session, IBuildVariant variant, IFileTreeDiff diff, IBuildResult buildResult) throws CoreException {
        initDelegate();
        delegate.createPackage(project, session, variant, diff, buildResult);
    }

	@Override
	public String getGenerateMode(IProfile profile) throws CoreException {
		initDelegate();
		return delegate.getGenerateMode(profile);
	}

	@Override
	public String getShortDescription(MoSyncProject project, IProfile profile) {
		try {
			initDelegate();
			return delegate.getShortDescription(project, profile);
		} catch (CoreException e) {
			return profile.getName();
		}
	}

	@Override
	public void buildNative(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IBuildResult result) throws Exception {
		initDelegate();
		delegate.buildNative(project, session, variant, result);
	}
	
	@Override
	public String getOutputType(MoSyncProject project) {
		try {
			initDelegate();
			return delegate.getOutputType(project);
		} catch (CoreException e) {
			return MoSyncBuilder.OUTPUT_TYPE_INTERPRETED;
		}
		
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getPlatform() {
		return platform;
	}


}
