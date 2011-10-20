package com.mobilesorcery.sdk.core.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.BuildResult;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.build.BundleBuildStep.Factory;
import com.mobilesorcery.sdk.internal.builder.IncrementalBuilderVisitor;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;

public class BundleBuildStep extends CommandLineBuildStep {

	public final static String ID = "bundle";

	public static class Factory extends CommandLineBuildStep.Factory {

		private String inFile;
		private String outFile;

		@Override
		public String getId() {
			return BundleBuildStep.ID;
		}

		@Override
		public IBuildStep create() {
			return new BundleBuildStep(this);
		}

		public void setInFile(String inFile) {
			this.inFile = inFile;
		}

		public String getInFile() {
			return inFile;
		}

		public void setOutFile(String outFile) {
			this.outFile = outFile;
		}

		public String getOutFile() {
			return outFile;
		}

		@Override
		public Script getScript() {
			Script script = new Script(new String[][] {{
				"%mosync-bin%/Bundle", "-in", inFile, "-out", outFile
			}});
			return script;
		}

		@Override
		public boolean shouldRunPerFile() {
			return false;
		}

		@Override
		public void load(IMemento memento) {
			IMemento command = memento.getChild("bundle");
			if (command != null) {
				this.inFile = command.getString("inFile");
				this.outFile = command.getString("outFile");
				this.name = command.getString("name");
				Boolean failOnError = command.getBoolean("foe");
				this.failOnError = failOnError == null ? false : failOnError;
			}
		}

		@Override
		public void store(IMemento memento) {
			IMemento command = memento.createChild("bundle");
			command.putString("inFile", inFile);
			command.putString("outFile", outFile);
			command.putBoolean("foe", failOnError);
			command.putString("name", name);
		}

		@Override
		public boolean requiresPrivilegedAccess() {
			return false;
		}

	}

	private final Factory factory;

	public BundleBuildStep(Factory prototype) {
		super(prototype);
		this.factory = prototype;
	}

	@Override
	public int incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff,
			IBuildResult buildResult, IProgressMonitor monitor) throws Exception {
		final File inFile = new File(Util.replace(factory.inFile, getParameterResolver()));
		final File outFile = new File(Util.replace(factory.outFile, getParameterResolver()));

		IncrementalBuilderVisitor visitor = new IncrementalBuilderVisitor() {
			@Override
			public boolean doesAffectBuild(IResource resource) {
				return super.doesAffectBuild(resource) && Util.isParent(inFile, resource.getLocation().toFile());
			}
		};

		// TODO: REFACTOR!
		visitor.setProject(project.getWrappedProject());
        visitor.setVariant(variant);
        visitor.setDependencyProvider(getDependencyProvider());

        visitor.setConsole(getConsole());
        visitor.setDiff(diff);
        visitor.setResourceFilter(getResourceFilter());
        visitor.setParameterResolver(getParameterResolver());

		visitor.visit(project.getWrappedProject());
		DependencyManager<IResource> deps = getBuildState().getDependencyManager();
		deps.clearDependencies(Arrays.asList(visitor.getDeletedResources()));
		IFile[] correspondingResources = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(outFile.toURI());
		for (IFile correspondingResource : correspondingResources) {
			deps.addDependencies(correspondingResource, Arrays.asList(visitor.getChangedOrAddedResources()));
		}
		Set<IResource> rebuildTheseResources = visitor.computeResourcesToRebuild(deps);
		if (!rebuildTheseResources.isEmpty()) {
			outFile.getParentFile().mkdirs();
			super.incrementalBuild(project, session, variant, diff, buildResult, monitor);
		}
		return CONTINUE;
	}
}
