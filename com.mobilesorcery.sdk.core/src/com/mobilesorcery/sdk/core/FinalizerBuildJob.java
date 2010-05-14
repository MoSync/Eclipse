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
package com.mobilesorcery.sdk.core;

import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;


public class FinalizerBuildJob extends Job {
    private IBuildVariant variant;
    private MoSyncProject project;

    public FinalizerBuildJob(MoSyncProject project, IBuildVariant variant) {
        super(MessageFormat.format("Finalizing project {0} for profile {1}", project.getName(), variant.getProfile()));
        this.variant = variant;
        this.project = project;
        setUser(true);
    }

    protected IStatus run(IProgressMonitor monitor) {
        try {
            IBuildSession buildSession = MoSyncBuilder.createFinalizerBuildSession(Arrays.asList(variant));
            new MoSyncBuilder().fullBuild(project.getWrappedProject(), buildSession, variant, null, monitor);
        } catch (OperationCanceledException e) {
            return Status.CANCEL_STATUS;
        } catch (CoreException e) {
            return e.getStatus();
        }
        
        return Status.OK_STATUS;
    }        
}