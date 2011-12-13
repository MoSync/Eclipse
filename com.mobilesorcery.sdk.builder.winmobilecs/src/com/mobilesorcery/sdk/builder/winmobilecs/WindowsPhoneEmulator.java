package com.mobilesorcery.sdk.builder.winmobilecs;

import java.io.File;

import javax.tools.ToolProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.Util;

public class WindowsPhoneEmulator extends AbstractTool {

	private static WindowsPhoneEmulator instance;
	private static String toolPath = null;

	public static WindowsPhoneEmulator getDefault() {
		String newToolPath = WinMobileCSPlugin.getDefault().getPreferenceStore().getString(WinMobileCSPlugin.EMULATOR_LOCATION);
		if (instance == null || !Util.equals(toolPath, newToolPath)) {
			Path path = newToolPath == null ? null : new Path(newToolPath);
			instance = new WindowsPhoneEmulator(path);
		}
		return instance;
	}

	WindowsPhoneEmulator(IPath toolPath) {
		super(toolPath);
	}

	public void run(File binFile) throws CoreException {
		execute(new String[] {
			getToolPath().getAbsolutePath(),
			binFile.getAbsolutePath()
		}, null, null, true);
	}

	@Override
	protected String getToolName() {
		return "Windows Phone Emulator";
	}
}
