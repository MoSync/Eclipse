/*  Copyright (C) 2013 Mobile Sorcery AB

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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.build.IBuildStep;

public class Binutils {
	public static final String BUILD_MX_MODE = "-mx";	// replaces BUILD_C_MODE
	public static final String BUILD_GEN_CPP_MODE = "-cpp";
	public static final String BUILD_GEN_CS_MODE = "-cs";
	public static final String BUILD_LIB_MODE = "BUILD_LIB_MODE";	// not used on the command line.

	public static final int DEFAULT_HEAP_SIZE_KB = 512;
	public static final int DEFAULT_STACK_SIZE_KB = 128;
	//public static final int DEFAULT_DATA_SIZE_KB = 1024;

	private String appCode;
	private IPath outputFile;
	private String[] inputFiles;
	private IProcessConsole console;
	private String mode;
	private String[] extra;	// for GCC/ld
	private IProject project;
	private ILineHandler linehandler;
	private ParameterResolver resolver;
	private IBuildVariant variant;
	private IPath[] libraries;
	private IPath[] libraryPaths;
	private IPropertyOwner argumentMap;

	/**
	 * Returns the app code, corresponding
	 * to the elfStabSld -buildid switch.
	 * @return
	 */
	public String getAppCode() {
		return appCode;
	}

	/**
	 * Generates a random 4-character app code
	 * @return
	 */
	public static String generateAppCode() {
		Random rnd = new Random(System.currentTimeMillis());
		char[] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		char[] result = new char[4];
		for (int i = 0; i < result.length; i++) {
			result[i] = CHARS[rnd.nextInt(CHARS.length)];
		}

		return new String(result);
	}

	/**
	 * Sets the app code of this pipe tool
	 * @param appCode
	 */
	public void setAppCode(String appCode) {
		this.appCode = appCode;
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

	public void setProject(IProject project) {
		this.project = project;
	}

	public void setLineHandler(ILineHandler linehandler) {
		this.linehandler = linehandler;
	}

	public void setParameterResolver(ParameterResolver resolver) {
		this.resolver = resolver;
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

	public void setLibraries(IPath[] libraries) {
		this.libraries = libraries;
	}

	public void setLibraryPaths(IPath[] paths) {
		this.libraryPaths = paths;
	}

	public void setArguments(IPropertyOwner argumentMap) {
		this.argumentMap = argumentMap;
	}

	private void addMessage(String message) {
		if (console != null) {
			console.addMessage(message);
		}
	}

	private void addMemoryArg(ArrayList<String> args, IPropertyOwner argumentMap,
		String key, int defaultSize, String prefix)
	{
		Integer size = PropertyUtil.getInteger(argumentMap, key);
		if (size != null) {
			args.add(prefix);
			args.add(size.toString());
		}
	}

	private String[] assembleLibraryArgs(IPath[] libraries) throws ParameterResolverException {
		String[] result = new String[libraries.length];
		for (int i = 0; i < result.length; i++) {
			IPath lib = libraries[i];
			for(int j = 0; j<libraryPaths.length; j++) {
				IPath path = libraryPaths[j];
				IPath total = path.append(lib);
				if(total.toFile().exists()) {
					result[i] = Util.ensureQuoted(Util.replace(total.toOSString(), resolver));
					break;
				}
			}
			if(result[i] == null) {
				throw new ParameterResolverException("library", lib.toString());
			}
		}

		return result;
	}

	private int run(ArrayList<String> args) throws CoreException, ParameterResolverException {
		addMessage(Util.join(args.toArray(new String[0]), " "));
		try {
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

			if (result != 0) {
				throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
						MessageFormat.format("Binutils failed. (See console for more information).\n Command line: {0}", cmdLine)));
			}

			return result;
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	private int runXgcc() throws CoreException, ParameterResolverException {
		IPath xgcc = MoSyncTool.getDefault().getBinary("../mapip2/xgcc");

		ArrayList<String> args = new ArrayList<String>();

		args.add(xgcc.toOSString());

		args.addAll(Arrays.asList(inputFiles));

		if (extra != null && Util.join(extra, "").trim().length() > 0) {
			args.addAll(Arrays.asList(Util.replace(extra, resolver)));
		}

		args.add("-nodefaultlibs");
		args.add("-nostartfiles");
		args.add("-Wl,--warn-common,--emit-relocs,--no-check-sections");
		args.add("-Wl,--start-group");
		args.addAll(Arrays.asList(assembleLibraryArgs(libraries)));
		args.add("-Wl,--end-group");

		args.add("-o");
		args.add(outputFile.addFileExtension("elf").toOSString());

		return run(args);
	}

	private int runElfStabSld() throws CoreException, ParameterResolverException {
		IPath elfStabSld = MoSyncTool.getDefault().getBinary("elfStabSld");

		ArrayList<String> args = new ArrayList<String>();

		args.add(elfStabSld.toOSString());

		// Split up multiple arguments
		for(String a : mode.split(" "))
			args.add(a);

		boolean mx = mode.equals(BUILD_MX_MODE);
		if(mx)
			args.add(outputFile.toOSString());

		if (mx && appCode != null) {
			args.add("-buildid");
			args.add(getAppCode());
		}

		if (mx && argumentMap != null) {
			addMemoryArg(args, argumentMap, MoSyncBuilder.MEMORY_HEAPSIZE_KB, DEFAULT_HEAP_SIZE_KB, "-heapsize");
			addMemoryArg(args, argumentMap, MoSyncBuilder.MEMORY_STACKSIZE_KB, DEFAULT_STACK_SIZE_KB, "-stacksize");
		}

		args.add(outputFile.addFileExtension("elf").toOSString());
		if(mx)
			args.add(outputFile.addFileExtension("sld").toOSString());
		else if(mode.equals(BUILD_GEN_CPP_MODE))
			args.add(outputFile.removeLastSegments(1).append("rebuild.build.cpp").toOSString());
		else if(mode.equals(BUILD_GEN_CS_MODE))
			args.add(outputFile.removeLastSegments(1).append("rebuild.build.cs").toOSString());
		else
			args.add(outputFile.toOSString());

		return run(args);
	}

	// Run, runner!
	public int run() throws CoreException, ParameterResolverException {
		int res;
		res = runXgcc();
		if(res != IBuildStep.CONTINUE)
			return res;
		res = runElfStabSld();
		return res;
	}
}
