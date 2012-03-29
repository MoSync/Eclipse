package com.mobilesorcery.sdk.html5;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.build.AbstractBuildStep;
import com.mobilesorcery.sdk.core.build.AbstractBuildStepFactory;
import com.mobilesorcery.sdk.core.build.BundleBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStep;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.internal.builder.IncrementalBuilderVisitor;

public class HTML5DebugSupportBuildStep extends AbstractBuildStep {

	private final class InstrumentationBuilderVisitor extends
			IncrementalBuilderVisitor {
		private final class InstrumentationProjectVisitor implements
				IResourceVisitor {
			private final JSODDSupport op;
			private final boolean shouldAddBoilerPlate;

			private InstrumentationProjectVisitor(JSODDSupport op,
					boolean shouldAddBoilerPlate) {
				this.op = op;
				this.shouldAddBoilerPlate = shouldAddBoilerPlate;
			}

			@Override
			public boolean visit(IResource resourceToInstrument)
					throws CoreException {
				boolean isFramework = resourceToInstrument
						.getFullPath().lastSegment()
						.equalsIgnoreCase("wormhole.js");
				boolean doInstrument = resourcesToInstrument
						.contains(resourceToInstrument)
						|| (shouldAddBoilerPlate && isFramework);
				if (doInstrument) {
					IPath resourcePath = resourceToInstrument
							.getFullPath();
					resourcePath = resourcePath
							.removeFirstSegments(inputRoot
									.getFullPath().segmentCount());
					File outputFile = new File(outputRoot, resourcePath
							.toOSString());
					outputFile.getParentFile().mkdirs();
					FileWriter output = null;
					try {
						output = new FileWriter(outputFile);
						// TODO! Not only wormhole!
						boolean addBoilerPlate = resourcePath
								.lastSegment().equalsIgnoreCase(
										"wormhole.js");
						if (addBoilerPlate) {
							op.generateBoilerplate(
									MoSyncProject.create(getProject()),
									output);
						}
						op.rewrite(resourceToInstrument.getFullPath(),
								output);
					} catch (IOException e) {
						throw new CoreException(
								new Status(
										IStatus.ERROR,
										Html5Plugin.PLUGIN_ID,
										"Cannot instrument JavaScript for debugging",
										e));
					} finally {
						Util.safeClose(output);
					}
				}
				return !MoSyncBuilder.isInOutput(project, resourceToInstrument);
			}
		}

		private final HashSet<IResource> resourcesToInstrument = new HashSet<IResource>();
		private final File outputRoot;
		private final IFolder inputRoot;
		private IFileTreeDiff diff;

		public InstrumentationBuilderVisitor(IFolder inputRoot, File outputRoot) {
			this.inputRoot = inputRoot;
			this.outputRoot = outputRoot;
		}

		@Override
		public boolean doesAffectBuild(IResource resource) {
			return JSODDSupport.isValidJavaScriptFile(resource.getLocation());
		}

		@Override
		public boolean visit(IResource resource) throws CoreException {
			boolean shouldVisitChildren = super.visit(resource);
			if (isBuildable(resource)) {
				resourcesToInstrument.add(resource);
			}
			return shouldVisitChildren;
		}

		@Override
		public void setDiff(IFileTreeDiff diff) throws CoreException {
			super.setDiff(diff);
			this.diff = diff;
		}

		public void instrument(IProgressMonitor monitor) throws CoreException {
			/*
			 * TODO: Put boilerplate code in a separate file: boolean
			 * addBoilerplate = !resourcesToInstrument.isEmpty(); if
			 * (addBoilerplate) { File boilerplateFile = new File(outputRoot,
			 * "BoIlErPlAtE.js"); boilerplateFile.getParentFile().mkdirs();
			 * FileWriter boilerplateOutput = new FileWriter(boilerplateFile);
			 * try { op.generateBoilerplate(MoSyncProject.create(getProject()),
			 * boilerplateOutput); } finally {
			 * Util.safeClose(boilerplateOutput); } }
			 */
			final JSODDSupport op = Html5Plugin.getDefault().getJSODDSupport(
					project);
			final boolean shouldAddBoilerPlate = op.applyDiff(diff);

			if (diff == null || !diff.isEmpty()) {
				project.accept(new InstrumentationProjectVisitor(op, shouldAddBoilerPlate));
			}
		}
	}

	public static class Factory extends AbstractBuildStepFactory {

		@Override
		public IBuildStep create() {
			return new HTML5DebugSupportBuildStep(this);
		}

		@Override
		public String getId() {
			return HTML5DebugSupportBuildStepExtension.ID;
		}

		@Override
		public String getName() {
			return "JavaScript Debug Support";
		}

	}

	public HTML5DebugSupportBuildStep(Factory prototype) {
		setName(prototype.getName());
	}

	@Override
	public int incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult result,
			IProgressMonitor monitor) throws Exception {
		IProject wrappedProject = project.getWrappedProject();
		Path inputRootPath = new Path("LocalFiles");
		IFolder inputRoot = wrappedProject.getFolder(inputRootPath);
		if (inputRoot.exists()) {
			IPropertyOwner properties = MoSyncBuilder.getPropertyOwner(project,
					variant.getConfigurationId());
			if (PropertyUtil.getBoolean(properties,
					MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS)) {
				monitor.beginTask("Instrumenting JavaScript source files", 10);
				File outputRoot = MoSyncBuilder
						.getOutputPath(wrappedProject, variant)
						.append(inputRootPath).toFile();
				// TODO: Efficiency, perhaps?
				Util.copyDir(new SubProgressMonitor(monitor, 3), inputRoot
						.getLocation().toFile(), outputRoot, null);
				InstrumentationBuilderVisitor visitor = new InstrumentationBuilderVisitor(
						inputRoot, outputRoot);
				visitor.setProject(wrappedProject);
				visitor.setDiff(diff);
				inputRoot.accept(visitor);
				visitor.instrument(new SubProgressMonitor(monitor, 7));

				// TODO -- would prefer it not to always output there... Since
				// we'll get build problems for sure!
				BundleBuildStep.bundle(outputRoot, wrappedProject.getLocation()
						.append("Resources/LocalFiles.bin").toFile());
			} else {
				BundleBuildStep.bundle(
						inputRoot.getLocation().toFile(),
						wrappedProject.getLocation()
								.append("Resources/LocalFiles.bin").toFile());
			}
		}
		monitor.done();
		return CONTINUE;
	}

}
