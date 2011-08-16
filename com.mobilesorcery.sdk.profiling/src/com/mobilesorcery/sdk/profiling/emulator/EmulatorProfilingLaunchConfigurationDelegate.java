/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.profiling.emulator;

import java.util.Calendar;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.MergeFilter;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.ParseException;
import com.mobilesorcery.sdk.core.ReverseFilter;
import com.mobilesorcery.sdk.core.SLD;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.ProfilingPlugin;
import com.mobilesorcery.sdk.profiling.IProfilingListener.ProfilingEventType;
import com.mobilesorcery.sdk.profiling.filter.NameFilter;
import com.mobilesorcery.sdk.profiling.filter.NameFilter.MatchType;
import com.mobilesorcery.sdk.profiling.internal.ProfilingDataParser;

public class EmulatorProfilingLaunchConfigurationDelegate extends EmulatorLaunchConfigurationDelegate {

	public static final String FD_FILTER = "func.filter";
	public static final String FILE_FILTER = "file.filter";
	public static final String USE_REG_EXP = "filter.reg.exp";

    @Override
    public void launchSync(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor)
    throws CoreException {
    	IProject project = getProject(launchConfig);
        ProfilingSession session;
		try {
			session = createProfilingSession(launchConfig);
		} catch (ParseException e) {
			throw new CoreException(new Status(IStatus.ERROR, ProfilingPlugin.PLUGIN_ID, "Invalid filter: " + e.getMessage()));
		}
        session.setLocationProvider(new DefaultLocationProvider(project));
        ProfilingPlugin.getDefault().notifyProfilingListeners(ProfilingEventType.STARTED, session);
        super.launchSync(launchConfig, mode, launch, emulatorId, monitor);
        MoSyncProject mosyncProject = MoSyncProject.create(project);
        // At this point, we parse profiling data only post-mortem.
        IBuildVariant variant = getVariant(launchConfig, mode);
        IPath launchDir = getLaunchDir(mosyncProject, variant);
        IBuildConfiguration buildConfiguration = mosyncProject.getBuildConfiguration(variant.getConfigurationId());
        SLD sld = mosyncProject.getSLD(buildConfiguration);
        
        IPath profilingFile = launchDir.append("fp.xml");
        ProfilingDataParser parser = new ProfilingDataParser();
        try {
            IInvocation profilingResult = parser.parse(profilingFile.toFile(), sld);
            session.setInvocation(profilingResult);
            session.setSLD(sld);
            session.setProfilingFile(profilingFile.toFile());
            ProfilingPlugin.getDefault().notifyProfilingListeners(ProfilingEventType.STOPPED, session);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, ProfilingPlugin.PLUGIN_ID, "Could not parse profiling data.", e));
        }
    }

	private ProfilingSession createProfilingSession(ILaunchConfiguration config) throws CoreException, ParseException {
		ProfilingSession session = new ProfilingSession(config.getName(), Calendar.getInstance());
		session.setFilter(createProfilingFilters(config));
		return session;
	}
	
	public static IFilter<IInvocation> createProfilingFilters(ILaunchConfiguration config) throws ParseException, CoreException {
		MatchType matchType = config.getAttribute(USE_REG_EXP, false) ? MatchType.REGEXP : MatchType.CONTAINS;
		IFilter<IInvocation> funcFilter = NameFilter.create(config.getAttribute(FD_FILTER, ""), NameFilter.Criteria.NAME, matchType, true);
		IFilter<IInvocation> fileFilter = NameFilter.create(config.getAttribute(FILE_FILTER, ""), NameFilter.Criteria.FILE, matchType, true);
		return new MergeFilter<IInvocation>(MergeFilter.AND, funcFilter, fileFilter);
	}
    
	@Override
	public boolean allowsExternalEmulators() {
		return false;
	}
	
}
