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
package com.mobilesorcery.sdk.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * A 'default' packager, with an appropriate set of convenience methods.
 * 
 * @author Mattias Bybro, mattias@bybro.com/mattias.bybro@purplescout.se
 * 
 */
public class DefaultPackager {

    public final static String MOSYNC_HOME = "mosync-home";
    public final static String MOSYNC_BIN = "mosync-bin";
	public final static String RUNTIME_DIR = "runtime-dir";
	public final static String PROJECT_NAME = "project-name";
	public final static String OUTPUT_DIR = "output-dir";
	public final static String FINAL_OUTPUT_DIR = "final-output-dir";
	public static final String COMPILE_OUTPUT_DIR = "compile-output-dir";
	public static final String PACKAGE_OUTPUT_DIR = "package-output-dir";

	public final static String PLATFORM_ID = "platform";

	public final static String VENDOR_NAME = "vendor";
	public final static String PROFILE_NAME = "profile";
	
    private static final String PROGRAM_OUTPUT = "program-output";
    private static final String RESOURCE_OUTPUT = "resource-output";
    private static final String PROGRAMCOMB_OUTPUT = "programcomb-output";

	private MoSyncProject project;
	private CommandLineExecutor executor;
	private Map<String, String> userParameters = new HashMap<String, String>();
	private IProfile targetProfile;
	private Map<String, String> defaultParameters = new HashMap<String, String>();
	private boolean isFinalizerBuild;

	
	private CascadingProperties parameters;
	
	private DefaultPackager() {
	}

	public DefaultPackager(MoSyncProject project, IProfile targetProfile, boolean isFinalizerBuild) {
		parameters = new CascadingProperties(new Map[] { defaultParameters, userParameters });
		this.project = project;
		this.targetProfile = targetProfile;
		this.isFinalizerBuild = isFinalizerBuild;
		setDefaultParameters();
		initCommandLineExecutor(project);
	}

	public void setParameter(String param, String value) {
		userParameters.put(param, value);
	}

	public void setDefaultParameters() {
        defaultParameters.put(MOSYNC_HOME, MoSyncTool.getDefault().getMoSyncHome().toOSString());
        defaultParameters.put(MOSYNC_BIN, MoSyncTool.getDefault().getMoSyncBin().toOSString());
		IProfile targetProfile = this.targetProfile == null ? project.getTargetProfile() : this.targetProfile;
		defaultParameters.put(PROFILE_NAME, targetProfile.getName());
		defaultParameters.put(VENDOR_NAME, targetProfile.getVendor().getName());
		defaultParameters.put(RUNTIME_DIR, MoSyncTool.getDefault().getRuntimePath(targetProfile).toOSString());
		defaultParameters.put(PROJECT_NAME, project.getName());
		defaultParameters.put(OUTPUT_DIR, MoSyncBuilder.getOutputPath(project.getWrappedProject()).toOSString());
		defaultParameters.put(FINAL_OUTPUT_DIR, MoSyncBuilder.getFinalOutputPath(project.getWrappedProject(), targetProfile)
				.toOSString());
		defaultParameters.put(PLATFORM_ID, MoSyncBuilder.getAbbreviatedPlatform(targetProfile));
		
		defaultParameters.put(COMPILE_OUTPUT_DIR, MoSyncBuilder.getCompileOutputPath(project.getWrappedProject(), targetProfile, isFinalizerBuild).toOSString());
        defaultParameters.put(PROGRAM_OUTPUT, MoSyncBuilder.getProgramOutputPath(project.getWrappedProject(), targetProfile, isFinalizerBuild).toOSString());
        defaultParameters.put(RESOURCE_OUTPUT, MoSyncBuilder.getResourceOutputPath(project.getWrappedProject(), targetProfile, isFinalizerBuild).toOSString());
        defaultParameters.put(PROGRAMCOMB_OUTPUT, MoSyncBuilder.getProgramCombOutputPath(project.getWrappedProject(), targetProfile, isFinalizerBuild).toOSString());
        
        defaultParameters.put(PACKAGE_OUTPUT_DIR, MoSyncBuilder.getPackageOutputPath(project.getWrappedProject(), targetProfile, isFinalizerBuild).toOSString());
	}

	public CascadingProperties getParameters() {
	    return parameters;
	}
	
	protected void initCommandLineExecutor(MoSyncProject project) {
		setDefaultParameters();
		if (executor == null) {
			executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
			executor.setParameters(parameters);
			executor.setExecutionDirectory(project.getWrappedProject().getLocation().toOSString());
		}
	}

	public CommandLineExecutor getExecutor() {
		initCommandLineExecutor(project);
		return executor;
	}

	public void runCommandLine(String... commandLine) throws IOException {
		getExecutor().runCommandLine(commandLine);
	}
	
	public int runCommandLineWithRes ( String... commandLine ) 
	throws IOException {
		return getExecutor().runCommandLineWithRes( commandLine );
	}	

	public void mkdirs(String path) {
		String resolvedPath = CommandLineExecutor.replace(path, parameters);
		new File(resolvedPath).mkdirs();
	}

	public IProcessConsole getConsole() {
		return getExecutor().createConsole();
	}

	public String resolve(String path) {
		return CommandLineExecutor.replace(path, parameters);
	}

	public static void writeFile(File file, String contents) throws IOException {
		FileWriter output = new FileWriter(file);
		try {
			output.write(contents);
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}

	public void setParameters(Map<String, String> userParameters) {
		this.userParameters.putAll(userParameters);
	}

    public File resolveFile(String path) {
        return new File(resolve(path));
    }

}
