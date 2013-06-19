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
package com.mobilesorcery.sdk.builder.app;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.finalizer.core.FinalizerParser;
import com.mobilesorcery.sdk.finalizer.ui.FinalizeJob;
import com.mobilesorcery.sdk.ui.ImportProjectsRunnable;

/* Rudimentary headless build
 */
public class HeadlessBuild implements IApplication {

	public class BuildRunnable implements Runnable {

		private final String[] args;
		private Integer result;
		private Exception exception;

		public BuildRunnable(String[] args) {
			this.args = args;
		}

		public Thread start() {
			Thread buildThread = new Thread(this, "Headless build thread"); //$NON-NLS-1$
			buildThread.start();
			return buildThread;
		}

		@Override
		public void run() {
			try {
				result = startBuild(args);
			} catch (Exception e) {
				exception = e;
			}
		}

		public Exception getException() {
			return exception;
		}

		public Integer getResult() {
			return result;
		}
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
		String osName = System.getProperty("os.name");
		boolean isMac = osName.startsWith("Mac") || osName.startsWith("Darwin");

		Display dummy = null;
		if (isMac) {
			// I know, it's quite ridiculus.
			dummy = PlatformUI.createDisplay();
			Bundle ideBundle = Platform.getBundle("org.eclipse.ui.ide");
			if (ideBundle != null) {
				ideBundle.start();
			}
		}

		// Make sure it's headless from the start.
		CoreMoSyncPlugin.setHeadless(true);

		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace(
					"*** Started headless build at {0,time}, {0,date}",
					new Date());
		}
		try {
			// OK, kill the splash
			context.applicationRunning();

			String[] args = (String[]) context.getArguments().get(
					IApplicationContext.APPLICATION_ARGS);

			BuildRunnable buildRunnable = new BuildRunnable(args);
			buildRunnable.start().join();
			if (buildRunnable.getException() != null) {
				System.err.println(buildRunnable.getException());
				throw buildRunnable.getException();
			}
		} finally {
			if (dummy != null) {
				dummy.dispose();
			}
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace(
						"*** Stopped headless build at {0,time}, {0,date}",
						new Date());
			}
		}

		return EXIT_OK;
	}

	Integer startBuild(String[] args) throws Exception {
		IWorkspace ws = ResourcesPlugin.getWorkspace();

		int i = 0;

		String projectName = null;
		String importProject = null;
		String finalizerScript = null;
		String cfgId = null;

		while (i < args.length) {
			String arg = args[i];
			String nextArg = i == args.length - 1 ? "" : args[i + 1]; //$NON-NLS-1$

			if ("-project".equals(arg)) { //$NON-NLS-1$
				projectName = nextArg;
				i++;
			} else if ("-import".equals(arg)) { 
				importProject = nextArg;
				i++;
			} else if ("-f".equals(arg)) { //$NON-NLS-1$
				finalizerScript = nextArg;
				i++;
			} else if ("-cfg".equals(arg)) { //$NON-NLS-1$
				cfgId = nextArg;
				i++;
			}
			i++;
		}

		if (projectName == null && importProject == null) {
			printUsage();
			throw new Exception(Messages.HeadlessBuild_Usage);
		}

		if (importProject != null) {
			IPath projectDescriptionFile = new Path(importProject).append(".project");
			IPath projectMetaDataFile = new Path(importProject).append(MoSyncProject.MOSYNC_PROJECT_META_DATA_FILENAME);
			
			if (projectName == null) {
				IProjectDescription desc = ResourcesPlugin.getWorkspace().loadProjectDescription(projectDescriptionFile);
				projectName = desc.getName();
			}		
			ImportProjectsRunnable importer = new ImportProjectsRunnable(new File[] {
					projectMetaDataFile.toFile() }, new String[] { projectName }, ImportProjectsRunnable.DO_NOT_COPY);
			importer.createJob(true).join();
		}
		
		IProject project = ws.getRoot().getProject(projectName);
		if (!project.exists() || !project.isOpen()) {
			throw new IOException(Messages.HeadlessBuild_IllegalProjectState);
		}

		MoSyncProject mosyncProject = MoSyncProject.create(project);

		if (cfgId != null && mosyncProject.getBuildConfiguration(cfgId) == null) {
			String availableCfgs = Util.join(mosyncProject
					.getBuildConfigurations().toArray(), ", ");
			throw new IOException(
					MessageFormat
							.format("Unknown build configuration: {0}. Available configurations for this project are: {1}",
									cfgId, availableCfgs));
		}

		String script = null;
		if (finalizerScript != null) {
			script = Util.readFile(finalizerScript);
		}

		FinalizerParser parser = new FinalizerParser(project);
		if (script == null) {
			script = parser.extractScriptFromProject(mosyncProject);
		}

		FinalizeJob job = new FinalizeJob(mosyncProject, script, cfgId);
		job.setUser(true);
		job.setRule(mosyncProject.getWrappedProject().getWorkspace().getRuleFactory().buildRule());
		job.schedule();
		job.join();

		IStatus result = job.getResult();

		if (result.getSeverity() != IStatus.OK) {
			throw new CoreException(result);
		}

		return EXIT_OK;
	}

	private void printUsage() {
		System.err.println(Messages.HeadlessBuild_Usage);
	}

	@Override
	public void stop() {
	}

}
