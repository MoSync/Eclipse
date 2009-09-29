package com.mobilesorcery.sdk.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;

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

    public void createPackage(MoSyncProject project, IProfile targetProfile, IBuildResult buildResult) throws CoreException {
        initDelegate();
        delegate.createPackage(project, targetProfile, buildResult);
    }

	public void setParameter(String param, String value) throws CoreException {
		initDelegate();
		delegate.setParameter(param, value);
	}

}
