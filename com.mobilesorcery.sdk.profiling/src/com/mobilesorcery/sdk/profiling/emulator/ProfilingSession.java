package com.mobilesorcery.sdk.profiling.emulator;

import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.profiling.ProfilingPlugin;

public class ProfilingSession implements IProfilingSession {

    private IInvocation invocation = IInvocation.EMPTY;
    private ILaunchConfiguration launchConfiguration;
    private IFilter<IInvocation> filter = ProfilingPlugin.getDefault().getDefaultFilter();

    public ProfilingSession(ILaunchConfiguration launchConfiguration) {
        this.launchConfiguration = launchConfiguration;
    }
    
    public void setInvocation(IInvocation root) {
        this.invocation = root;
    }
    
    public IInvocation getInvocation() {
        return invocation;
    }

    public ILaunchConfiguration getLaunchConfiguration() {
        return launchConfiguration;
    }

    public String getName() {
        return launchConfiguration.getName();
    }
    
    public String toString() {
        return getName();
    }

    public IFilter<IInvocation> getFilter() {
        return filter;
    }
}
