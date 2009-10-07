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
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IErrorParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.resources.ACBuilder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.internal.PipeTool;
import com.mobilesorcery.sdk.internal.builder.MoSyncBuilderVisitor;
import com.mobilesorcery.sdk.internal.builder.MoSyncIconBuilderVisitor;
import com.mobilesorcery.sdk.internal.builder.MoSyncResourceBuilderVisitor;
import com.mobilesorcery.sdk.internal.dependencies.CompoundDependencyProvider;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;
import com.mobilesorcery.sdk.internal.dependencies.GCCDependencyProvider;
import com.mobilesorcery.sdk.internal.dependencies.IDependencyProvider;
import com.mobilesorcery.sdk.internal.dependencies.ProjectResourceDependencyProvider;
import com.mobilesorcery.sdk.internal.dependencies.ResourceFileDependencyProvider;
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * The main builder. This builder extends ACBuilder for its implementation of
 * {@link IMarkerGenerator}.
 * 
 * @author Mattias
 * 
 */
public class MoSyncBuilder extends ACBuilder {

    public final static String ID = CoreMoSyncPlugin.PLUGIN_ID + ".builder";
    
    public static final String COMPATIBLE_ID = "com.mobilesorcery.sdk.builder.builder";  

    public static final String CONSOLE_ID = "com.mobilesorcery.build.console";

    public final static String IS_FINALIZER_BUILD = "finalizer-build";

    private static final String BUILD_PREFS_PREFIX = "build.prefs:";

    public final static String ADDITIONAL_INCLUDE_PATHS = BUILD_PREFS_PREFIX + "additional.include.paths";

    public final static String IGNORE_DEFAULT_INCLUDE_PATHS = BUILD_PREFS_PREFIX + "ignore.default.include.paths";

    public final static String ADDITIONAL_LIBRARY_PATHS = BUILD_PREFS_PREFIX + "additional.library.paths";

    public final static String IGNORE_DEFAULT_LIBRARY_PATHS = BUILD_PREFS_PREFIX + "ignore.default.library.paths";

    public final static String ADDITIONAL_LIBRARIES = BUILD_PREFS_PREFIX + "additional.libraries";

    public final static String IGNORE_DEFAULT_LIBRARIES = BUILD_PREFS_PREFIX + "ignore.default.libraries";
    
    public static final String OUTPUT_PATH = "output.path";

    public static final String DEAD_CODE_ELIMINATION = BUILD_PREFS_PREFIX + "dead.code.elim";

    public static final String EXTRA_LINK_SWITCHES = BUILD_PREFS_PREFIX + "extra.link.sw";

    public static final String EXTRA_RES_SWITCHES = BUILD_PREFS_PREFIX + "extra.res.sw";

    public static final String PROJECT_TYPE = BUILD_PREFS_PREFIX + "project.type";

    public static final String PROJECT_TYPE_APPLICATION = "app";

    public static final String PROJECT_TYPE_LIBRARY = "lib";

    public static final String EXTRA_COMPILER_SWITCHES = BUILD_PREFS_PREFIX + "gcc.switches";
    
    public static final String GCC_WARNINGS = BUILD_PREFS_PREFIX + "gcc.warnings";

    public static final String USE_DEBUG_RUNTIME_LIBS = BUILD_PREFS_PREFIX + "runtime.debug";
    
    public static final int GCC_WALL = 1 << 1;
    
    public static final int GCC_WEXTRA = 1 << 2;
    
    public static final int GCC_WERROR = 1 << 3;

    public final class GCCLineHandler implements ILineHandler {
        
        private ErrorParserManager epm;

        public GCCLineHandler(ErrorParserManager epm) {
            this.epm = epm;
        }
        
        private String aggregateLine = "";

        public void newLine(String line) {
            // We need to 'aggregate' lines;
            // whenever a line is indented,
            // it is actually an extension
            // of last line - it's a hack to reconcile
            // the CDT error parser and the 'real world'
            if (isLineIndented(line)) {
                aggregateLine += " " + line.trim();
            } else {
                reportLine(aggregateLine);
                aggregateLine = line.trim();
            }
        }

        private boolean isLineIndented(String line) {
            return line.startsWith(Util.fill(' ', 2));
        }

        public void reportLine(String line) {
            try {
                // Kind of backwards..., first we remove a \n, and
                // now
                // we add it, so the ErrorParserManager can remove
                // it again...
                if (epm != null) {
                    // Write directly to emp, ignore
                    // getoutputstream.
                    epm.write((line + '\n').getBytes());
                }
            } catch (IOException e) {
                // Ignore.
            }
        }

        public void stop(IOException e) {
            newLine("");
            /*
             * if (aggregateLine.length() > 0) {
             * reportLine(aggregateLine); }
             */
        }
        
        public void reset() {
            stop(null);
            aggregateLine = "";
        }
    }

    public MoSyncBuilder() {
    }

    public IProfile getTargetProfile() {
        MoSyncProject project = MoSyncProject.create(getProject());
        IProfile target = project.getTargetProfile();
        if (target == null) {
            target = MoSyncTool.getDefault().getDefaultTargetProfile();
        }

        return target;
    }

    public IPath getProjectPath() {
        return getProject().getLocation();
    }

    public static IPath getOutputPath(IProject project) {
        return project.getFile("Output").getLocation();
    }

    public static IPath getFinalOutputPath(IProject project, IProfile targetProfile) {
        return project.getLocation().append("FinalOutput").append(targetProfile.getVendor().getName()).append(
                targetProfile.getName());
    }

    static IPath getCompileOutputPath(IProject project, IProfile targetProfile, boolean isFinalizeBuild) {
        return isFinalizeBuild ? getFinalOutputPath(project, targetProfile) : getOutputPath(project);
    }

    public static IPath getProgramOutputPath(IProject project, IProfile targetProfile, boolean isFinalizeBuild) {
        return getCompileOutputPath(project, targetProfile, isFinalizeBuild).append("program");
    }

    public static IPath getProgramCombOutputPath(IProject project, IProfile targetProfile, boolean isFinalizeBuild) {
        return getCompileOutputPath(project, targetProfile, isFinalizeBuild).append("program.comb");
    }

    public static IPath getResourceOutputPath(IProject project, IProfile targetProfile, boolean isFinalizeBuild) {
        return getCompileOutputPath(project, targetProfile, isFinalizeBuild).append("resources");
    }

    public static IPath getPackageOutputPath(IProject project, IProfile targetProfile, boolean isFinalizerBuild) {
        if (isFinalizerBuild) {
            return getFinalOutputPath(project, targetProfile).append("package");
        } else {
            return getOutputPath(project).append(getAbbreviatedPlatform(targetProfile));
        }
    }

    public static String getAbbreviatedPlatform(IProfile targetProfile) {
        String platform = targetProfile.getPlatform();
        String abbrPlatform = platform.substring("profiles\\runtime\\".length() + 1, platform.length());
        return abbrPlatform;
    }

    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
        IProject project = getProject();
        IProfile targetProfile = getTargetProfile();

        // TODO: Duplicate, see incrementalBuild method. Fix bug, then remove this and retest.
        IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        project.deleteMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        if (markers.length > 0) {
        	// If we had problems, always do a full build. (For now)
            kind = FULL_BUILD;
        }
        
        if (MoSyncProject.NULL_DEPENDENCY_STRATEGY == PropertyUtil.getInteger(MoSyncProject.create(project), MoSyncProject.DEPENDENCY_STRATEGY, MoSyncProject.GCC_DEPENDENCY_STRATEGY)) {
        	// TODO: At this point, we only have a GCC dependency strategy and a "always full build" strategy 
        	kind = FULL_BUILD;
        }
        
        IResourceDelta[] deltas = kind == FULL_BUILD ? null : getDeltas(getProject());
        boolean doPack = kind == FULL_BUILD;
        incrementalBuild(project, deltas, targetProfile, false, doPack, monitor);

        Set<IProject> dependencies = CoreMoSyncPlugin.getDefault().getProjectDependencyManager(ResourcesPlugin.getWorkspace()).getDependenciesOf(project);
        dependencies.add(project);
        
        return dependencies.toArray(new IProject[dependencies.size()]);
    }

    /**
     * Returns all deltas for <code>project</code>; including
     * any projects that <code>project</code> depends on.
     * @param project
     * @return
     */
    private IResourceDelta[] getDeltas(IProject project) {
    	Set<IProject> projectDependencies = CoreMoSyncPlugin.getDefault().getProjectDependencyManager().getDependenciesOf(project);
    	projectDependencies.add(project);
		ArrayList<IResourceDelta> result = new ArrayList<IResourceDelta>();
		for (IProject projectDependency : projectDependencies) {
			IResourceDelta delta = getDelta(projectDependency);
			if (delta != null) {
				result.add(delta);
			}
		}
		
		return result.toArray(new IResourceDelta[result.size()]);
	}

	public void clean(IProgressMonitor monitor) {
        forgetLastBuiltState();
        IProject project = getProject();
        IProfile targetProfile = getTargetProfile();
        clean(project, targetProfile, false, monitor);
        MoSyncProject.create(project).getDependencyManager().clear();
    }

    public void clean(IProject project, IProfile targetProfile, boolean isFinalizerBuild, IProgressMonitor monitor) {
        IPath output = getCompileOutputPath(project, targetProfile, isFinalizerBuild);
        File outputFile = output.toFile();

        IProcessConsole console = CoreMoSyncPlugin.getDefault().createConsole(CONSOLE_ID);
        prepareConsole(console);

        console.addMessage(MessageFormat.format("Cleaning project {0}", project.getName()));
        Util.deleteFiles(getPackageOutputPath(project, targetProfile, isFinalizerBuild).toFile(), null, 512, monitor);
        Util.deleteFiles(getProgramOutputPath(project, targetProfile, isFinalizerBuild).toFile(), null, 1, monitor);
        Util.deleteFiles(getProgramCombOutputPath(project, targetProfile, isFinalizerBuild).toFile(), null, 1, monitor);
        Util.deleteFiles(getResourceOutputPath(project, targetProfile, isFinalizerBuild).toFile(), null, 1, monitor);
        Util.deleteFiles(outputFile, Util.getExtensionFilter("s"), 512, monitor);
    }

    public IBuildResult fullBuild(IProject project, IProfile targetProfile, boolean isFinalizerBuild, boolean doClean, IProgressMonitor monitor)
            throws CoreException {
        monitor.beginTask(MessageFormat.format("Full build of {0}", project.getName()), 8);
        if (doClean) {
            clean(project, targetProfile, isFinalizerBuild, new SubProgressMonitor(monitor, 1));
        } else {
            monitor.worked(1);
        }
        return incrementalBuild(project, null, targetProfile, isFinalizerBuild, true, monitor);
    }

    public IBuildResult incrementalBuild(IProject project, IResourceDelta[] deltas, IProfile targetProfile, boolean isFinalizerBuild,
            boolean doPack, IProgressMonitor monitor) throws CoreException {
    	
    	if (CoreMoSyncPlugin.getDefault().isDebugging()) {
    		CoreMoSyncPlugin.trace("Building project {0}", project);
    	}
    	
        ErrorParserManager epm = createErrorParserManager(project);

        try {
            BuildResult buildResult = new BuildResult(project);

            monitor.beginTask(MessageFormat.format("Building {0}", project), 4);

            MoSyncProject mosyncProject = MoSyncProject.create(project);
            
            // TODO: No longer necessary, since dependency mgr?
            //if (deltas != null && !hasDeltaThatAffectsBuild(mosyncProject, deltas)) {
            //    monitor.done();
            //    return buildResult;
            //}
                    
        	monitor.setTaskName("Clearing old problem markers");
    		boolean hadMarkers = clearCMarkers(project);
    		
    		if (hadMarkers) {
    			// Build all
    			deltas = null;
    		}

            if (deltas == null) {
            	mosyncProject.getDependencyManager().clear();
            }
            
            boolean isLib = PROJECT_TYPE_LIBRARY.equals(mosyncProject.getProperty(PROJECT_TYPE));

            GCCLineHandler linehandler = new GCCLineHandler(epm);
            
            IProcessConsole console = CoreMoSyncPlugin.getDefault().createConsole(CONSOLE_ID);
            prepareConsole(console);

            if (!MoSyncTool.getDefault().isValid()) {
                String error = MoSyncTool.getDefault().validate();
                console.addMessage(MessageFormat.format("MoSync Tool not properly initialized: {0}", error));
                console.addMessage("- go to Window > Preferences > MoSync Tool to set the MoSync home directory");
            }

            console.addMessage("Build started at "
                    + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(Calendar.getInstance().getTime()));

            console.addMessage(MessageFormat.format("Building project {0} for profile {1}", project.getName(), targetProfile));

            MoSyncBuilderVisitor compilerVisitor = new MoSyncBuilderVisitor();

            PipeTool pipeTool = new PipeTool();
            pipeTool.setProject(project);
            pipeTool.setConsole(console);
            pipeTool.setLineHandler(linehandler);

            // First we build the resources...
            monitor.setTaskName("Assembling resources");
            MoSyncResourceBuilderVisitor resourceVisitor = new MoSyncResourceBuilderVisitor();

            IPath resource = getResourceOutputPath(project, targetProfile, isFinalizerBuild);
            resourceVisitor.setProject(project);
            resourceVisitor.setPipeTool(pipeTool);
            resourceVisitor.setOutputFile(resource);
            resourceVisitor.setDependencyProvider(compilerVisitor.getDependencyProvider());
            resourceVisitor.setDelta(deltas);
            resourceVisitor.incrementalCompile(monitor, mosyncProject.getDependencyManager());

            // ...and then the actual code is compiled
            IPath compileDir = getCompileOutputPath(project, targetProfile, isFinalizerBuild);
            monitor.setTaskName(MessageFormat.format("Compiling for {0}", targetProfile));

            compilerVisitor.setProfile(targetProfile);
            compilerVisitor.setProject(project);
            compilerVisitor.setConsole(console);
            compilerVisitor.setExtraCompilerSwitches(mosyncProject.getProperty(EXTRA_COMPILER_SWITCHES));
            Integer gccWarnings = PropertyUtil.getInteger(mosyncProject, GCC_WARNINGS);
            compilerVisitor.setGCCWarnings(gccWarnings == null ? 0 : gccWarnings.intValue());
            compilerVisitor.setOutputPath(compileDir);
            compilerVisitor.setLineHandler(linehandler);
            compilerVisitor.setBuildResult(buildResult);
            compilerVisitor.setDelta(deltas);
            
            compilerVisitor.incrementalCompile(monitor, mosyncProject.getDependencyManager());

            // TODO: This does not really work as expected - but we'll keep it here anyways, since
            // it pretty much does what we want except that deltas do not include resources from other projects.
            IResource[] allAffectedResources = compilerVisitor.getAllAffectedResources();
            Set<IProject> projectDependencies = computeProjectDependencies(monitor, mosyncProject, allAffectedResources);
            DependencyManager<IProject> projectDependencyMgr = CoreMoSyncPlugin.getDefault().getProjectDependencyManager(ResourcesPlugin.getWorkspace());
            projectDependencyMgr.setDependencies(project, projectDependencies);

            epm.reportProblems();
            monitor.worked(1);

            if (monitor.isCanceled()) {
                return buildResult;
            }

            String[] objectFiles = compilerVisitor.getObjectFilesForProject(project);

            int ec = compilerVisitor.getErrorCount();

            monitor.setTaskName(MessageFormat.format("Packaging for {0}", targetProfile));

            IPath program = getProgramOutputPath(project, targetProfile, isFinalizerBuild);
            if (ec == 0) {
                boolean elim = !isLib && PropertyUtil.getBoolean(mosyncProject, DEAD_CODE_ELIMINATION);
                pipeTool.setMode(isLib ? PipeTool.BUILD_LIB_MODE : PipeTool.BUILD_C_MODE);
                pipeTool.setInputFiles(objectFiles);
                IPath libraryOutput = computeLibraryOutput(mosyncProject);
                pipeTool.setOutputFile(isLib ? libraryOutput : program);
                pipeTool.setLibraryPaths(getLibraryPaths(mosyncProject));
                pipeTool.setLibraries(getLibraries(mosyncProject));
                pipeTool.setDeadCodeElimination(elim);
                pipeTool.setCollectStabs(true);

                String[] extraLinkerSwitches = PropertyUtil.getStrings(mosyncProject, EXTRA_LINK_SWITCHES);
                pipeTool.setExtraSwitches(extraLinkerSwitches);

                pipeTool.run();

                if (elim) {
                    PipeTool elimPipeTool = new PipeTool();
                    elimPipeTool.setProject(project);
                    elimPipeTool.setLineHandler(linehandler);
                    elimPipeTool.setNoVerify(true);
                    elimPipeTool.setGenerateSLD(false);
                    elimPipeTool.setMode(PipeTool.BUILD_C_MODE);
                    elimPipeTool.setOutputFile(program);
                    elimPipeTool.setConsole(console);
                    elimPipeTool.setExtraSwitches(extraLinkerSwitches);
                    File rebuildFile = new File(elimPipeTool.getExecDir(), "rebuild.s");
                    elimPipeTool.setInputFiles(new String[] { rebuildFile.getAbsolutePath() });
                    elimPipeTool.run();
                }

                if (!isLib) {
                    // Create "comb" file - program + resources in one. We'll
                    // always
                    // make one, even though no resources present.
                    ArrayList<File> parts = new ArrayList<File>();
                    parts.add(program.toFile());
                    if (resourceVisitor.getResourceFiles().length > 0 && program.toFile().exists() && resource.toFile().exists()) {
                        parts.add(resource.toFile());
                    }

                    IPath programComb = getProgramCombOutputPath(project, targetProfile, isFinalizerBuild);
                    if (parts.size() > 1) {
                    	console.addMessage(MessageFormat.format("Combining {0} into one large file, {1}", Util.join(parts.toArray(), ", "), programComb.toFile()));
                    }
                    Util.mergeFiles(new SubProgressMonitor(monitor, 1), parts.toArray(new File[parts.size()]), programComb.toFile());
                }
            }
            
            // And the icon, finally...
            MoSyncIconBuilderVisitor iconVisitor = new MoSyncIconBuilderVisitor();
            iconVisitor.setProject(project);
            iconVisitor.setConsole(console);
            iconVisitor.setDelta(deltas);
            iconVisitor.incrementalCompile(monitor, mosyncProject.getDependencyManager());

            if (doPack && !isLib) {
                IPackager packager = targetProfile.getPackager();
                packager.setParameter(IS_FINALIZER_BUILD, Boolean.toString(isFinalizerBuild));
                packager.setParameter(USE_DEBUG_RUNTIME_LIBS, Boolean.toString(PropertyUtil.getBoolean(mosyncProject, USE_DEBUG_RUNTIME_LIBS)));

                if (ec == 0) {
                    packager.createPackage(mosyncProject, targetProfile, buildResult);

                    if (buildResult.getBuildResult() == null || !buildResult.getBuildResult().exists()) {
                        throw new IOException(MessageFormat.format("Failed to create package for {0}", targetProfile));
                    }
                }
            }

            monitor.worked(1);

            console.addMessage("Build finished at "
                    + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(Calendar.getInstance().getTime()));

            project.refreshLocal(IProject.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
            
            buildResult.setSuccess(true);
            return buildResult;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
        } finally {
            epm.reportProblems();
        }
 }

    /**
     * 'Prepares' a console for printing -- first checks
     * whether the CDT preference is set to clear the console,
     * and if so, clears it.
     * @param console
     */
	private void prepareConsole(IProcessConsole console) {
		if (CoreMoSyncPlugin.isHeadless()) {
			// No need to prepare anything.
			return;
		}
		
		console.prepare();		
	}

	private Set<IProject> computeProjectDependencies(IProgressMonitor monitor, MoSyncProject mosyncProject, IResource[] allAffectedResources) {
    	IProject project = mosyncProject.getWrappedProject();
        monitor.setTaskName(MessageFormat.format("Computing project dependencies for {0}", project.getName()));
        DependencyManager<IProject> projectDependencies = CoreMoSyncPlugin.getDefault().getProjectDependencyManager(ResourcesPlugin.getWorkspace());
        projectDependencies.clearDependencies(project);
        HashSet<IProject> allProjectDependencies = new HashSet<IProject>();
    	Set<IResource> dependencies = mosyncProject.getDependencyManager().getDependenciesOf(Arrays.asList(allAffectedResources));
       	for (IResource resourceDependency : dependencies) {
       		if (resourceDependency.getType() != IResource.ROOT) {
       			allProjectDependencies.add(resourceDependency.getProject());
       		}
        }
       	
       	// No deps on self
       	allProjectDependencies.remove(project);
       	
       	if (CoreMoSyncPlugin.getDefault().isDebugging()) {
       		CoreMoSyncPlugin.trace(MessageFormat.format("Computed project dependencies. Project {0} depends on {1}", project.getName(), allProjectDependencies));
       	}
       	return allProjectDependencies;
	}

    /*private boolean hasDeltaThatAffectsBuild(MoSyncProject project, IResourceDelta[] deltas) throws CoreException {
    	for (int i = 0; i < deltas.length; i++) {
    		if (hasDeltaThatAffectsBuild(project, deltas[i])) {
    			return true;
    		}
    	}
    	
    	return false;
	}*/

/*	private boolean hasDeltaThatAffectsBuild(final MoSyncProject project, IResourceDelta delta) throws CoreException {
        // A delta must contain at least one c source/header file
		// or be a dependency of a resource file
		
        final boolean[] hasSourceDelta = new boolean[1];
        hasSourceDelta[0] = false;
        
        delta.accept(new IResourceDeltaVisitor() {
            public boolean visit(IResourceDelta delta) throws CoreException {
                IResource resource = delta.getResource();
                if (MoSyncBuilderVisitor.doesResourceAffectBuild(resource)) {
                	hasSourceDelta[0] = true;
                } else {
                	hasSourceDelta[0] = project.getDependencyManager().getReverseDependenciesOf(resource, 1).contains();
                }

                return !hasSourceDelta[0];
            }            
        });
        
        return hasSourceDelta[0];
    }*/    
    
    public static IPath computeDefaultLibraryOutput(MoSyncProject mosyncProject) {
        IProject project = mosyncProject.getWrappedProject();
        return getOutputPath(project).append(project.getName() + ".lib"); 
    }
    
    public static IPath computeLibraryOutput(MoSyncProject mosyncProject) {
        String libraryOutputSetting = mosyncProject.getProperty(MoSyncBuilder.OUTPUT_PATH);
        IPath libraryOutput = (libraryOutputSetting == null || libraryOutputSetting.length() == 0) ? computeDefaultLibraryOutput(mosyncProject) : new Path(libraryOutputSetting);
        return libraryOutput;
    }
    
    /**
     * Clears all C markers of the given resource
     * @param resource
     * @return true if at least one marker was cleared, false otherwise
     * @throws CoreException
     */
    public static boolean clearCMarkers(IResource resource) throws CoreException {
    	if (!resource.exists()) {
    		return false;
    	}
    	
		IMarker[] markers = resource.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		resource.getWorkspace().deleteMarkers(markers);    		
		return markers.length > 0;
    }

    private ErrorParserManager createErrorParserManager(IProject project) {
        String epId = CCorePlugin.PLUGIN_ID + ".GCCErrorParser";
        IErrorParser[] gccEp = CCorePlugin.getDefault().getErrorParser(epId);
        if (gccEp.length < 1) {
            return null; // No error parser for gcc available.
        } else {
            return new ErrorParserManager(project, this, new String[] { epId });
        }
    }

    public static IPath[] getIncludePaths(MoSyncProject project) {
        ArrayList<IPath> result = new ArrayList<IPath>();
        if (!PropertyUtil.getBoolean(project, IGNORE_DEFAULT_INCLUDE_PATHS)) {
            result.addAll(Arrays.asList(MoSyncTool.getDefault().getMoSyncDefaultIncludes()));
            result.addAll(Arrays.asList(getProfileIncludes(project.getTargetProfile())));
        }
        
        IPath[] additionalIncludePaths = PropertyUtil.getPaths(project, ADDITIONAL_INCLUDE_PATHS);
        for (int i = 0; i < additionalIncludePaths.length; i++) {
            if (additionalIncludePaths[i].getDevice() == null) {
                // Then might be project relative path.
                IPath relativeAdditionalIncludePath = project.getWrappedProject().getLocation().append(additionalIncludePaths[i]); 
                if (relativeAdditionalIncludePath.toFile().exists()) {
                    additionalIncludePaths[i] = relativeAdditionalIncludePath;
                }
            }
        }

        if (additionalIncludePaths != null) {
            result.addAll(Arrays.asList(additionalIncludePaths));
        }

        return result.toArray(new IPath[0]);
    }
    
    private static IPath[] getProfileIncludes(IProfile profile) {
    	IPath profilePath = MoSyncTool.getDefault().getProfilePath(profile);
        return profilePath == null ? new IPath[0] : new IPath[] { profilePath };
    }


    public static IPath[] getLibraryPaths(MoSyncProject project) {
        ArrayList<IPath> result = new ArrayList<IPath>();
        if (!PropertyUtil.getBoolean(project, IGNORE_DEFAULT_LIBRARY_PATHS)) {
            result.addAll(Arrays.asList(MoSyncTool.getDefault().getMoSyncDefaultLibraryPaths()));
        }

        IPath[] additionalLibraryPaths = PropertyUtil.getPaths(project, ADDITIONAL_LIBRARY_PATHS);

        if (additionalLibraryPaths != null) {
            result.addAll(Arrays.asList(additionalLibraryPaths));
        }

        return result.toArray(new IPath[0]);
    }

    public static IPath[] getLibraries(MoSyncProject project) {
        // Ehm, I think I've seen this code elsewhere...
        ArrayList<IPath> result = new ArrayList<IPath>();
        if (!PropertyUtil.getBoolean(project, IGNORE_DEFAULT_LIBRARIES)) {
            result.addAll(Arrays.asList(MoSyncTool.getDefault().getMoSyncDefaultLibraries()));
        }

        IPath[] additionalLibraries = PropertyUtil.getPaths(project, ADDITIONAL_LIBRARIES);

        if (additionalLibraries != null) {
            result.addAll(Arrays.asList(additionalLibraries));
        }

        return result.toArray(new IPath[0]);
    }

    private IDependencyProvider<IResource> createDependencyProvider(MoSyncBuilderVisitor mbv) {
    	return new CompoundDependencyProvider<IResource>(new GCCDependencyProvider(mbv),
    				new ProjectResourceDependencyProvider(),
    				new ResourceFileDependencyProvider());    	    	
    }
    
    public static boolean isBuilderPreference(String preferenceKey) {
        return preferenceKey != null && preferenceKey.startsWith(BUILD_PREFS_PREFIX);
    }

	public static IProject getProject(ILaunchConfiguration launchConfig) throws CoreException {
        String projectName = launchConfig.getAttribute(ILaunchConstants.PROJECT, "");
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (!project.exists()) {
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
                    MessageFormat.format("Cannot launch: Project {0} does not exist", project.getName())));
        }
        
        return project;
	}

}
