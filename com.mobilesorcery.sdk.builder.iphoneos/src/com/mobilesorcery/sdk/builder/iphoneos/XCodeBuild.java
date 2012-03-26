package com.mobilesorcery.sdk.builder.iphoneos;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.LineReader.LineAdapter;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;

public class XCodeBuild extends AbstractTool {

	private static class XCodeBuildHandler extends LineAdapter {
		private final static Pattern XCODE_VERSION_PATTERN = Pattern
				.compile(".*?(\\d.*)");

		Version version = null;

		@Override
		public void newLine(String line) {
			if (version == null || !version.isValid()) {
				Matcher matcher = XCODE_VERSION_PATTERN.matcher(line);
				if (matcher.matches()) {
					String versionStr = matcher.group(1);
					version = new Version(versionStr);
				}
			}
		}

		public Version getVersion() {
			return version;
		}

	}

	public static final int IOS_SDKS = 1 << 0;
	public static final int IOS_SIMULATOR_SDKS = 1 << 1;
	public static final int ALL_SDKS = IOS_SDKS | IOS_SIMULATOR_SDKS;

	private static XCodeBuild instance = null;

	private final String pathToCommand;

	private boolean canExecute = false;

	private ArrayList<SDK> cachedSDKs;

	private Version version;

	/**
	 * Returns the xcodebuild tool with the default path (ie it is expected to
	 * be added to the system path, and modified using the xcode-select command)
	 *
	 * @return
	 */
	public static synchronized XCodeBuild getDefault() {
		if (instance == null) {
			String xcodeBuildCommand = System.getProperty("XCODEBUILD_COMMAND");
			instance = new XCodeBuild(xcodeBuildCommand == null ? "xcodebuild"
					: xcodeBuildCommand);
		}
		return instance;
	}

	public static void refreshDefault() {
		instance = null;
		getDefault();
	}

	private XCodeBuild(String pathToCommand) {
		super(null);
		this.pathToCommand = pathToCommand;
		init();
	}

	private void init() {
		if (isMac()) {
			try {
				XCodeBuildHandler handler = new XCodeBuildHandler();
				execute(new String[] { pathToCommand, "-version" }, handler,
						handler, false);
				this.version = handler.getVersion();
				this.canExecute = true;
			} catch (CoreException e) {
				// Cannot execute.
			}
		}
	}

	public boolean canExecute() {
		return canExecute;
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
		chdir(pathToXcodeProject.toFile());
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(pathToCommand);
		cmd.add("-project");
		// XCode < 4.0 does not allow for absolute paths here.
		cmd.add("%project-name%.xcodeproj");
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
	 * @param sdkTypes
	 *            A list of OR'ed sdk types. An sdk type is either
	 *            {@link #IOS_SDKS} or {@link #IOS_SIMULATOR_SDKS}.
	 * @return The list of matching SDKs
	 */
	public synchronized List<SDK> listSDKs(int sdkTypes) {
		if (cachedSDKs == null) {
			cachedSDKs = new ArrayList<SDK>();
			CollectingLineHandler lh = new CollectingLineHandler();
			try {
				execute(new String[] { pathToCommand, "-showsdks" }, lh, lh,
						false);
				List<String> sdksOutput = lh.getLines();
				cachedSDKs.addAll(extractSDKs(sdksOutput));
			} catch (CoreException e) {
				// Do nothing but log.
				CoreMoSyncPlugin.getDefault().log(e);
			}
		}

		return filterSDKs(cachedSDKs, sdkTypes);
	}

	private List<SDK> filterSDKs(ArrayList<SDK> sdks, int sdkTypes) {
		ArrayList<SDK> result = new ArrayList<SDK>();
		for (SDK sdk : sdks) {
			if (sdk.isSimulatorSDK() && (sdkTypes & IOS_SIMULATOR_SDKS) != 0) {
				result.add(sdk);
			}
			if (sdk.isIOSSDK() && (sdkTypes & IOS_SDKS) != 0) {
				result.add(sdk);
			}
		}
		return result;
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

	/**
	 * Returns a default SDK for the given SDK type.
	 *
	 * @param sdkType
	 *            Either {@link #IOS_SDKS} or {@link #IOS_SIMULATOR_SDKS}.
	 * @return
	 */
	public SDK getDefaultSDK(int sdkType) {
		List<SDK> sdks = listSDKs(sdkType);
		Version bestMatch = null;
		SDK bestMatchSDK = null;
		for (SDK sdk : sdks) {
			Version version = sdk.getVersion();
			if (bestMatch == null || version.isNewer(bestMatch)) {
				bestMatch = version;
				bestMatchSDK = sdk;
			}
		}
		return bestMatchSDK;
	}

	/**
	 * Returns the SDK with a specific id
	 *
	 * @param sdkId
	 *            Returns the first matching SDK with id <code>sdkId</code>.
	 * @return <code>null</code> if no match was found
	 */
	public SDK getSDK(String sdkId) {
		if (Util.isEmpty(sdkId)) {
			return null;
		}

		for (SDK sdk : listSDKs(ALL_SDKS)) {
			if (sdkId.equals(sdk.getId())) {
				return sdk;
			}
		}

		return null;
	}

	public void refresh() {
		cachedSDKs = null;
	}

	public Version getVersion() {
		return version;
	}

}
