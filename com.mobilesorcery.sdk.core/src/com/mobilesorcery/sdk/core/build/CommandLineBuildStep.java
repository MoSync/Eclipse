package com.mobilesorcery.sdk.core.build;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.Util;

public class CommandLineBuildStep extends AbstractBuildStep {

	public final static String ID = "cmd";
	
	public static class Factory implements IBuildStepFactory {

		private String cmdLine;

		@Override
		public void load(IMemento memento) {
			IMemento command = memento.getChild("cmd");
			cmdLine = command.getTextData();
		}

		@Override
		public void store(IMemento memento) {
			IMemento command = memento.createChild("cmd");
			command.putTextData(cmdLine);
		}

		@Override
		public IBuildStep create() {
			return new CommandLineBuildStep(cmdLine);
		}
		
	}

	private String cmdLine;
	
	public CommandLineBuildStep(String cmdLine) {
		this.cmdLine = cmdLine;
	}

	@Override
	public void incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildState buildState, IBuildVariant variant, IFileTreeDiff diff,
			IBuildResult result, IFilter<IResource> resourceFilter,
			IProgressMonitor monitor) throws Exception {
		ParameterResolver resolver = getParameterResolver();
		String resolvedCommandLine = Util.replace(cmdLine, resolver);
		CommandLineExecutor executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
		int exitCode = executor.runCommandLine(executor.parseCommandLine(resolvedCommandLine));
		if (exitCode != 0) {
			throw new IOException("External command failed: returned error code <> 0");
		}
	}

}
