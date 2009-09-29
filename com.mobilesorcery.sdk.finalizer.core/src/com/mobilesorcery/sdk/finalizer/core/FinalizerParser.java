package com.mobilesorcery.sdk.finalizer.core;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
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
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;

public class FinalizerParser {

	public class ParseException extends Exception {	
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
		ArrayList<IRunnableWithProgress> buildJobs = new ArrayList<IRunnableWithProgress>();

		int lineNo = 1;
		LineNumberReader lines = new LineNumberReader(script);
		for (String line = lines.readLine(); line != null; line = lines.readLine()) {
			IRunnableWithProgress buildJob = parse(line, lineNo);
			if (buildJob != null) {
				buildJobs.add(buildJob);
			}
			lineNo++;
		}

		monitor.beginTask(Messages.FinalizerParser_FinalizingProgress, buildJobs.size());

		if (buildJobs.isEmpty()) {
			throw new ParseException(Messages.FinalizerParser_ParseError_0, 0);
		}

		for (IRunnableWithProgress buildJob : buildJobs) {
			SubProgressMonitor jobMonitor = new SubProgressMonitor(monitor, 1);
			if (!monitor.isCanceled()) {
				buildJob.run(jobMonitor);
			}
		}
	}

	private IRunnableWithProgress parse(String line, int lineNo) throws ParseException {
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

			return createBuildJob(project, profile);
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

	private IRunnableWithProgress createBuildJob(final IProject project, final IProfile targetProfile) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					IBuildResult buildResult = new MoSyncBuilder().fullBuild(project, targetProfile, true, true, monitor);
					if (!buildResult.success()) {
						throw new InvocationTargetException(new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, Messages.FinalizerParser_BuildFailed)));
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e, targetProfile + ": " + e.getMessage()); //$NON-NLS-1$
				}
			}
		};
	}

}
