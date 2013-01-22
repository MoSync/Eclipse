package com.mobilesorcery.sdk.builder.iphoneos;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;

public class IPhoneSimulator extends AbstractTool {

	public synchronized static IPhoneSimulator createDefault() {
		XCodeBuild.refreshDefault();
		Version xcodeVersion = XCodeBuild.getDefault().getVersion();
		boolean legacyVersion = new Version("4.3").isNewer(xcodeVersion);
		String binary = legacyVersion ? "iphonesim42" : "iphonesim";
		IPhoneSimulator result = new IPhoneSimulator(MoSyncTool.getDefault().getBinary(binary), !legacyVersion);
		return result;
	}

	private CoreException error;

	protected IPhoneSimulator(IPath toolPath, boolean initEnv) {
		super(toolPath);
		if (initEnv) {
			try {
				initEnv();
			} catch (CoreException e) {
				error = e;
			}
		}
	}
	
	public boolean isValid() {
		return super.isValid() && error == null;
	}
	
	public void assertValid() throws CoreException {
		if (error != null) {
			throw error;
		}
		super.assertValid();
	}

	private void initEnv() throws CoreException {
		String xcodepath = XCodeSelect.getInstance().getCurrentXCodePath();
		String dyldFrameworkEnv = MessageFormat.format("{0}/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks:{0}/../OtherFrameworks", xcodepath);
		setAdditionalEnv("DYLD_FRAMEWORK_PATH", dyldFrameworkEnv);
	}

	/**
	 * Runs an iPhone app on the iPhone simulator
	 * @param pathToApp The path to the app (the file ending with .app)
	 * @param sdk The sdk to use, or {@code null }for the default
	 * @param family The device family (eg iPhone, iPad) to simulate, or {@code null} for the default
	 * @throws CoreException
	 * @return The emulator process
	 */
	public Process runApp(IPath pathToApp, String sdk, String family) throws CoreException {
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
		CollectingLineHandler processHandler = new CollectingLineHandler();
		execute(cmd.toArray(new String[0]), processHandler, null, true);
		return processHandler.getProcess();
	}

	@Override
	protected String getToolName() {
		return "IPhone Simulator";
	}

}
