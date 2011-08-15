package com.mobilesorcery.sdk.builder.iphoneos;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;

public class XCodeBuild extends AbstractTool {

	private final static Pattern XCODE_VERSION_PATTERN = Pattern.compile(".*(\\d.*)");
	
	private String pathToCommand;

	private boolean canExecute;

	/**
	 * Returns the xcodebuild tool with the default path (ie it is expected to
	 * be added to the system path, and modified using the xcode-select command)
	 * 
	 * @return
	 */
	public static XCodeBuild getDefault() {
		return new XCodeBuild("xcodebuild");
	}

	private XCodeBuild(String pathToCommand) {
		super(null);
		this.pathToCommand = pathToCommand;
		this.canExecute = isMac() && canExecute();
	}

	public boolean canExecute() {
		try {
			execute(new String[] { pathToCommand, "-version" }, null, null, false);
			return true;
		} catch (CoreException e) {
			return false;
		}
	}

	/**
	 * Builds an Xcode project
	 * 
	 * @param pathToXcodeProject
	 * @param sdk
	 *            The sdk to use for building, or <code>null</code> for the
	 *            default.
	 * @return The path to the built app
	 * @throws CoreException
	 *             If the build for some reason failed.
	 */
	public void build(IPath pathToXcodeProject, String target, String sdk)
			throws CoreException {
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(pathToCommand);
		cmd.add("-project");
		cmd.add(pathToXcodeProject.toOSString() + "/%project-name%.xcodeproj");
		if (!Util.isEmpty(sdk)) {
			cmd.add("-sdk");
			cmd.add(sdk);
		}
		if (execute(cmd.toArray(new String[cmd.size()]), null, null, false) != 0) {
			throw new CoreException(new Status(IStatus.ERROR,
					"com.mobilesorcery.sdk.builder.iphoneos",
					"xcodebuild failed to build"));
		}
	}

	@Override
	protected String getToolName() {
		return "xcodebuild";
	}

	@Override
	public boolean isValid() {
		return canExecute && super.isValid();
	}

	/**
	 * Returns the list of available SDKs, or <code>null</code> if unable to
	 * extract them.
	 * 
	 * @return
	 */
	public List<SDK> listSDKs() {
		CollectingLineHandler lh = new CollectingLineHandler();
		try {
			execute(new String[] { pathToCommand, "-showsdks" }, lh, lh, false);
			List<String> sdksOutput = lh.getLines();
			return extractSDKs(sdksOutput);
		} catch (CoreException e) {
			return null;
		}
	}

	private List<SDK> extractSDKs(List<String> lines) {
		ArrayList<SDK> result = new ArrayList<SDK>();
		for (String line : lines) {
			if (line.contains("-sdk")) {
				String[] components = line.split("-sdk", 2);
				if (components.length == 2) {
					String name = components[0].trim();
					String id = components[1].trim();
					result.add(new SDK(id, name));
				}
			}
		}
		return result;
	}

}
