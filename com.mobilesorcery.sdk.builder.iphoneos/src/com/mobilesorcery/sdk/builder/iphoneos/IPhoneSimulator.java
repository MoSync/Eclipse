package com.mobilesorcery.sdk.builder.iphoneos;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

public class IPhoneSimulator extends AbstractTool {

	public static IPhoneSimulator getDefault() {
		return new IPhoneSimulator(MoSyncTool.getDefault().getBinary("iphonesim"));
	}
	
	protected IPhoneSimulator(IPath toolPath) {
		super(toolPath);
	}
	
	/**
	 * Runs an iPhone app on the iPhone simulator
	 * @param pathToApp The path to the app (the file ending with .app)
	 * @param sdk The sdk to use, or <code>null</code> for the default
	 * @throws CoreException
	 */
	public void runApp(IPath pathToApp, String sdk) throws CoreException {
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(getToolPath().getAbsolutePath());
		cmd.add("launch");
		cmd.add(pathToApp.toOSString());
		if (!Util.isEmpty(sdk)) {
			cmd.add(sdk);
		}
		execute(cmd.toArray(new String[0]), null, null, false);
	}

	@Override
	protected String getToolName() {
		return "IPhone Simulator";
	}

}
