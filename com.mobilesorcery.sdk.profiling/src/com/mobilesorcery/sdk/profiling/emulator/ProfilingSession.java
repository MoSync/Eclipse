package com.mobilesorcery.sdk.profiling.emulator;

import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.IProfilingSession;

public class ProfilingSession implements IProfilingSession {

    private IInvocation invocation = IInvocation.EMPTY;
    private ILaunchConfiguration launchConfiguration;

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

}
