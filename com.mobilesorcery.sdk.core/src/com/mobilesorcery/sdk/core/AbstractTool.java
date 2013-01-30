package com.mobilesorcery.sdk.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;

public abstract class AbstractTool {

	private boolean toolExists;
	private File toolPath;
	private CascadingProperties parameters;
	private File currentDir;
	private Map<String, String> envs = new HashMap<String, String>();

	protected AbstractTool(IPath toolPath) {
		setToolPath(toolPath == null ? null : toolPath.toFile());
	}

	/**
	 * Returns <code>true</code> if this tool
	 * is valid. The default implementation checks
	 * whether the path provided in the default constructor
	 * exists. If no tool was provided (<code>null</code>),
	 * the tool is considered valid, but clients should
	 * override to provide proper validation.
	 * @return
	 */
	public boolean isValid() {
		return toolPath == null || toolExists;
	}

	/**
	 * Asserts whether the tool is valid. The default
	 * implementation defers to {@link #isValid()}.
	 * @throws CoreException
	 */
	public void assertValid() throws CoreException {
		if (!isValid()) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, String.format("%s not found at %s; please check your settings", getToolName(), getToolPath())));
		}
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
	}

	public static boolean isMac() {
		return System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
	}

	protected void setToolPath(File toolPath) {
		this.toolPath = toolPath;
		toolExists = toolPath != null && toolPath.exists();
	}

	public File getToolPath() {
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
	 * @throws CoreException If the tool is not valid or if some other exception was thrown.
	 */
	protected int execute(String[] commandLine, ILineHandler stdoutLineHandler,
			ILineHandler stderrLineHandler, boolean fork) throws CoreException {
		return execute(commandLine, stdoutLineHandler, stderrLineHandler, MoSyncBuilder.CONSOLE_ID, fork);
	}
	/**
	 * Executes a command line
	 * @param commandLine
	 * @param stdoutLineHandler
	 * @param stderrLineHandler
	 * @param fork
	 * @return <code>-1</code> if <code>fork</code> is set to <code>true</code>,
	 * otherwise the return code of the executed process.
	 * @throws CoreException If the tool is not valid or if some other exception was thrown.
	 */
	protected int execute(String[] commandLine, ILineHandler stdoutLineHandler,
			ILineHandler stderrLineHandler, String consoleId, boolean fork) throws CoreException {
		try {
			CommandLineExecutor executor = new CommandLineExecutor(consoleId);
			if (currentDir != null) {
				executor.setExecutionDirectory(currentDir.getAbsolutePath());
			}
			if (envs != null) {
				for (Map.Entry<String, String> env : envs.entrySet()) {
					executor.addEnv(env.getKey(), env.getValue());
				}
			}
			executor.setParameters(getParameters());
			executor.setLineHandlers(stdoutLineHandler, stderrLineHandler);
			executor.addCommandLine(commandLine);
			if (fork) {
				executor.fork();
				return -1;
			} else {
				return executor.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new CoreException(new Status(IStatus.ERROR,
					CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	public void setParameters(CascadingProperties parameters) {
		this.parameters = parameters;
	}

	protected CascadingProperties getParameters() {
		return parameters;
	}

	/**
	 * Changes the current directory of this tool.
	 * Executions will take place in this directory.
	 * @param parentFile {@code null} implies no change of directory
	 * before executing.
	 * @throws CoreException If {@code currentDir} is not
	 * a directory.
	 */
	protected void chdir(File currentDir) throws CoreException {
		if (!currentDir.isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Expected a directory, this file is not:" + currentDir));
		}
		this.currentDir = currentDir;
	}
	
	protected void setAdditionalEnv(String key, String value) {
		this.envs.put(key, value);
	}

	protected abstract String getToolName();
}
