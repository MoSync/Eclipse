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

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.finalizer.core.FinalizerParser;
import com.mobilesorcery.sdk.finalizer.ui.FinalizeJob;

/* Rudimentary headless build
 */
public class HeadlessBuild implements IApplication {

	public class BuildRunnable implements Runnable {

		private String[] args;
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

	public Object start(IApplicationContext context) throws Exception {
		// OK, kill the splash
		context.applicationRunning();
		
		CoreMoSyncPlugin.setHeadless(true);
		
		String[] args = (String[]) context.getArguments().get(
				IApplicationContext.APPLICATION_ARGS);

		BuildRunnable buildRunnable = new BuildRunnable(args);
		buildRunnable.start().join();

		if (buildRunnable.getException() != null) {
			System.err.println(buildRunnable.getException());
		}

		return EXIT_OK;
	}
	
	Integer startBuild(String[] args) throws Exception {
		IWorkspace ws = ResourcesPlugin.getWorkspace();

		int i = 0;

		String projectName = null;
		String finalizerScript = null;

		while (i < args.length) {
			String arg = args[i];
			String nextArg = i == args.length - 1 ? "" : args[i + 1]; //$NON-NLS-1$

			if ("-project".equals(arg)) { //$NON-NLS-1$
				projectName = nextArg;
				i += 2;
			} else if ("-f".equals(arg)) { //$NON-NLS-1$
				finalizerScript = nextArg;
				i += 2;
			}
		}

		if (projectName == null) {
			printUsage();
			return EXIT_OK;
		}

		IProject project = ws.getRoot().getProject(projectName);
		if (!project.exists() || !project.isOpen()) {
			throw new IOException(Messages.HeadlessBuild_IllegalProjectState);
		}

		String script = null;
		if (finalizerScript != null) {
			script = Util.readFile(finalizerScript);
		}

		FinalizerParser parser = new FinalizerParser(project);
		if (script == null) {
			script = parser.extractScriptFromProject(MoSyncProject
					.create(project));
		}

		FinalizeJob job = new FinalizeJob(MoSyncProject.create(project), script);
		job.setUser(true);
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

	public void stop() {
	}

}
