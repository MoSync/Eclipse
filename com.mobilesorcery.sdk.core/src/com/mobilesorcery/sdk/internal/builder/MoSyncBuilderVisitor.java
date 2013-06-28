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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;

// TODO: The main responsibility of this class is no longer to
// visit projects - split into 2 classes!
// TODO: Replace all IResource references in build classes with IPath. (Huge refactoring)
public class MoSyncBuilderVisitor extends IncrementalBuilderVisitor {

    private static final String GCC_WALL_STR = "-Wall";

    private static final String GCC_WEXTRA_STR[] = {"-Wextra", "-Wno-unused-parameter"};

    private static final String GCC_WERROR_STR = "-Werror";

    private static final String GCC_WMOAR_STR[] = {
		"-Wwrite-strings", "-Wshadow", "-Wpointer-arith", "-Wundef", "-Wfloat-equal",
		"-Winit-self", "-Wmissing-noreturn", "-Wmissing-format-attribute",
		"-Wvariadic-macros", "-Wmissing-include-dirs", "-Wmissing-declarations", "-Wlogical-op",
    };

    /**
     * The standard extensions for C/C++ files
     */
    public static final String[] C_SOURCE_FILE_EXTS = new String[] { "cpp", "c++", "c", "cc" };

    /**
     * The standard extensions for C++ files (no C file extensions)
     */
    public static final String[] CPP_SOURCE_FILE_EXTS = new String[] { "cpp", "c++", "cc" };

    public static final String[] C_HEADER_FILE_EXTS = new String[] { "hpp", "h++", "h" };

    public static final String[] CPP_HEADER_FILE_EXTS = new String[] { "hpp", "h++" };

    public static final String[] RESOURCE_FILE_EXTS = new String[] { "lst", "lstx" };

    public MoSyncBuilderVisitor() {
    }

    private ArrayList<String> objectFiles = new ArrayList<String>();
    private int errors;
	private int compileCount = 0;
    private IPath outputPath;
    private String extraSwitches;
    private IBuildResult buildResult;
    private ILineHandler linehandler;
    private int gccWarnings;
	private boolean generateDependencies = true;

    @Override
	public boolean visit(IResource resource) throws CoreException {
    	boolean shouldVisitChildren = super.visit(resource);
    	if (isBuildable(resource)) {
	        IFile cFile = getCFile(resource, false);
	        if (cFile != null && outputPath != null) {
	            objectFiles.add(mapFileToOutput(cFile).toOSString());
	        }
    	}

        return shouldVisitChildren;
    }

    /**
     * Performs an incremental compile; either setChangedOrAddedResources
     * and setDeletedResources must have been called, or a project must
     * have been visited by this visitor (visit method)
     * @param monitor
     * @throws CoreException
     * @throws ParameterResolverException
     */
    public void incrementalCompile(IProgressMonitor monitor, DependencyManager<IResource> dependencies, DependencyManager.Delta<IResource> delta) throws CoreException, ParameterResolverException {
    	Set<IResource> recompileThese = computeResourcesToRebuild(dependencies);

        IResource[] deletedResources = this.deletedResources.toArray(new IResource[0]);
        for (int i = 0; i < deletedResources.length; i++) {
            if (monitor.isCanceled()) {
                return;
            }

            if (shouldBuild(deletedResources[i])) {
            	deleteBuildResult(deletedResources[i], dependencies);
            }
        }

        for (IResource recompileThis : recompileThese) {
            if (monitor.isCanceled()) {
                return;
            }
            if (shouldBuild(recompileThis)) {
            	compile(recompileThis, delta);
            }
        }
    }

    private boolean shouldBuild(IResource recompileThis) {
		return project.equals(recompileThis.getProject());
	}

	private void deleteBuildResult(IResource resource, DependencyManager<IResource> dependencies) {
        IFile cFile = getCFile(resource, false);
        if (cFile != null) {
            IPath output = mapFileToOutput(cFile);
            console.addMessage(MessageFormat.format("Deleting {0}", output.toOSString()));
            output.toFile().delete();
        }

        if (dependencies != null) {
        	dependencies.clearDependencies(resource);
        }
    }

    /**
     * Returns a cast resource if the resource is a c source file,
     * otherwise null. If the resource is a derived resource
     * (such as for example generated C files), {@code null} is
     * returned
     * @param res
     * @param if true, also header files are included in the filtering
     * @return
     */
    public static IFile getCFile(IResource resource, boolean header) {
        if (hasExtension(resource, C_SOURCE_FILE_EXTS)) {
            return (IFile) resource;
        }

        if (header && hasExtension(resource, C_HEADER_FILE_EXTS)) {
            return (IFile) resource;
        }

        return null;
    }

    @Override
	public boolean doesAffectBuild(IResource resource) {
        return (MoSyncBuilderVisitor.hasExtension(resource, MoSyncBuilderVisitor.C_SOURCE_FILE_EXTS) ||
                MoSyncBuilderVisitor.hasExtension(resource, MoSyncBuilderVisitor.C_HEADER_FILE_EXTS) ||
                MoSyncBuilderVisitor.hasExtension(resource, MoSyncBuilderVisitor.RESOURCE_FILE_EXTS)) && super.doesAffectBuild(resource);
    }

    /**
     * Returns true if resource is a file and if its extension
     * is one of <code>extensions</code>.
     * @param resource
     * @param extensions
     * @return
     */
    public static boolean hasExtension(IResource resource, String... extensions) {
        if (resource.getType() == IResource.FILE) {
            IFile file = (IFile) resource;
            String ext = file.getFileExtension();
            for (int i = 0; i < extensions.length; i++) {
                if (extensions[i].equalsIgnoreCase(ext)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void compile(IResource resource, DependencyManager.Delta<IResource> dependenciesDelta) throws CoreException, ParameterResolverException {
    	if (!CoreMoSyncPlugin.isHeadless()) {
    		MoSyncBuilder.clearCMarkers(resource);
            //clearCMarkers(resource.getProject());
    	}

        IFile cFile = getCFile(resource, false);

        if (cFile != null) {
            // Assume unique filenames.
            IPath output = mapFileToOutput(cFile);

            IPath xgcc = MoSyncTool.getDefault().getBinary("../mapip2/xgcc");

            MoSyncProject project = MoSyncProject.create(resource.getProject());
            List<IPath> includePaths = new ArrayList<IPath>(Arrays.asList(MoSyncBuilder.getBaseIncludePaths(project, getVariant())));

			// TODO: Too much 'secret sauce' here; add special dialogs for this instead,
			// like JDT/CDT, to allow user to control this better. Like %output%?
            includePaths.add(outputPath);

            String[] includeStr = assembleIncludeString(includePaths.toArray(new IPath[0]));

            ArrayList<String> args = new ArrayList<String>();
            args.add(xgcc.toOSString());
            args.add("-c");
            args.add("-g");
						if (hasExtension(resource, CPP_SOURCE_FILE_EXTS)) {
							args.add("-std=gnu++0x");
						} else {
							args.add("-std=gnu99");
						}

            if (generateDependencies) {
            	args.add("-MMD");
            	args.add("-MF");
            	args.add(mapToDependencyFile(output.toOSString()));
            }

            addGccWarnings(args);
            String[] extra = extraSwitches == null ? new String[0] : Util.parseCommandLine(resolve(extraSwitches));
            args.addAll(Arrays.asList(extra));
            args.add(cFile.getLocation().toOSString());
            args.addAll(Arrays.asList(includeStr));

            // Put output last, so all the flags will affect it.
            args.add("-o");
            args.add(output.toOSString());

            // Create output if it does not exist
            output.toFile().getParentFile().mkdirs();

            String argsAsArray[] = new String[args.size()];
            args.toArray(argsAsArray);

            // Display invocation in console
            String cmdLine = Util.join(argsAsArray, " ");
            console.addMessage(cmdLine);

            try {
                // Java automatically escapes the arguments when we call exec with an array instead of a string
                Process process = Runtime.getRuntime().exec(argsAsArray, null, resource.getProject().getLocation().toFile());

                console.attachProcess(process, linehandler);

                int result = process.waitFor();
                if (result != 0) {
                    errors++;
                    if (buildResult != null) {
                        buildResult.addError("Failed to compile " + cFile.getLocation());
                    }
                }
                compileCount ++;
            } catch (Exception e) {
                throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
            }
        }

        if (dependenciesDelta != null) {
        	dependenciesDelta.addDependencies(resource, getDependencyProvider());
        }

    }

	public static String mapToDependencyFile(String filename) {
    	 return filename + ".deps";
    }

	public void setGenerateDependencies(boolean generateDependencies) {
    	this.generateDependencies = generateDependencies;
    }

    private void addGccWarnings(List<String> args) {
        if ((gccWarnings & MoSyncBuilder.GCC_WALL) != 0) {
            args.add(GCC_WALL_STR);
        }

        if ((gccWarnings & MoSyncBuilder.GCC_WEXTRA) != 0) {
            args.addAll(Arrays.asList(GCC_WEXTRA_STR));
        }

        if ((gccWarnings & MoSyncBuilder.GCC_WERROR) != 0) {
            args.add(GCC_WERROR_STR);
        }

        if ((gccWarnings & MoSyncBuilder.GCC_WMOAR) != 0) {
            args.addAll(Arrays.asList(GCC_WMOAR_STR));
        }
    }

    public static String[] assembleIncludeString(IPath[] includePaths) {
        String[] strs = new String[includePaths.length];
        for (int i = 0; i < strs.length; i++) {
            strs[i] = assembleIncludeString(includePaths[i]);
        }

        return strs;
    }

    private static String assembleIncludeString(IPath includePath) {
    	// Remove trailing separator, otherwise the \ will be considered an escape char.
        return "-I" + includePath.removeTrailingSeparator().toOSString();
    }

    public IPath mapFileToOutput(IResource file) {
    	return mapFileToOutput(file, outputPath);
    }

    public static IPath mapFileToOutput(IResource file, IPath outputPath) {
        String name = file.getName();
        String newName = Util.replaceExtension(name, "o");
        return outputPath.append(newName);
    }

    public String[] getObjectFilesForProject(IProject project) throws CoreException {
        objectFiles = new ArrayList<String>();
        project.accept(this);
        return objectFiles.toArray(new String[0]);
    }

    public int getErrorCount() {
        return errors;
    }

    public int getCompileCount() {
    	return compileCount;
    }

    public void setOutputPath(IPath outputPath) {
        this.outputPath = outputPath;
    }

    public void setExtraCompilerSwitches(String extraSwitches) {
        this.extraSwitches = extraSwitches;
    }

    public void setLineHandler(ILineHandler linehandler) {
        this.linehandler = linehandler;
    }

    public void setBuildResult(IBuildResult buildResult) {
        this.buildResult = buildResult;
    }

    public void setGCCWarnings(int gccWarnings) {
        this.gccWarnings = gccWarnings;
    }

	protected String getName() {
		return "C Compiler";
	}
}
