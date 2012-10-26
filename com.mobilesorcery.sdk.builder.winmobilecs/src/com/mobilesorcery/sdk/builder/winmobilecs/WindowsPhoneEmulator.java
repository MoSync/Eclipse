package com.mobilesorcery.sdk.builder.winmobilecs;

import java.io.File;

import javax.tools.ToolProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

public class WindowsPhoneEmulator extends AbstractTool {

	private static WindowsPhoneEmulator instance;
	private static IPath toolPath = null;

	public static WindowsPhoneEmulator getDefault() {
		IPath newToolPath = MoSyncTool.getDefault().getBinary("WP7AppLauncher/WP7AppLauncher");
		if (instance == null || !Util.equals(toolPath, newToolPath)) {
			IPath path = newToolPath == null ? null : newToolPath;
			instance = new WindowsPhoneEmulator(path);
		}
		return instance;
	}

	WindowsPhoneEmulator(IPath toolPath) {
		super(toolPath);
	}

	public Process run(File binFile) throws CoreException {
		CollectingLineHandler processHandler = new CollectingLineHandler();
		execute(new String[] {
			getToolPath().getAbsolutePath(),
			binFile.getAbsolutePath()
		}, processHandler, null, true);
		return processHandler.getProcess();
	}

	@Override
	protected String getToolName() {
		return "Windows Phone Emulator";
	}
}
