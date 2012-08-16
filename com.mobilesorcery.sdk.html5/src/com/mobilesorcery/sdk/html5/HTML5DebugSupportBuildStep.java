package com.mobilesorcery.sdk.html5;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
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
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;

public class HTML5DebugSupportBuildStep extends AbstractBuildStep {

	// Must be shared instance due to file ids; we could move it to be project centric instead?
	private static JSODDSupport op = new JSODDSupport();
	private final class InstrumentationBuilderVisitor extends
			IncrementalBuilderVisitor {
		private final ArrayList<IResource> resourcesToInstrument = new ArrayList<IResource>();
		private final File outputRoot;
		private final IFolder inputRoot;

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

		public void instrument(IProgressMonitor monitor) throws IOException {
			boolean addBoilerplate = !resourcesToInstrument.isEmpty();
			if (addBoilerplate) {
				File boilerplateFile = new File(outputRoot, "BoIlErPlAtE.js");
				boilerplateFile.getParentFile().mkdirs();
				FileWriter boilerplateOutput = new FileWriter(boilerplateFile);
				try {
					op.generateBoilerplate(MoSyncProject.create(getProject()), boilerplateOutput);
				} finally {
					Util.safeClose(boilerplateOutput);
				}
			}
			for (IResource resourceToInstrument : resourcesToInstrument) {
				IPath resourcePath = resourceToInstrument.getFullPath();
				resourcePath = resourcePath.removeFirstSegments(inputRoot.getFullPath().segmentCount());
				File outputFile = new File(outputRoot, resourcePath.toOSString());
				outputFile.getParentFile().mkdirs();
				FileWriter output = new FileWriter(outputFile);
				try {
					op.rewrite(resourceToInstrument.getFullPath(), output);
				} finally {
					Util.safeClose(output);
				}
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
			return "HTML5/JavaScript bundling";
		}

	}

	public HTML5DebugSupportBuildStep(Factory prototype) {
	}

	@Override
	public int incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult result,
			IProgressMonitor monitor) throws Exception {
		IProject wrappedProject = project.getWrappedProject();
		Path inputRootPath = new Path("LocalFiles");
		IFolder inputRoot = wrappedProject.getFolder(inputRootPath);
		if (inputRoot.exists()) {
			IPropertyOwner properties = MoSyncBuilder.getPropertyOwner(project, variant.getConfigurationId());
			// MOSYNC-2326 & MOSYNC-2327
			IFile outputFile = wrappedProject.getFile("Resources/LocalFiles.bin");
			DependencyManager<IResource> deps = getBuildState().getDependencyManager();
			deps.addDependency(outputFile, inputRoot);
			/*if (PropertyUtil.getBoolean(properties, MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS)) {
				monitor.beginTask("Instrumenting JavaScript source files", 10);
				File outputRoot = MoSyncBuilder.getOutputPath(wrappedProject, variant).append(inputRootPath).toFile();
				// TODO: Efficiency, perhaps?
				Util.copyDir(new SubProgressMonitor(monitor, 3), inputRoot.getLocation().toFile(), outputRoot, null);
				InstrumentationBuilderVisitor visitor = new InstrumentationBuilderVisitor(inputRoot, outputRoot);
				visitor.setProject(wrappedProject);
				inputRoot.accept(visitor);
				visitor.instrument(new SubProgressMonitor(monitor, 7));

				// TODO -- would prefer it not to always output there... Since we'll get build problems for sure!
				BundleBuildStep.bundle(outputRoot, wrappedProject.getLocation().append("Resources/LocalFiles.bin").toFile());
			} else {*/
				BundleBuildStep.bundle(inputRoot.getLocation().toFile(), wrappedProject.getLocation().append("Resources/LocalFiles.bin").toFile());
			//}
		}
		monitor.done();
		return CONTINUE;
	}

}
