package com.mobilesorcery.sdk.builder.iphoneos;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;

public class IPhoneSimulator extends AbstractTool {

	public synchronized static IPhoneSimulator createDefault() {
		XCodeBuild.refreshDefault();
		Version xcodeVersion = XCodeBuild.getDefault().getVersion();
		String binary = new Version("4.3").isNewer(xcodeVersion) ? "iphonesim42" : "iphonesim";
		return new IPhoneSimulator(MoSyncTool.getDefault().getBinary(binary));
	}

	protected IPhoneSimulator(IPath toolPath) {
		super(toolPath);
	}

	/**
	 * Runs an iPhone app on the iPhone simulator
	 * @param pathToApp The path to the app (the file ending with .app)
	 * @param sdk The sdk to use, or {@code null }for the default
	 * @param family The device family (eg iPhone, iPad) to simulate, or {@code null} for the default
	 * @throws CoreException
	 */
	public void runApp(IPath pathToApp, String sdk, String family) throws CoreException {
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(getToolPath().getAbsolutePath());
		cmd.add("launch");
		cmd.add(pathToApp.toOSString());
		if (!Util.isEmpty(sdk)) {
			cmd.add("--sdk");
			cmd.add(sdk);
		}
		if (!Util.isEmpty(family)) {
			cmd.add("--family");
			cmd.add(family);
		}
		execute(cmd.toArray(new String[0]), null, null, false);
	}

	@Override
	protected String getToolName() {
		return "IPhone Simulator";
	}

}
