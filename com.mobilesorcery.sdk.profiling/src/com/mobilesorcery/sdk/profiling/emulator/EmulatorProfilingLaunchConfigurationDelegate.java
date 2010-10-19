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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.SLD;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.ProfilingPlugin;
import com.mobilesorcery.sdk.profiling.IProfilingListener.ProfilingEventType;
import com.mobilesorcery.sdk.profiling.internal.ProfilingDataParser;

public class EmulatorProfilingLaunchConfigurationDelegate extends EmulatorLaunchConfigurationDelegate {

    @Override
    public void launchSync(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor)
    throws CoreException {
        ProfilingSession session = new ProfilingSession(launchConfig.getName(), Calendar.getInstance());
        ProfilingPlugin.getDefault().notifyProfilingListeners(ProfilingEventType.STARTED, session);
        super.launchSync(launchConfig, mode, launch, emulatorId, monitor);
        MoSyncProject mosyncProject = MoSyncProject.create(getProject(launchConfig));
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
    
}
