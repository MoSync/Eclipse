package com.mobilesorcery.sdk.builder.android.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class Emulator extends AbstractTool {

	public Emulator(IPath pathToEmulator) {
		super(pathToEmulator);
	}

	public CollectingLineHandler start(String avd, boolean fork, String... options) throws CoreException {
		String[] commandLine = new String[options.length + 3];
		commandLine[0] = getToolPath().getAbsolutePath();
		commandLine[1] = "-avd";
		commandLine[2] = avd;
		System.arraycopy(options, 0, commandLine, 3, options.length);
		CollectingLineHandler result = new CollectingLineHandler();
		execute(commandLine, result, null, fork);
		return result;
	}

	public static Emulator getExternal() {
		IPath sdkPath = Activator.getDefault().getExternalAndroidSDKPath();
		return new Emulator(sdkPath == null ? null : sdkPath.append("tools/emulator" + MoSyncTool.getBinExtension()));
	}

	@Override
	protected String getToolName() {
		return "Android Emulator";
	}
}
