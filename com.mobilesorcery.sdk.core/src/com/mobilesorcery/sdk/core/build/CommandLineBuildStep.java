package com.mobilesorcery.sdk.core.build;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.PathExclusionFilter;
import com.mobilesorcery.sdk.core.PrivilegedAccess;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.builder.IncrementalBuilderVisitor;

public class CommandLineBuildStep extends AbstractBuildStep {

	public final static String ID = "cmd";

	public static class Factory implements IBuildStepFactory {

		String script;
		boolean runPerFile = false;
		String filePattern;
		String name;
		boolean failOnError = false;

		public boolean shouldFailOnError() {
			return failOnError;
		}

		public void setFailOnError(boolean failOnError) {
			this.failOnError = failOnError;
		}

		public Factory() {
			setName("Command Line");
		}

		@Override
		public void load(IMemento memento) {
			IMemento command = memento.getChild("cmd");
			if (command != null) {
				script = command.getTextData();
				Boolean runPerFile = command.getBoolean("pf");
				this.runPerFile = runPerFile == null ? false : runPerFile;
				filePattern = command.getString("pt");
				name = command.getString("name");
				Boolean failOnError = command.getBoolean("foe");
				this.failOnError = failOnError == null ? false : failOnError;
			}
		}

		@Override
		public void store(IMemento memento) {
			IMemento command = memento.createChild("cmd");
			command.putBoolean("pf", runPerFile);
			command.putString("pt", filePattern);
			command.putString("name", name);
			command.putBoolean("foe", failOnError);
			command.putTextData(script);
		}

		@Override
		public IBuildStep create() {
			return new CommandLineBuildStep(this);
		}

		@Override
		public String getId() {
			return ID;
		}

		public String getScript() {
			return script;
		}

		public void setScript(String script) {
			this.script = script;
		}

		public boolean shouldRunPerFile() {
			return runPerFile;
		}

		public void setRunPerFile(boolean runPerFile) {
			this.runPerFile = runPerFile;
		}

		public String getFilePattern() {
			return filePattern;
		}

		public void setFilePattern(String filePattern) {
			this.filePattern = filePattern;
		}

		@Override
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean requiresPrivilegedAccess() {
			return true;
		}

	}

	public static final class BuildStepParameterResolver extends ParameterResolver {

		private IResource currentResource;
		private final ParameterResolver fallback;
		private String[] allResources;

		public BuildStepParameterResolver(ParameterResolver fallback) {
			this.fallback = fallback;
		}

		public void setCurrentResource(IResource currentResource) {
			this.currentResource = currentResource;
		}

		@Override
		public String get(String key) throws ParameterResolverException {
			if (currentResource != null && "file".equals(key)) {
				return currentResource.getFullPath().toFile().getAbsolutePath();
			}
			if ("@files".equals(key)) {
				return Util.join(allResources, " ");
			}

			return fallback == null ? null : fallback.get(key);
		}

		public void setAllFiles(List<IResource> allResources) {
			this.allResources = new String[allResources.size()];
			int ix = 0;
			for (IResource resource : allResources) {
				this.allResources[ix] = resource.getFullPath().toFile().getAbsolutePath();
	            ix++;
			}
		}

		@Override
		public List<String> listPrefixes() {
			ArrayList<String> result = new ArrayList<String>();
			if (fallback != null) {
				result.addAll(fallback.listPrefixes());
			}

			// TODO: Ehm... context sensitive perhaps!?
			result.add("file");
			result.add("@files");
			return result;
		}

		@Override
		public List<String> listAvailableParameters(String prefix) {
			return fallback.listAvailableParameters(prefix);
		}
	}

	private final Factory prototype;

	public CommandLineBuildStep(Factory prototype) {
		this.prototype = prototype;
		setId(ID);
		setName(prototype.name);
	}

	class Visitor extends IncrementalBuilderVisitor {

		private PathExclusionFilter filter;
		private final boolean runPerFile;

		public Visitor(String filePattern, boolean runPerFile) {
			this.runPerFile = runPerFile;
			if (!Util.isEmpty(filePattern)) {
				filter = PathExclusionFilter.parse(filePattern.split("\\s"));
			}
		}

		@Override
		public boolean doesAffectBuild(IResource resource) {
			return filter == null || filter.inverseAccept(resource);
		}

		public void executeScript() throws IOException, ParameterResolverException {
			// TODO: Refactor this!?
			BuildStepParameterResolver resolver = (BuildStepParameterResolver) getParameterResolver();
			resolver.setAllFiles(changedOrAddedResources);

			if (runPerFile) {
				for (IResource resource : changedOrAddedResources) {
					resolver.setCurrentResource(resource);
					CommandLineBuildStep.this.executeScript(resolver);
				}
			} else {
				CommandLineBuildStep.this.executeScript(resolver);
			}
		}

		@Override
		public ParameterResolver getParameterResolver() {
			BuildStepParameterResolver resolver = createParameterResolver(super.getParameterResolver());
			return resolver;
		}
	}

	@Override
	public int incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff,
			IBuildResult result, IProgressMonitor monitor) throws Exception {
		if (prototype.requiresPrivilegedAccess()) {
			PrivilegedAccess.getInstance().assertAccess(project);
		}
		Visitor visitor = new Visitor(prototype.filePattern, prototype.runPerFile);
		visitor.setParameterResolver(getParameterResolver());
		project.getWrappedProject().accept(visitor);
		visitor.executeScript();

        return CONTINUE;
	}

	public void executeScript(ParameterResolver resolver) throws IOException,
			ParameterResolverException {
		String[] cmdLines = prototype.script == null ? new String[0]
				: prototype.script.split("\\n");
		for (int i = 0; i < cmdLines.length; i++) {
			String resolvedCommandLine = Util.replace(cmdLines[i], resolver);
			CommandLineExecutor executor = new CommandLineExecutor(
					MoSyncBuilder.CONSOLE_ID);
			int exitCode = executor.runCommandLine(executor
					.parseCommandLine(resolvedCommandLine));
			if (prototype.failOnError && exitCode != 0) {
				throw new IOException(
						MessageFormat.format(
								"External command \"{0}\" failed: returned error code <> 0",
								prototype.name));
			}
		}
	}

	public static BuildStepParameterResolver createParameterResolver(ParameterResolver fallback) {
		return new BuildStepParameterResolver(fallback);
	}
}
