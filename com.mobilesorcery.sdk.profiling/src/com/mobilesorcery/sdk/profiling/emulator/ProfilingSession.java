package com.mobilesorcery.sdk.profiling.emulator;

import java.io.File;
import java.util.Calendar;

import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.SLD;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.ILocationProvider;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.profiling.ProfilingPlugin;

public class ProfilingSession implements IProfilingSession {

    private IInvocation invocation = IInvocation.EMPTY;
    private IFilter<IInvocation> filter = ProfilingPlugin.getDefault().getDefaultFilter();
	private Calendar startTime;
	private String name;
	private SLD sld;
	private File profilingFile;
	private ILocationProvider locationProvider = new DefaultLocationProvider(null);

    public ProfilingSession(String name, Calendar startTime) {
        this.name = name;
        this.startTime = startTime;
    }
    
    public void setLocationProvider(ILocationProvider locationProvider) {
    	this.locationProvider  = locationProvider;
    }
    
    public ILocationProvider getLocationProvider() {
    	return locationProvider;
    }
    
    public Calendar getStartTime() {
    	return startTime;
    }
    
    public void setInvocation(IInvocation root) {
        this.invocation = root;
    }
    
    public IInvocation getInvocation() {
        return invocation;
    }

    public String getName() {
        return name;
    }
    
    public String toString() {
        return getName();
    }

    public IFilter<IInvocation> getFilter() {
        return filter;
    }

	public void setSLD(SLD sld) {
		this.sld = sld;
	}
	
	public SLD getSLD() {
		return sld;
	}

	public Object getAdapter(Class adapter) {
		if (SLD.class.equals(adapter)) {
			return getSLD();
		}
		return null;
	}

	public void setProfilingFile(File profilingFile) {
		this.profilingFile = profilingFile;
	}
	
	public File getProfilingFile() {
		return profilingFile;
	}
}
