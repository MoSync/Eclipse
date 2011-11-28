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
/**
 *
 */
package com.mobilesorcery.sdk.internal.builder;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;

import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.PipeTool;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;
import com.mobilesorcery.sdk.internal.dependencies.IDependencyProvider;
import com.mobilesorcery.sdk.internal.dependencies.ResourceFileDependencyProvider;

public class MoSyncResourceBuilderVisitor extends IncrementalBuilderVisitor {
	private ArrayList<IResource> resourceFiles = new ArrayList<IResource>();
	private IPath outputFile;
	private PipeTool pipeTool;

	private IDependencyProvider<IResource> dependencyProvider;

	@Override
	public boolean visit(IResource resource) throws CoreException {
		super.visit(resource);

		if (isResourceFile(resource)) {
			resourceFiles.add(resource);
		}

		return true;
	}

	public static boolean isResourceFile(IResource resource) {
		if (resource.getType() == IResource.FILE) {
			IFile file = (IFile) resource;
			String name = file.getName();
			return ((name.endsWith(".lst") || name.endsWith(".lstx")) && !name.startsWith("stabs.") && !name.startsWith("~tmpres."));
		}

		return false;
	}

	public String[] getResourceFiles() throws CoreException {
		// TODO: side-effect-less
		resourceFiles = new ArrayList<IResource>();
		project.accept(this);
		String[] result = new String[resourceFiles.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = resourceFiles.get(i).getLocation().toOSString();
		}
		return result;
	}

	public void setOutputFile(IPath outputFile) {
		this.outputFile = outputFile;
	}

	public void setPipeTool(PipeTool pipeTool) {
		this.pipeTool = pipeTool;
	}

	private int countResourceFiles(List<String> resourceFiles, String ext) {
		int count = 0;
		for (String resourceFile : resourceFiles) {
			if (ext.equals(Util.getExtension(new File(resourceFile)))) {
				count++;
			}
		}
		return count;
	}

	private File getResourcesDirectory() {
		File resDir = getProject().getLocation().append("Resources").toFile();
		return resDir.exists() && resDir.isDirectory() ? resDir : null;
	}

	public void incrementalCompile(IProgressMonitor monitor, DependencyManager<IResource> dependencyManager, DependencyManager.Delta<IResource> dependencyDelta) throws CoreException, IOException {
		Set<IResource> recompileThese = computeResourcesToRebuild(dependencyManager);
		if (!recompileThese.isEmpty()) {
			List<String> resourceFiles = new ArrayList<String>(Arrays.asList(getResourceFiles()));
			File resDir = getResourcesDirectory();
			int lstxCount = countResourceFiles(resourceFiles, "lstx");
			if (resDir != null) {
				lstxCount++;
				resourceFiles.add(resDir.getAbsolutePath());
			}
			int lstCount = countResourceFiles(resourceFiles, "lst");
			if (lstxCount > 0) {
				if (lstCount > 0) {
					throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Cannot mix .lst files and .lstx files or 'Resources' directories"));
				}
				// Beware; here we once more update the resourceFiles array...
				resourceFiles = Arrays.asList(compileWithResComp(resourceFiles.toArray(new String[0]), monitor, dependencyManager, dependencyDelta));
			}

			compileWithPipeTool(monitor, dependencyManager, dependencyDelta, resourceFiles.toArray(new String[0]));
		}
	}

	private String[] getExtraSwitches() {
		String[] extraResourceSwitches = PropertyUtil.getStrings(getBuildProperties(), MoSyncBuilder.EXTRA_RES_SWITCHES);
		return extraResourceSwitches;
	}

	private String compileWithResComp(String[]resourceFiles, IProgressMonitor monitor, DependencyManager<IResource> dependencyManager, DependencyManager.Delta<IResource> dependencyDelta) throws CoreException, IOException {
		if (getExtraSwitches().length > 0) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Extra resource switches are not allowed when using .lstx files or file tree based resources."));
		}
		IPath rescomp = MoSyncTool.getDefault().getBinary("rescomp");
		IPath resourceOutput = MoSyncBuilder.getResourceOutputPath(project, getVariant());
		IPath intermediateLstFile = resourceOutput.removeLastSegments(1).append("~tmpres.lst");
		String platform = getVariant().getProfile().getPackager().getPlatform();
		if (platform == null) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format("No platform defined for {0}", getVariant())));
		}

		ArrayList<String> args = new ArrayList<String>();
		args.add(rescomp.toOSString());
		args.add("-L");
		args.add(platform.toLowerCase());
		args.add(intermediateLstFile.removeLastSegments(1).toOSString());
		for (String resourceFile : resourceFiles) {
			args.add(resourceFile);
		}

		CommandLineExecutor exe = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
		if (exe.runCommandLine(args.toArray(new String[0])) != 0) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not compile resources"));
		}

		return intermediateLstFile.toOSString();
	}

	private void compileWithPipeTool(IProgressMonitor monitor, DependencyManager<IResource> dependencyManager, DependencyManager.Delta<IResource> dependencyDelta, String[] lstFiles) throws CoreException {
		String[] extraResourceSwitches = PropertyUtil.getStrings(getBuildProperties(), MoSyncBuilder.EXTRA_RES_SWITCHES);
		pipeTool.setExtraSwitches(extraResourceSwitches);
		pipeTool.setMode(PipeTool.BUILD_RESOURCES_MODE);
		pipeTool.setInputFiles(lstFiles);
		pipeTool.setOutputFile(outputFile);
		pipeTool.setParameterResolver(getParameterResolver());
		try {
			pipeTool.run();
		} catch (ParameterResolverException e) {
			throw ParameterResolverException.toCoreException(e);
		}

		// Explicitly add dependencies for the pipetool output file -- TODO:
		// outputfile must equal getresourceoutput; remove one.
		IPath resourcePath = MoSyncBuilder.getResourceOutputPath(project, getVariant());
		IFile[] resourceFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(resourcePath.toFile().toURI());
		for (IFile resourceFile : resourceFiles) {
			dependencyDelta.addDependencies(resourceFile, dependencyProvider);
		}
		dependencyDelta.addDependencies(this.resourceFiles, dependencyProvider);
	}

	@Override
	public void setDependencyProvider(IDependencyProvider<IResource> dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public boolean isBuildable(IResource resource) {
		// Ok, I added the 'resource' file just so it will not get filtered out -- what we'd really
		// like is the dependency files of pipe-tool resource compilation to work properly
		return isResourceFile(resource) || MoSyncBuilder.getResourceOutputPath(project, getVariant()).equals(resource.getLocation());
	}

	protected String getName() {
		return "Resource Compiler";
	}

}