package com.mobilesorcery.sdk.builder.android.launch;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;

public abstract class AndroidTool {

	private boolean toolExists;
	private File toolPath;
	
	public boolean isValid() {
		return toolExists;
	}
	
	protected AndroidTool(IPath toolPath) {
		setToolPath(toolPath.toFile());
	}
	
	public void assertValid() throws CoreException {
		if (!isValid()) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, String.format("%s not found at %s; please check your settings", getToolName(), getToolPath())));
		}
	}

	protected void setToolPath(File toolPath) {
		this.toolPath = toolPath;
		toolExists = toolPath != null && toolPath.exists();
	}
	
	protected File getToolPath() {
		return toolPath;
	}

	/**
	 * Executes a command line
	 * @param commandLine
	 * @param stdoutLineHandler
	 * @param stderrLineHandler
	 * @param fork
	 * @return <code>-1</code> if <code>fork</code> is set to <code>true</code>,
	 * otherwise the return code of the executed process.
	 * @throws CoreException
	 */
	protected int execute(String[] commandLine, ILineHandler stdoutLineHandler,
			ILineHandler stderrLineHandler, boolean fork) throws CoreException {
		try {
			CommandLineExecutor executor = new CommandLineExecutor(
					MoSyncBuilder.CONSOLE_ID);
			executor.setLineHandlers(stdoutLineHandler, stderrLineHandler);
			executor.addCommandLine(commandLine);
			if (fork) {
				executor.fork();
				return -1;
			} else {
				return executor.execute();
			}
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, e.getMessage(), e));
		}
	}
	protected abstract String getToolName();
}
