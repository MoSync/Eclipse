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
package com.mobilesorcery.sdk.internal;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;

public class PipeTool {

	public static final String BUILD_RESOURCES_MODE = "-R";

	private static final String RESOURCE_DEPENDENCY_FILE_NAME = "resources.deps";

    /**
     *  A return code from pipe tool that instructs the builder to skip
     *  any remaining build steps.
	 */
	public static final int SKIP_RETURN_CODE = -10000;

    private IPath outputFile;
    private String[] inputFiles;
    private IProcessConsole console;
    private String mode;
    private String[] extra;
    private IProject project;
    private ILineHandler linehandler;
	private ParameterResolver resolver;
	private IBuildVariant variant;

    public PipeTool() {

    }

    public void setConsole(IProcessConsole console) {
        this.console = console;
    }

    public void setOutputFile(IPath outputPath) {
        this.outputFile = outputPath;
    }

    public void setInputFiles(String[] objectFiles) {
        this.inputFiles = objectFiles;
    }

    public void setVariant(IBuildVariant variant) {
    	this.variant = variant;
    }

    /**
     * <p>Runs pipetool and returns the error code
     * if it is either <code>0</code> or {@link #SKIP_RETURN_CODE}
     * (Otherwise it will throw an exception).</p>
     * @return
     * @throws CoreException
     * @throws ParameterResolverException
     */
    public int run() throws CoreException, ParameterResolverException {
    	IPath pipeTool = MoSyncTool.getDefault().getBinary("pipe-tool");

        ArrayList<String> args = new ArrayList<String>();

        args.add(pipeTool.toOSString());

        if (extra != null && Util.join(extra, "").trim().length() > 0) {
            args.addAll(Arrays.asList(Util.replace(extra, resolver)));
        }

        // Split up multiple arguments
        for ( String a : mode.split( " " ) )
        	args.add( a );

        if (BUILD_RESOURCES_MODE == mode) {
        	IPath depsFile = getResourcesDependencyFile(project, variant);
        	depsFile.toFile().getParentFile().mkdirs();
        	// Pipetool only accepts -depend files in exeuction dir
        	args.add("-depend=" + depsFile.toOSString());
        }

        args.add(outputFile.toOSString());

        args.addAll(Arrays.asList(inputFiles));

        addMessage(Util.join(args.toArray(new String[0]), " "));

        try {
            assertArgLength(args);

            boolean ensureOutputFileParentExists = outputFile.toFile().getParentFile().mkdirs();
            if (ensureOutputFileParentExists && CoreMoSyncPlugin.getDefault().isDebugging()) {
            	CoreMoSyncPlugin.trace("Created directory {0}", outputFile.toFile().getParentFile());
            }

            getExecDir().mkdirs();

            // Note where it's executed - this is where the sld will end up.
            Process process = DebugPlugin.exec(args.toArray(new String[0]), getExecDir());

            String cmdLine = Util.join(Util.ensureQuoted(args.toArray()), " ");

            if (CoreMoSyncPlugin.getDefault().isDebugging()) {
            	CoreMoSyncPlugin.trace(cmdLine);
            }

            console.attachProcess(process, linehandler);
            int result = process.waitFor();

            if (result != 0 && result != SKIP_RETURN_CODE) {
                throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
                        MessageFormat.format("Pipe tool failed. (See console for more information).\n Command line: {0}", cmdLine)));
            }

            return result;
        } catch (CoreException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

	private void assertArgLength(ArrayList<String> args) throws IOException {
    	/*for (String arg : args) {
    		if (arg.length() > MAX_PIPE_TOOL_ARG_LENGTH) {
    			throw new IOException(MessageFormat.format("Argument/file name too long: {0} (max length {1} characters, was {2} characters)", arg, MAX_PIPE_TOOL_ARG_LENGTH, arg.length()));
    		}
    	}*/
	}

	public static IPath getResourcesDependencyFile(IProject project, IBuildVariant variant) {
    	return MoSyncBuilder.getOutputPath(project, variant).append(RESOURCE_DEPENDENCY_FILE_NAME);
    }

    private void addMessage(String message) {
        if (console != null) {
            console.addMessage(message);
        }
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public File getExecDir() {
    	//return getExecDir(project);
    	return outputFile.toFile().getParentFile();
    }

    public void setExtraSwitches(String[] extra) {
        this.extra = extra;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    public void setLineHandler(ILineHandler linehandler) {
        this.linehandler = linehandler;
    }

	public void setParameterResolver(ParameterResolver resolver) {
		this.resolver = resolver;
	}
}
