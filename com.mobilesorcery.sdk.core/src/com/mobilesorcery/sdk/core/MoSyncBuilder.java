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
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IErrorParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.resources.ACBuilder;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.LineReader.LineAdapter;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.IBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.stats.CounterVariable;
import com.mobilesorcery.sdk.core.stats.Stats;
import com.mobilesorcery.sdk.core.stats.Variables;
import com.mobilesorcery.sdk.internal.BuildSession;
import com.mobilesorcery.sdk.internal.BuildState;
import com.mobilesorcery.sdk.internal.PipeTool;
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
 * @author Mattias Bybro
 *
 */
public class MoSyncBuilder extends ACBuilder {

	public static final String OUTPUT = "Output";

	public final static String ID = CoreMoSyncPlugin.PLUGIN_ID + ".builder";

	public static final String COMPATIBLE_ID = "com.mobilesorcery.sdk.builder.builder";

	public static final String CONSOLE_ID = "com.mobilesorcery.build.console";

	/**
	 * The prefix used by property initializers.
	 */
	public static final String BUILD_PREFS_PREFIX = "build.prefs:";

	public final static String ADDITIONAL_INCLUDE_PATHS = BUILD_PREFS_PREFIX
			+ "additional.include.paths";

	public final static String IGNORE_DEFAULT_INCLUDE_PATHS = BUILD_PREFS_PREFIX
			+ "ignore.default.include.paths";

	public final static String ADDITIONAL_LIBRARY_PATHS = BUILD_PREFS_PREFIX
			+ "additional.library.paths";

	public final static String IGNORE_DEFAULT_LIBRARY_PATHS = BUILD_PREFS_PREFIX
			+ "ignore.default.library.paths";

	public static final String DEFAULT_LIBRARIES = BUILD_PREFS_PREFIX
			+ "default.libraries";

	public final static String ADDITIONAL_LIBRARIES = BUILD_PREFS_PREFIX
			+ "additional.libraries";

	public final static String IGNORE_DEFAULT_LIBRARIES = BUILD_PREFS_PREFIX
			+ "ignore.default.libraries";

	public static final String LIB_OUTPUT_PATH = BUILD_PREFS_PREFIX
			+ "lib.output.path";

	public static final String APP_OUTPUT_PATH = BUILD_PREFS_PREFIX
			+ "app.output.path";

	public static final String DEAD_CODE_ELIMINATION = BUILD_PREFS_PREFIX
			+ "dead.code.elim";

	public static final String EXTRA_LINK_SWITCHES = BUILD_PREFS_PREFIX
			+ "extra.link.sw";

	public static final String EXTRA_RES_SWITCHES = BUILD_PREFS_PREFIX
			+ "extra.res.sw";

	public static final String PROJECT_TYPE = BUILD_PREFS_PREFIX
			+ "project.type";

	public static final String PROJECT_TYPE_APPLICATION = "app";

	public static final String PROJECT_TYPE_LIBRARY = "lib";

	public static final String EXTRA_COMPILER_SWITCHES = BUILD_PREFS_PREFIX
			+ "gcc.switches";

	public static final String GCC_WARNINGS = BUILD_PREFS_PREFIX
			+ "gcc.warnings";

	public static final String MEMORY_PREFS_PREFIX = BUILD_PREFS_PREFIX
			+ "memory.";

	public static final String MEMORY_HEAPSIZE_KB = MEMORY_PREFS_PREFIX
			+ "heap";

	public static final String MEMORY_STACKSIZE_KB = MEMORY_PREFS_PREFIX
			+ "stack";

	public static final String MEMORY_DATASIZE_KB = MEMORY_PREFS_PREFIX
			+ "data";

	public static final String USE_DEBUG_RUNTIME_LIBS = BUILD_PREFS_PREFIX
			+ "runtime.debug";

	public static final String USE_STATIC_RECOMPILATION = BUILD_PREFS_PREFIX
			+ "output.static.recompilation";

	public static final String PROJECT_VERSION = BUILD_PREFS_PREFIX
			+ "app.version";

	public static final String APP_NAME = BUILD_PREFS_PREFIX + "app.name";
	
	public static final String REBUILD_ON_ERROR = BUILD_PREFS_PREFIX + "rebuild.on.error";

	private static final String APP_CODE = "app.code";

	private static final String CONSOLE_PREPARED = "console.prepared";

	public static final int GCC_WALL = 1 << 1;

	public static final int GCC_WEXTRA = 1 << 2;

	public static final int GCC_WERROR = 1 << 3;

	private static Boolean doesRescompilerLibExist;

	public final class GCCLineHandler extends LineAdapter {

		private final ErrorParserManager epm;

		public GCCLineHandler(ErrorParserManager epm) {
			this.epm = epm;
		}

		private String aggregateLine = "";

		@Override
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

		@Override
		public void stop(IOException e) {
			newLine("");
			/*
			 * if (aggregateLine.length() > 0) { reportLine(aggregateLine); }
			 */
		}

		public void reset() {
			stop(null);
			aggregateLine = "";
		}
	}

	public MoSyncBuilder() {
	}

	/**
	 * <p>
	 * Returns a fragment/prefix/suffix that may be used in file names, based on
	 * the specifiers of a given variant.
	 * </p>
	 *
	 * @see {@link IBuildVariant#getSpecifiers()}
	 * @param variant
	 * @return The same set of specifiers will always return the same string.
	 *         (Usually in some alphabetical order). Only the <b>values</b> of
	 *         the specifiers will be used to produce the string.
	 */
	public static String getSpecifierPathFragment(IBuildVariant variant) {
		SortedMap<String, String> specifiers = variant.getSpecifiers();
		StringBuffer result = new StringBuffer();
		if (specifiers != null) {
			TreeSet<String> sortedSpecifiers = new TreeSet<String>(
					String.CASE_INSENSITIVE_ORDER);
			for (Map.Entry<String, String> specifier : specifiers.entrySet()) {
				if (specifier.getValue().length() > 0) {
					sortedSpecifiers.add(specifier.getValue());
				}
			}
			result.append(Util.join(sortedSpecifiers.toArray(), "_"));
		}
		return result.toString();
	}

	/**
	 * Determines whether a resource is part of the output directory tree
	 *
	 * @param project
	 * @param res
	 * @return
	 */
	public static boolean isInOutput(IProject project, IResource res) {
		IPath projectRelativePath = res.getProjectRelativePath();
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		String outputFolder = IsReleasePackageTester
				.getReleasePackageFolder(mosyncProject);
		boolean isOutputFolder = outputFolder != null
				&& new Path(outputFolder).isPrefixOf(projectRelativePath);
		boolean isLegacyOutputFolder = new Path(OUTPUT)
				.isPrefixOf(projectRelativePath);
		return isLegacyOutputFolder || isOutputFolder;
	}

	/**
	 * Converts a project-relative path to an absolute path.
	 *
	 * @param path
	 * @return An absolute path
	 */
	public static IPath toAbsolute(IPath root, String pathStr) {
		Path path = new Path(pathStr);
		return root.append(path);
	}

	private static IPath getFinalOutputPath(IProject project,
			IBuildVariant variant) {
		IProfile targetProfile = variant.getProfile();
		String outputPath = getPropertyOwner(MoSyncProject.create(project),
				variant.getConfigurationId()).getProperty(APP_OUTPUT_PATH);
		if (outputPath == null) {
			throw new IllegalArgumentException("No output path specified");
		}

		String variantPath = targetProfile.getName();
		String specifierSuffix = getSpecifierPathFragment(variant);
		if (!Util.isEmpty(specifierSuffix)) {
			variantPath += "_" + specifierSuffix;
		}
		return toAbsolute(project.getLocation().append(OUTPUT), outputPath)
				.append(targetProfile.getVendor().getName())
				.append(variantPath);
	}

	public static IPath getOutputPath(IProject project, IBuildVariant variant) {
		return getFinalOutputPath(project, variant);
	}

	public static IPath getProgramOutputPath(IProject project,
			IBuildVariant variant) {
		return getOutputPath(project, variant).append("program");
	}

	public static IPath getProgramCombOutputPath(IProject project,
			IBuildVariant variant) {
		return getOutputPath(project, variant).append("program.comb");
	}

	public static IPath getResourceOutputPath(IProject project,
			IBuildVariant variant) {
		return getOutputPath(project, variant).append("resources");
	}

	public static IPath getPackageOutputPath(IProject project,
			IBuildVariant variant) {
		return getFinalOutputPath(project, variant).append("package");
	}

	public static String getExtraCompilerSwitches(MoSyncProject project)
			throws ParameterResolverException {
		IBuildVariant variant = getActiveVariant(project);
		ParameterResolver resolver = createParameterResolver(project, variant);
		IPropertyOwner buildProperties = getPropertyOwner(project,
				variant.getConfigurationId());
		return Util.replace(buildProperties
				.getProperty(MoSyncBuilder.EXTRA_COMPILER_SWITCHES), resolver);
	}

	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		IProject project = getProject();

		if (MoSyncProject.NULL_DEPENDENCY_STRATEGY == PropertyUtil.getInteger(
				MoSyncProject.create(project),
				MoSyncProject.DEPENDENCY_STRATEGY,
				MoSyncProject.GCC_DEPENDENCY_STRATEGY)) {
			// TODO: At this point, we only have a GCC dependency strategy and a
			// "always full build" strategy
			kind = FULL_BUILD;
		}

		IBuildVariant variant = getActiveVariant(MoSyncProject.create(project));
		IBuildSession session = createIncrementalBuildSession(project, kind);
		if (kind == FULL_BUILD) {
			build(project, session, variant, null, monitor);
		} else {
			incrementalBuild(project, session, variant, null, monitor);
		}

		Set<IProject> dependencies = CoreMoSyncPlugin.getDefault()
				.getProjectDependencyManager(ResourcesPlugin.getWorkspace())
				.getDependenciesOf(project);
		dependencies.add(project);

		return dependencies.toArray(new IProject[dependencies.size()]);
	}

	private boolean hasErrorMarkers(IProject project) throws CoreException {
		return hasErrorMarkers(project, IResource.DEPTH_INFINITE);
	}

	private boolean hasErrorMarkers(IProject project, int depth)
			throws CoreException {
		return project.findMaxProblemSeverity(
				ICModelMarker.C_MODEL_PROBLEM_MARKER, true, depth) == IMarker.SEVERITY_ERROR;
	}

	@Override
	public void clean(IProgressMonitor monitor) {
		forgetLastBuiltState();
		IProject project = getProject();
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		IBuildVariant variant = getActiveVariant(mosyncProject);
		clean(project, variant, monitor);
	}

	public void clean(IProject project, IBuildVariant variant,
			IProgressMonitor monitor) {
		IPath output = getOutputPath(project, variant);
		File outputFile = output.toFile();

		IProcessConsole console = CoreMoSyncPlugin.getDefault().createConsole(
				CONSOLE_ID);
		prepareConsole(null, console);

		// We use a null monitor to avoid overloading the UI thread.
		IProgressMonitor subMonitor = new NullProgressMonitor();

		console.addMessage(createBuildMessage("Cleaning",
				MoSyncProject.create(project), variant));
		Util.deleteFiles(getPackageOutputPath(project, variant).toFile(), null,
				512, subMonitor);
		Util.deleteFiles(getProgramOutputPath(project, variant).toFile(), null,
				1, subMonitor);
		Util.deleteFiles(getProgramCombOutputPath(project, variant).toFile(),
				null, 1, subMonitor);
		Util.deleteFiles(getResourceOutputPath(project, variant).toFile(),
				null, 1, subMonitor);
		Util.deleteFiles(outputFile, Util.getExtensionFilter("s"), 512, subMonitor);

		IBuildState buildState = MoSyncProject.create(project).getBuildState(
				variant);
		buildState.clear();
		buildState.save();
		buildState.setValid(true);
	}

	/**
	 * Perfoms a full build of a project
	 *
	 * @param project
	 * @param variant
	 * @param doClean
	 * @param doBuildResources
	 * @param doPack
	 * @param resourceFilter
	 * @param doLink
	 * @param saveDirtyEditors
	 * @param monitor
	 * @return
	 * @throws CoreException
	 * @throws {@link OperationCanceledException} If the operation was canceled
	 */
	public IBuildResult build(IProject project, IBuildSession session,
			IBuildVariant variant, IFilter<IResource> resourceFilter,
			IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		try {
			// TODO: Allow for setting build config explicitly!
			monitor.beginTask(MessageFormat.format("Full build of {0}",
					project.getName()), 8);
			if (session.doClean()) {
				clean(project, variant, new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}

			if (!session.doClean() && session.doSaveDirtyEditors()) {
				if (!saveAllEditors(project)) {
					throw new OperationCanceledException();
				}
			}

			return incrementalBuild(project, session, variant, resourceFilter,
					monitor);
		} finally {
			monitor.done();
		}
	}

	IBuildResult build(IProject project, IResourceDelta[] ignoreMe,
			IBuildSession session, IBuildVariant variant,
			IFilter<IResource> resourceFilter, IProgressMonitor monitor) {
		return null;
	}

	/**
	 * Returns the pipe tool mode that should be used for the given kind of
	 * input.
	 *
	 * @param profile
	 *            The profile we want to use pipe-tool for.
	 * @param isLib
	 *            Indicates whether we are building a library or not.
	 * @return The appropriate mode for the given profile.
	 * @throws CoreException
	 */
	public static String getPipeToolMode(MoSyncProject project,
			IProfile profile, boolean isLib) throws CoreException {
		if (isLib) {
			return PipeTool.BUILD_LIB_MODE;
		}

		if (project.getProfileManagerType() == MoSyncTool.LEGACY_PROFILE_TYPE) {
			Map<String, Object> properties = profile.getProperties();
			if (properties.containsKey("MA_PROF_OUTPUT_CPP")) {
				return PipeTool.BUILD_GEN_CPP_MODE;
			} else if (properties.containsKey("MA_PROF_OUTPUT_JAVA")) {
				return PipeTool.BUILD_GEN_JAVA_MODE;
			} else if (properties.containsKey("MA_PROF_OUTPUT_CS")) {
				return PipeTool.BUILD_GEN_CS_MODE;
			}
		}

		// The default mode.
		return profile.getPackager().getGenerateMode(profile);
	}

	IBuildResult incrementalBuild(IProject project, IBuildSession session,
			IBuildVariant variant, IFilter<IResource> resourceFilter,
			IProgressMonitor monitor) throws CoreException {
		IProcessConsole console = createConsole(session);
		IBuildResult result = incrementalBuild0(project, session, variant, resourceFilter, console, monitor);
		if (monitor.isCanceled()) {
			console.addMessage(IProcessConsole.ERR, "*** Build was cancelled by user ***");
		}
		return result;
	}
	
	IBuildResult incrementalBuild0(IProject project, IBuildSession session,
			IBuildVariant variant, IFilter<IResource> resourceFilter,
			IProcessConsole console,
			IProgressMonitor monitor) throws CoreException {
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Building project {0}", project);
		}

		BuildResult buildResult = new BuildResult(project);
		buildResult.setVariant(variant);
		Calendar timestamp = Calendar.getInstance();
		buildResult.setTimestamp(timestamp.getTimeInMillis());

		MoSyncProject mosyncProject = MoSyncProject.create(project);
		IBuildState buildState = mosyncProject.getBuildState(variant);

		// STATS.
		Variables vars = Stats.getStats().getVariables();
		vars.get(CounterVariable.class, "builds").inc();
		String templateId = mosyncProject
				.getProperty(MoSyncProject.TEMPLATE_ID);
		if (templateId != null) {
			vars.get(CounterVariable.class, "builds-template-" + templateId)
					.inc();
		}
		if (mosyncProject.getProfileManagerType() == MoSyncTool.DEFAULT_PROFILE_TYPE) {
			String family = variant.getProfile().getVendor().getName();
			vars.get(CounterVariable.class, "builds-platform-" + family).inc();
		}

		ParameterResolver resolver = createParameterResolver(mosyncProject,
				variant);

		ensureOutputIsMarkedDerived(project, variant);

		ErrorParserManager epm = createErrorParserManager(project);

		CoreException errorToShowInConsole = null;

		try {
			/* Set up build monitor */
			monitor.beginTask(MessageFormat.format("Building {0}", project), 4);
			monitor.setTaskName("Clearing old problem markers");

			if (session.doLink() && !session.doBuildResources()) {
				throw new CoreException(
						new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
								"If resource building is suppressed, then linking should also be."));
			}

			// And we only remove things that are on the project.
			IFileTreeDiff diff = createDiff(buildState, session);
			if (PropertyUtil.getBoolean(mosyncProject, REBUILD_ON_ERROR) &&
					hasErrorMarkers(project)) {
				// Build all files
				console.addMessage(IProcessConsole.ERR, "*** Errors in previous build triggered full rebuild ***");
				diff = null;
			}

			if (diff == null || !buildState.isValid()) {
				buildState.getDependencyManager().clear();
			}

			buildResult.setDependencyDelta(buildState.getDependencyManager()
					.createDelta());

			IPropertyOwner buildProperties = MoSyncBuilder.getPropertyOwner(
					mosyncProject, variant.getConfigurationId());

			DateFormat dateFormater = DateFormat.getDateTimeInstance(
					DateFormat.SHORT, DateFormat.LONG);
			console.addMessage("Build started at "
					+ dateFormater.format(timestamp.getTime()));
			console.addMessage(createBuildMessage("Building", mosyncProject,
					variant));

			GCCLineHandler linehandler = new GCCLineHandler(epm);

			/* Set up pipe-tool */
			PipeTool pipeTool = new PipeTool();
			pipeTool.setAppCode(getCurrentAppCode(session));
			pipeTool.setProject(project);
			pipeTool.setVariant(variant);
			pipeTool.setConsole(console);
			pipeTool.setLineHandler(linehandler);
			pipeTool.setArguments(buildProperties);

			IDependencyProvider<IResource> dependencyProvider = createDependencyProvider(
					mosyncProject, variant);

			boolean requiresPrivilegedAccess = requiresPrivilegedAccess(mosyncProject);
			if (requiresPrivilegedAccess) {
				PrivilegedAccess.getInstance().assertAccess(mosyncProject);
			}

			BuildSequence sequence = new BuildSequence(mosyncProject);
			List<IBuildStep> buildSteps = sequence.getBuildSteps(session);

			monitor.beginTask("Build", buildSteps.size());

			sequence.assertValid(session);

			int continueFlag = IBuildStep.CONTINUE;

			for (IBuildStep buildStep : buildSteps) {
				long startTime = System.currentTimeMillis();
				if (monitor.isCanceled()) {
					return buildResult;
				}

				console.addMessage(IProcessConsole.MESSAGE, MessageFormat.format("Performing build step ''{0}''", buildStep.getName()));

				if (continueFlag != IBuildStep.SKIP
						&& buildStep.shouldBuild(mosyncProject, session,
								buildResult)) {
					if (CoreMoSyncPlugin.getDefault().isDebugging()) {
						CoreMoSyncPlugin.trace(
								"Performing build step {0} for project {1}",
								buildStep.getName(), buildResult.getProject()
										.getName());
					}
					buildStep.initConsole(console);
					buildStep.initBuildProperties(buildProperties);
					buildStep.initBuildState(buildState);
					buildStep.initPipeTool(pipeTool);
					buildStep.initParameterResolver(resolver);
					buildStep.initDefaultLineHandler(linehandler);
					buildStep.initDependencyProvider(dependencyProvider);
					buildStep.initResourceFilter(resourceFilter);
					continueFlag = buildStep.incrementalBuild(mosyncProject,
							session, variant, diff, buildResult, monitor);
					if (continueFlag == IBuildStep.SKIP) {
						console.addMessage(MessageFormat
								.format("Was told by build step {0} to skip the remaining build steps. Build successful.",
										buildStep.getName()));
					}
					console.addMessage(MessageFormat.format("Time for buildstep {0}: {1}.", buildStep.getName(), Util.elapsedTime(System.currentTimeMillis() - startTime)));
				}
				monitor.worked(1);
			}

			CoreException buildError = buildResult.createException();
			if (buildError != null) {
				throw buildError;
			}

			// Update the current set of dependencies.
			buildState.getDependencyManager().applyDelta(
					buildResult.getDependencyDelta());
			
			Date endTimestamp = Calendar.getInstance().getTime();
			console.addMessage(MessageFormat.format("Build finished at {0}. (Build time: {1}.)",
					dateFormater.format(endTimestamp),
					Util.elapsedTime(endTimestamp.getTime() - timestamp.getTime().getTime())));

			refresh(project);

			buildResult.setSuccess(true);

			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace("Changed dependencies:\n{0}",
						buildResult.getDependencyDelta());
				CoreMoSyncPlugin.trace("Current set of dependencies:\n{0}",
						buildState.getDependencyManager());
			}

		} catch (OperationCanceledException e) {
			// That's ok, why? MB 11-01-10: Because :)
			return buildResult;
		} catch (CoreException e) {
			errorToShowInConsole = e;
		} catch (Exception e) {
			errorToShowInConsole = new CoreException(new Status(IStatus.ERROR,
					CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
		} finally {
			epm.reportProblems();
			if (!monitor.isCanceled() && !buildResult.success()
					&& !hasErrorMarkers(project)) {
				addBuildFailedMarker(project);
			} else if (buildResult.success()) {
				clearCMarkers(project);
			}

			saveBuildState(buildState, mosyncProject, buildResult);

			if (errorToShowInConsole != null) {
				console.addMessage(IProcessConsole.ERR, errorToShowInConsole.getMessage());
				throw errorToShowInConsole;
			}
		}
		return buildResult;
	}

	public static void refresh(IResource resource) throws CoreException {
		if (!CoreMoSyncPlugin.isHeadless()) {
			resource.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
		}
	}

	public static boolean requiresPrivilegedAccess(MoSyncProject mosyncProject) {
		BuildSequence seq = new BuildSequence(mosyncProject);
		return requiresPrivilegedAccess(seq);
	}

	public static boolean requiresPrivilegedAccess(BuildSequence seq) {
		List<IBuildStepFactory> factories = seq.getBuildStepFactories();
		for (IBuildStepFactory factory : factories) {
			if (factory.requiresPrivilegedAccess()) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasBuildState(IResource location) {
		return BuildState.hasBuildState(location);
	}

	public static IBuildState parseBuildState(IResource location) {
		return BuildState.parseBuildState(location);
	}

	private IDependencyProvider<IResource> createDependencyProvider(
			MoSyncProject mosyncProject, IBuildVariant variant) {
		CompoundDependencyProvider<IResource> dependencyProvider = new CompoundDependencyProvider<IResource>(
				new GCCDependencyProvider(mosyncProject, variant),
				new ProjectResourceDependencyProvider(mosyncProject
						.getWrappedProject(), variant),
				new ResourceFileDependencyProvider(mosyncProject, variant));

		return dependencyProvider;
	}

	/**
	 * Creates a {@link ParameterResolver} for a project
	 *
	 * @param project
	 * @param variant
	 *            The variant to create the resolver for, or <code>null</code>
	 *            for the active, non-finalizing variant.
	 * @return
	 */
	public static ParameterResolver createParameterResolver(
			MoSyncProject project, IBuildVariant variant) {
		return MoSyncProjectParameterResolver.create(project, variant);
	}

	public static IPath[] resolvePaths(IPath[] paths, ParameterResolver resolver)
			throws ParameterResolverException {
		// TODO: Consider moving this method
		IPath[] result = new IPath[paths.length];
		for (int i = 0; i < paths.length; i++) {
			result[i] = new Path(Util.replace(paths[i].toString(), resolver));
		}
		return result;
	}

	public static String getCurrentAppCode(IBuildSession session) {
		// TODO: This will result in *most* equivalent apps to share
		// app code across devices; in particular finalization will always
		// share app code.
		String appCode = (String) session.getProperties().get(APP_CODE);
		if (Util.isEmpty(appCode)) {
			appCode = PipeTool.generateAppCode();
			session.getProperties().put(APP_CODE, appCode);
		}

		return appCode;
	}

	private String createBuildMessage(String buildType, MoSyncProject project,
			IBuildVariant variant) {
		StringBuffer result = new StringBuffer();
		result.append(MessageFormat.format("{0}: Project {1} for profile {2}",
				buildType, project.getName(), variant.getProfile()));
		if (project.areBuildConfigurationsSupported()
				&& variant.getConfigurationId() != null) {
			result.append(MessageFormat.format("\nConfiguration: {0}",
					variant.getConfigurationId()));
		}

		return result.toString();
	}

	// returns null if a full build should be performed.
	private IFileTreeDiff createDiff(IBuildState buildState,
			IBuildSession session) throws CoreException {
		Set<String> changedProperties = buildState.getChangedBuildProperties();

		if (session.doClean() || buildState.fullRebuildNeeded()) {
			// Always full build after clean.
			return null;
		}

		// Also, if the variant we want to build does not match what was
		// previously built here,
		// then we'll do a full rebuild.
		IBuildResult previousResult = buildState.getBuildResult();
		if (previousResult == null
				|| !Util.equals(buildState.getBuildVariant(),
						previousResult.getVariant())) {
			return null;
		}

		if (changedProperties.isEmpty()) {
			return buildState.createDiff();
		} else {
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin
						.trace("Changed build properties, full rebuild required. Changed properties: {0}",
								changedProperties);
			}
			return null;
		}

	}

	private void ensureOutputIsMarkedDerived(IProject project,
			IBuildVariant variant) throws CoreException {
		ensureFolderIsMarkedDerived(project.getFolder(OUTPUT));

		IPath outputPath = getOutputPath(project, variant);
		IContainer[] outputFolder = project.getWorkspace().getRoot()
				.findContainersForLocation(outputPath);
		for (int i = 0; i < outputFolder.length; i++) {
			ensureFolderIsMarkedDerived((IFolder) outputFolder[i]);
		}
	}

	private static void ensureFolderExists(IFolder folder) throws CoreException {
		if (!folder.exists()) {
			if (!folder.getParent().exists()
					&& folder.getParent().getType() == IResource.FOLDER) {
				ensureFolderExists((IFolder) folder.getParent());
			}
			folder.create(true, true, new NullProgressMonitor());
		}
	}

	public static void ensureFolderIsMarkedDerived(IFolder folder)
			throws CoreException {
		ensureFolderExists(folder);

		if (!folder.isDerived()) {
			folder.setDerived(true);
		}
	}

	private void saveBuildState(IBuildState buildState, MoSyncProject project,
			IBuildResult buildResult) throws CoreException {
		buildState.updateResult(buildResult);
		buildState.updateState(project.getWrappedProject());
		buildState.updateBuildProperties(project.getProperties());
		buildState.fullRebuildNeeded(buildResult == null
				|| !buildResult.success());
		buildState.save();
		buildState.setValid(true);
	}

	private void addBuildFailedMarker(IProject project) throws CoreException {
		// Ensure there is a build failed marker if the build failed; will cause
		// all failed builds to
		// be completely rebuilt later.
		IMarker marker = project
				.createMarker(ICModelMarker.C_MODEL_PROBLEM_MARKER);
		marker.setAttribute(IMarker.MESSAGE, "Last build failed");
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
	}

	public static IPropertyOwner getPropertyOwner(MoSyncProject mosyncProject,
			String configId) {
		return getPropertyOwner(mosyncProject,
				mosyncProject.getBuildConfiguration(configId));
	}

	static IPropertyOwner getPropertyOwner(MoSyncProject mosyncProject,
			IBuildConfiguration config) {
		return mosyncProject.areBuildConfigurationsSupported()
				&& config != null ? config.getProperties() : mosyncProject;
	}

	/**
	 * 'Prepares' a console for printing -- first checks whether the CDT
	 * preference is set to clear the console, and if so, clears it.
	 *
	 * @param console
	 */
	private void prepareConsole(IBuildSession session, IProcessConsole console) {
		boolean needsPreparing = !CoreMoSyncPlugin.isHeadless();
		needsPreparing &= (session != null && session.getProperties().get(
				CONSOLE_PREPARED) == null);

		if (needsPreparing) {
			console.prepare();
			if (session != null) {
				session.getProperties().put(CONSOLE_PREPARED,
						Boolean.TRUE.toString());
			}
		}
	}

	/**
	 * Creates a console that is ready for printing.
	 *
	 * @param session
	 *            The current build session.
	 * @return A console that is ready for printing.
	 */
	private IProcessConsole createConsole(IBuildSession session) {
		IProcessConsole console = CoreMoSyncPlugin.getDefault().createConsole(
				CONSOLE_ID);
		prepareConsole(session, console);

		if (!MoSyncTool.getDefault().isValid()) {
			String error = MoSyncTool.getDefault().validate();
			console.addMessage(MessageFormat.format(
					"MoSync Tool not properly initialized: {0}", error));
			console.addMessage("- go to Window > Preferences > MoSync Tool to set the MoSync home directory");
		}

		return console;
	}

	// TODO: REMOVE!
	protected Set<IProject> computeProjectDependencies(
			IProgressMonitor monitor, MoSyncProject mosyncProject,
			IBuildState buildState, IResource[] allAffectedResources) {
		IProject project = mosyncProject.getWrappedProject();
		monitor.setTaskName(MessageFormat.format(
				"Computing project dependencies for {0}", project.getName()));
		DependencyManager<IProject> projectDependencies = CoreMoSyncPlugin
				.getDefault().getProjectDependencyManager(
						ResourcesPlugin.getWorkspace());
		projectDependencies.clearDependencies(project);
		HashSet<IProject> allProjectDependencies = new HashSet<IProject>();
		Set<IResource> dependencies = buildState.getDependencyManager()
				.getDependenciesOf(Arrays.asList(allAffectedResources));
		for (IResource resourceDependency : dependencies) {
			if (resourceDependency.getType() != IResource.ROOT) {
				allProjectDependencies.add(resourceDependency.getProject());
			}
		}

		// No deps on self
		allProjectDependencies.remove(project);

		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin
					.trace(MessageFormat
							.format("Computed project dependencies. Project {0} depends on {1}",
									project.getName(), allProjectDependencies));
		}
		return allProjectDependencies;
	}

	public static IPath computeLibraryOutput(MoSyncProject mosyncProject,
			IPropertyOwner buildProperties) {
		String outputPath = buildProperties.getProperty(LIB_OUTPUT_PATH);
		if (outputPath == null) {
			throw new IllegalArgumentException("Library path is not specified");
		}

		return toAbsolute(mosyncProject.getWrappedProject().getLocation()
				.append(OUTPUT), outputPath);
	}

	/**
	 * Clears all C markers of the given resource
	 *
	 * @param resource
	 * @param severityError
	 * @return true if at least one marker was cleared, false otherwise
	 * @throws CoreException
	 * @throws CoreException
	 */
	public static boolean clearCMarkers(IResource resource)
			throws CoreException {
		return clearCMarkers(resource, IMarker.SEVERITY_INFO);
	}

	public static boolean clearCMarkers(IResource resource, int severity)
			throws CoreException {
		if (!resource.exists()) {
			return false;
		}

		IMarker[] markers = resource.findMarkers(
				ICModelMarker.C_MODEL_PROBLEM_MARKER, true,
				IResource.DEPTH_INFINITE);
		IMarker[] toBeRemoved = markers;

		if (severity != IMarker.SEVERITY_INFO) {
			ArrayList<IMarker> toBeRemovedList = new ArrayList<IMarker>();
			for (int i = 0; i < markers.length; i++) {
				Object severityOfMarker = markers[i]
						.getAttribute(IMarker.SEVERITY);
				if (severityOfMarker instanceof Integer) {
					if (((Integer) severityOfMarker) >= severity) {
						toBeRemovedList.add(markers[i]);
					}
				}
			}
			toBeRemoved = toBeRemovedList.toArray(new IMarker[0]);
		}

		resource.getWorkspace().deleteMarkers(toBeRemoved);
		return toBeRemoved.length > 0;
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

	public static IPath[] getBaseIncludePaths(MoSyncProject project,
			IBuildVariant variant) throws ParameterResolverException {
		IPropertyOwner buildProperties = getPropertyOwner(project,
				variant.getConfigurationId());

		ArrayList<IPath> result = new ArrayList<IPath>();
		if (!PropertyUtil.getBoolean(buildProperties,
				IGNORE_DEFAULT_INCLUDE_PATHS)) {
			result.addAll(Arrays.asList(MoSyncTool.getDefault()
					.getMoSyncDefaultIncludes()));
		}

		if (project.getProfileManagerType() == MoSyncTool.LEGACY_PROFILE_TYPE) {
			result.addAll(Arrays.asList(MoSyncBuilder
					.getProfileIncludes(variant.getProfile())));
		}

		IPath[] additionalIncludePaths = PropertyUtil.getPaths(buildProperties,
				ADDITIONAL_INCLUDE_PATHS);
		for (int i = 0; i < additionalIncludePaths.length; i++) {
			if (additionalIncludePaths[i].getDevice() == null) {
				// Then might be project relative path.
				IPath relativeAdditionalIncludePath = project
						.getWrappedProject().getLocation()
						.append(additionalIncludePaths[i]);
				if (relativeAdditionalIncludePath.toFile().exists()) {
					additionalIncludePaths[i] = relativeAdditionalIncludePath;
				}
			}
		}

		if (additionalIncludePaths != null) {
			result.addAll(Arrays.asList(additionalIncludePaths));
		}

		return resolvePaths(result.toArray(new IPath[0]),
				createParameterResolver(project, variant));
	}

	public static IPath[] getProfileIncludes(IProfile profile) {
		IPath profilePath = MoSyncTool.getDefault().getProfilePath(profile);
		return profilePath == null ? new IPath[0] : new IPath[] { profilePath };
	}

	public static IPath[] getLibraryPaths(IProject project,
			IPropertyOwner buildProperties) {
		ArrayList<IPath> result = new ArrayList<IPath>();
		if (!PropertyUtil.getBoolean(buildProperties,
				IGNORE_DEFAULT_LIBRARY_PATHS)) {
			result.addAll(Arrays.asList(MoSyncTool.getDefault()
					.getMoSyncDefaultLibraryPaths()));
		}

		IPath[] additionalLibraryPaths = PropertyUtil.getPaths(buildProperties,
				ADDITIONAL_LIBRARY_PATHS);

		if (additionalLibraryPaths != null) {
			result.addAll(Arrays.asList(additionalLibraryPaths));
		}

		return result.toArray(new IPath[0]);
	}

	public static IPath[] getLibraries(MoSyncProject project,
			IBuildVariant variant, IPropertyOwner buildProperties) {
		// Ehm, I think I've seen this code elsewhere...
		ArrayList<IPath> result = new ArrayList<IPath>();

		if (!PropertyUtil.getBoolean(buildProperties, IGNORE_DEFAULT_LIBRARIES)) {
			result.addAll(Arrays.asList(PropertyUtil.getPaths(buildProperties,
					DEFAULT_LIBRARIES)));
		}

		IPath[] additionalLibraries = PropertyUtil.getPaths(buildProperties,
				ADDITIONAL_LIBRARIES);

		if (additionalLibraries != null) {
			result.addAll(Arrays.asList(additionalLibraries));
		}

		return result.toArray(new IPath[0]);
	}

	/*private static boolean doesRescompilerLibExist() {
		if (doesRescompilerLibExist == null) {
			try {
				doesRescompilerLibExist = MoSyncTool.getDefault()
						.getMoSyncDefaultLibraryPaths()[0]
						.append("rescompiler.lib").toFile().exists();
			} catch (Exception e) {
				doesRescompilerLibExist = false;
			}
		}
		return doesRescompilerLibExist;
	}*/

	public static boolean isBuilderPreference(String preferenceKey) {
		return preferenceKey != null
				&& preferenceKey.startsWith(BUILD_PREFS_PREFIX);
	}

	public static IProject getProject(ILaunchConfiguration launchConfig)
			throws CoreException {
		String projectName = launchConfig == null ? null : launchConfig.getAttribute(
				ILaunchConstants.PROJECT, "");
		if (Util.isEmpty(projectName)) {
			return null;
		}
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		if (!project.exists()) {
			throw new CoreException(new Status(IStatus.ERROR,
					CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format(
							"Cannot launch: Project {0} does not exist",
							project.getName())));
		}

		return project;
	}

	public static IRunnableWithProgress createBuildJob(final IProject project,
			final IBuildSession buildSession,
			final List<IBuildVariant> variantsToBuild) {
		return new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				monitor.beginTask(MessageFormat.format("Building {0} variants",
						variantsToBuild.size()), variantsToBuild.size());

				for (IBuildVariant variantToBuild : variantsToBuild) {
					SubProgressMonitor jobMonitor = new SubProgressMonitor(
							monitor, 1);
					if (!monitor.isCanceled()) {
						IRunnableWithProgress buildJob = createBuildJob(
								project, buildSession, variantToBuild);
						buildJob.run(jobMonitor);
					}
				}
			}
		};
	}

	public static IRunnableWithProgress createBuildJob(final IProject project,
			final IBuildSession session, final IBuildVariant variant) {
		return new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				try {
					IBuildResult buildResult = new MoSyncBuilder().build(
							project, session, variant, null, monitor);
					if (!buildResult.success()) {
						throw new InvocationTargetException(buildResult.createException());
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e, variant.getProfile()
							+ ": " + e.getMessage()); //$NON-NLS-1$
				} finally {
					monitor.done();
				}
			}
		};
	}

	public static boolean saveAllEditors(IResource resource) {
		return saveAllEditors(resource, false, false);
	}

	public static boolean saveAllEditors(IResource resource, boolean force,
			boolean confirm) {
		ArrayList<IResource> resourceList = new ArrayList<IResource>();
		resourceList.add(resource);
		return saveAllEditors(resourceList, force, confirm);
	}

	public static boolean saveAllEditors(final List<IResource> resources) {
		return saveAllEditors(resources, false, false);
	}

	public static boolean saveAllEditors(final List<IResource> resources,
			boolean force, final boolean confirm) {
		if (CoreMoSyncPlugin.isHeadless()) {
			return true;
		}

		final boolean doSaveAll = force || CoreMoSyncPlugin.getSavePolicy().isSaveAllSet();
		final boolean[] result = new boolean[1];
		result[0] = true;

		if (doSaveAll) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			if (display != null) {
				display.syncExec(new Runnable() {
					@Override
					public void run() {
						if (!CoreMoSyncPlugin.getSavePolicy().saveAllEditors(
								resources.toArray(new IResource[0]), confirm)) {
							result[0] = false;
						}
					}
				});
			}
		}

		return result[0];
	}

	/**
	 * Returns a build variant that represents the currently active project
	 * settings.
	 *
	 * @param isFinalizerBuild
	 * @return
	 */
	public static IBuildVariant getActiveVariant(MoSyncProject project) {
		IBuildConfiguration cfg = project.getActiveBuildConfiguration();
		String cfgId = project.areBuildConfigurationsSupported() && cfg != null ? cfg
				.getId() : null;

		return new BuildVariant(project.getTargetProfile(), cfgId);
	}

	/**
	 * Creates a {@link IBuildVariant} for finalizing, based on a specified
	 * {@link IProfile} and the project's active build configuration.
	 *
	 * @param project
	 * @param profile
	 * @return The created {@link IBuildVariant}
	 */
	public static IBuildVariant createVariant(MoSyncProject project,
			IProfile profile) {
		IBuildConfiguration cfg = project.getActiveBuildConfiguration();
		return getVariant(project, profile, cfg);
	}

	/**
	 * Creates a {@link IBuildVariant} for finalizing, based on a specified
	 * {@link IProfile} and a specified {@link IBuildConfiguration}.
	 *
	 * @param project
	 * @param profile
	 * @param cfg
	 * @return The created {@link IBuildVariant}
	 */
	public static IBuildVariant getVariant(MoSyncProject project,
			IProfile profile, IBuildConfiguration cfg) {
		String cfgId = project.areBuildConfigurationsSupported() && cfg != null ? cfg
				.getId() : null;
		return new BuildVariant(profile, cfgId);
	}

	public static IBuildSession createFinalizerBuildSession(
			List<IBuildVariant> variants) {
		return new BuildSession(variants, BuildSession.DO_LINK
				| BuildSession.DO_PACK | BuildSession.DO_BUILD_RESOURCES);
	}

	public static IBuildSession createCompileOnlySession(IBuildVariant variant) {
		return new BuildSession(Arrays.asList(variant), 0);
	}

	/**
	 * Creates a build session with all build steps except CLEAN
	 *
	 * @param variant
	 * @return
	 */
	public static IBuildSession createDefaultBuildSession(IBuildVariant variant) {
		return new BuildSession(Arrays.asList(variant), BuildSession.ALL
				- BuildSession.DO_CLEAN);
	}

	public static IBuildSession createCleanBuildSession(IBuildVariant variant) {
		return new BuildSession(Arrays.asList(variant), BuildSession.ALL);
	}

	/**
	 * Returns a build session for incremental builds (ie the kind of build
	 * session created upon calls to the <code>build</code> method).
	 *
	 * @param kind
	 *            The build kind, for example
	 *            <code>IncrementalProjectBuilder.FULL_BUILD</code>.
	 * @return
	 */
	public static IBuildSession createIncrementalBuildSession(IProject project,
			int kind) {
		IBuildVariant variant = getActiveVariant(MoSyncProject.create(project));
		int clean = kind == FULL_BUILD ? BuildSession.DO_CLEAN : 0;
		return new BuildSession(Arrays.asList(variant), clean
				| BuildSession.DO_BUILD_RESOURCES | BuildSession.DO_LINK
				| BuildSession.DO_PACK);
	}

	public static IPath getMetaDataPath(MoSyncProject project,
			IBuildVariant variant) {
		return getOutputPath(project.getWrappedProject(), variant).append(
				".metadata");
	}

	public static boolean isLib(MoSyncProject mosyncProject) {
		return PROJECT_TYPE_LIBRARY.equals(mosyncProject
				.getProperty(PROJECT_TYPE));
	}
	
	public static boolean isResourceFile(IResource resource) {
		if (resource.getType() == IResource.FILE) {
			IFile file = (IFile) resource;
			String name = file.getName();
			return ((name.endsWith(".lst") || name.endsWith(".lstx")) && !name.startsWith("stabs.") && !name.startsWith("~tmpres."));
		}

		return false;
	}
	
	public static File getResourcesDirectory(IProject project) {
		File resDir = project.getLocation().append("Resources").toFile();
		return resDir.exists() && resDir.isDirectory() ? resDir : null;
	}
	
}
