package com.mobilesorcery.sdk.builder.android.launch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.LineReader.LineAdapter;

public class Android extends AbstractTool {

	// REFACTOR ME!
	private static class AVDHandler extends LineAdapter {

		private final static Pattern NAME_PATTERN = Pattern.compile("\\s*Name:\\s*(.*)\\s*");
		// Can we really know this works across all adb versions? Well, never mind, if it does
		// not work, that's fine too
		private final static Pattern API_LEVEL_PATTERN = Pattern.compile(".*API level\\s*(\\d*).*");
		private final List<AVD> avds = new ArrayList<AVD>();
		private AVD currentAVD;

		@Override
		public void newLine(String line) {
			String newName = extractName(line);
			if (newName != null) {
				addCurrentAVD();
				currentAVD = new AVD(newName, AVD.UNKNOWN_API_LEVEL);
			}
			Integer newAPILevel = extractAPILevel(line);
			if (newAPILevel != null) {
				currentAVD = new AVD(currentAVD.getName(), newAPILevel);
			}
		}

		private Integer extractAPILevel(String line) {
			Matcher matcher = API_LEVEL_PATTERN.matcher(line);
			if (matcher.matches()) {
				String apiLevelStr = matcher.group(1);
				return Integer.parseInt(apiLevelStr);
			}
			return null;
		}

		private String extractName(String line) {
			Matcher matcher = NAME_PATTERN.matcher(line);
			if (matcher.matches()) {
				String avdName = matcher.group(1);
				return avdName;
			}
			return null;
		}

		private void addCurrentAVD() {
			if (currentAVD != null) {
				avds.add(currentAVD);
				currentAVD = null;
			}
		}

		public List<AVD> getAVDs() {
			addCurrentAVD();
			return avds;
		}

	}

	private List<AVD> avds;

	private static IPath externalPath;

	private static Android external;

	public Android(IPath pathToAndroid) {
		super(pathToAndroid);
	}

	public static Android getExternal() {
		IPath sdkPath = Activator.getDefault().getExternalAndroidSDKPath();
		if (!sdkPath.equals(externalPath)) {
			String extension = "";
			if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
				extension = ".bat";
			}

			IPath path = sdkPath == null ? null : sdkPath.append("tools/android" + extension);
			external = new Android(path);
			externalPath = sdkPath;
		}
		return external;
	}

	public void refresh() {
		avds = null;
	}

	public synchronized List<AVD> listAVDs() throws CoreException {
		if (avds == null) {
			AVDHandler avdHandler = new AVDHandler();
			execute(new String[] {
					getToolPath().getAbsolutePath(),
					"list", "avds"
			}, avdHandler, null, CoreMoSyncPlugin.LOG_CONSOLE_NAME, false);

			avds = avdHandler.getAVDs();
		}
		return avds;
	}

	@Override
	protected String getToolName() {
		return "Android";
	}

	public boolean hasAVD(String avd) throws CoreException {
		// We make use of how the equals method is impl.
		return listAVDs().contains(new AVD(avd, AVD.UNKNOWN_API_LEVEL));
	}

	public void launchUI(boolean fork) throws CoreException {
		execute(new String[] { getToolPath().getAbsolutePath() }, null, null, CoreMoSyncPlugin.LOG_CONSOLE_NAME, fork);
	}

}
