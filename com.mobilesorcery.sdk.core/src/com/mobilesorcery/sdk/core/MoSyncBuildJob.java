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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class MoSyncBuildJob extends Job {
    private MoSyncProject project;
	private IBuildSession session;
	private List<IBuildVariant> variants;

    public MoSyncBuildJob(MoSyncProject project, IBuildSession session) {
        super(MessageFormat.format("Building project {0} for {1} profiles", project.getName(), session.getBuilds().size()));
        init(project, session, session.getBuilds());
    }

    public MoSyncBuildJob(MoSyncProject project, IBuildSession session, IBuildVariant variant) {
        super(MessageFormat.format("Building project {0} for profile {1}", project.getName(), variant.getProfile()));
        init(project, session, Arrays.asList(variant));
    }

    private void init(MoSyncProject project, IBuildSession session,
			List<IBuildVariant> variants) {
        this.session = session;
        this.variants = variants;
        this.project = project;
        setUser(true);
        setRule(project.getWrappedProject().getWorkspace().getRoot());
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
        try {
    		MoSyncBuilder.createBuildJob(project.getWrappedProject(), session, variants).run(monitor);
        } catch (OperationCanceledException e) {
            return Status.CANCEL_STATUS;
        } catch (Exception e) {
            return new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not build project", e);
        }

        return Status.OK_STATUS;
    }
}