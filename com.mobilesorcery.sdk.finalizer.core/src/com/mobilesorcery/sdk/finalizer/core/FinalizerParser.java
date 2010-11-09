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
package com.mobilesorcery.sdk.finalizer.core;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;

public class FinalizerParser {

	public static final String FINALIZER_PROPERTY_PREFIX = "finalizer";
	
	public static final String AUTO_CHANGE_CONFIG = FINALIZER_PROPERTY_PREFIX + ":auto.change.config";
	
	public static final String BUILD_CONFIG = FINALIZER_PROPERTY_PREFIX + ":build.config";
	
	public class ParseException extends Exception {	

		private static final long serialVersionUID = -9055537164752470136L;

		public ParseException(String msg, int line) {
			super("Line " + line + ": " + msg); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private static final String PATTERN_STR = "begin (.*)/(.*) end"; //$NON-NLS-1$

	final static Pattern finalizerPattern = Pattern.compile(PATTERN_STR);

	private IProject project;

	public FinalizerParser(IProject project) {
		this.project = project;
	}

	public void execute(Reader script, IProgressMonitor monitor) throws IOException, ParseException, InvocationTargetException,
			InterruptedException {
	    ArrayList<IBuildVariant> variantsToBuild = new ArrayList<IBuildVariant>();

	    MoSyncProject project = MoSyncProject.create(this.project);
	    //autoSwitchConfiguration(project);
	        
		int lineNo = 1;
		LineNumberReader lines = new LineNumberReader(script);
		for (String line = lines.readLine(); line != null; line = lines.readLine()) {
			IProfile profileToBuild = parse(line, lineNo);
			if (profileToBuild != null) {
			    IBuildVariant variant = MoSyncBuilder.getFinalizerVariant(project, profileToBuild);
			    variantsToBuild.add(variant);
			}
			lineNo++;
		}

		monitor.beginTask(Messages.FinalizerParser_FinalizingProgress, variantsToBuild.size());

		if (variantsToBuild.isEmpty()) {
			throw new ParseException(Messages.FinalizerParser_ParseError_0, 0);
		}

        IBuildSession buildSession = MoSyncBuilder.createFinalizerBuildSession(variantsToBuild);
        
		for (IBuildVariant variantToBuild : variantsToBuild) {
			SubProgressMonitor jobMonitor = new SubProgressMonitor(monitor, 1);
			if (!monitor.isCanceled()) {
                IRunnableWithProgress buildJob = createBuildJob(project.getWrappedProject(), buildSession, variantToBuild);
				buildJob.run(jobMonitor);
			}
		}
	}

	private IProfile parse(String line, int lineNo) throws ParseException {
		if (line.trim().length() == 0 || line.trim().startsWith("#")) { //$NON-NLS-1$
			return null;
		}

		Matcher m = finalizerPattern.matcher(line);
		int groups = m.groupCount();
		if (m.matches() && groups == 2) {
			String vendorName = m.group(1).trim();
			String profileName = m.group(2).trim();

			IVendor vendor = MoSyncTool.getDefault().getVendor(vendorName);
			if (vendor == null) {
				throw new ParseException(MessageFormat.format(Messages.FinalizerParser_ParseError_UnknownVender, vendorName), lineNo);
			}

			IProfile profile = vendor.getProfile(profileName);
			if (profile == null) {
				throw new ParseException(MessageFormat.format(Messages.FinalizerParser_ParseError_UnknownProfile, profileName), lineNo);
			}

			return profile;
		} else {
			throw new ParseException(Messages.FinalizerParser_ParseError_3, lineNo);
		}
	}

	public String extractScriptFromProject(MoSyncProject project) {
		StringBuffer result = new StringBuffer();
		result.append(MessageFormat.format(Messages.FinalizerParser_ScriptBoilerplate, project.getName()));

		IProfile[] profiles = project.getFilteredProfiles();
		if (profiles.length == 0) {
			result.append(Messages.FinalizerParser_ScriptNoProfiles);
		}

		for (int i = 0; i < profiles.length; i++) {
			result.append("begin " + profiles[i].toString() + " end\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result.toString();
	}

	private IRunnableWithProgress createBuildJob(final IProject project, final IBuildSession session, final IBuildVariant variant) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					IBuildResult buildResult = new MoSyncBuilder().fullBuild(project, session, variant, null, monitor);
					if (!buildResult.success()) {
						throw new InvocationTargetException(new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, Messages.FinalizerParser_BuildFailed)));
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e, variant.getProfile() + ": " + e.getMessage()); //$NON-NLS-1$
				} finally {
					monitor.done();
				}
			}
		};
	}

	/**
	 * If applicable, changes the active configuration of the project
	 * to whatever is set in the project's "Finalizer" properties page.
	 * @param project
	 */
    /*public static void autoSwitchConfiguration(MoSyncProject project) {
        if (project != null && PropertyUtil.getBoolean(project, AUTO_CHANGE_CONFIG)) {
            String buildConfiguration = project.getProperty(BUILD_CONFIG);
            if (project.areBuildConfigurationsSupported()) {
                project.setActiveBuildConfiguration(buildConfiguration);
            }
        }
    }*/

}
