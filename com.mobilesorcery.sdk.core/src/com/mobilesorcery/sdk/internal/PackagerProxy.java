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
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class PackagerProxy implements IPackager {

    public final static String PATTERN = "pattern";
    public final static String CLASS = "implementation";
    
    private IConfigurationElement element;
    private IPackager delegate;
    
    public PackagerProxy(IConfigurationElement element) {
        this.element = element;
    }
    
    private void initDelegate() throws CoreException {
        if (delegate == null) {
            delegate = (IPackager) element.createExecutableExtension(CLASS);
        }
    }

    public void createPackage(MoSyncProject project, IBuildVariant variant, IBuildResult buildResult) throws CoreException {
        initDelegate();
        delegate.createPackage(project, variant, buildResult);
    }

	public void setParameter(String param, String value) throws CoreException {
		initDelegate();
		delegate.setParameter(param, value);
	}

}
