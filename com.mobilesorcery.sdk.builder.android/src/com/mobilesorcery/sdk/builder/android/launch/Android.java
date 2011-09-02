package com.mobilesorcery.sdk.builder.android.launch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.LineReader.LineAdapter;

public class Android extends AbstractTool {

	private static class AVDHandler extends LineAdapter {

		private final static Pattern NAME_PATTERN = Pattern.compile("\\s*Name:\\s*(.*)\\s*");
		private final List<String> avds = new ArrayList<String>();

		@Override
		public void newLine(String line) {
			Matcher matcher = NAME_PATTERN.matcher(line);
			if (matcher.matches()) {
				String avdName = matcher.group(1);
				avds.add(avdName);
			}
		}

		public List<String> getAVDs() {
			return avds;
		}

	}

	public Android(IPath pathToAndroid) {
		super(pathToAndroid);
	}

	public static Android getExternal() {
		IPath sdkPath = Activator.getDefault().getExternalAndroidSDKPath();
		String extension = "";
		if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
			extension = ".bat";
		}

		IPath path = sdkPath == null ? null : sdkPath.append("tools/android" + extension);
		return new Android(path);
	}

	public List<String> listAVDs() throws CoreException {
		AVDHandler avdHandler = new AVDHandler();
		execute(new String[] {
				getToolPath().getAbsolutePath(),
				"list", "avds"
		}, avdHandler, null, false);

		return avdHandler.getAVDs();
	}

	@Override
	protected String getToolName() {
		return "Android";
	}

	public boolean hasAVD(String avd) throws CoreException {
		return listAVDs().contains(avd);
	}

}
