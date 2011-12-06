package com.mobilesorcery.sdk.builder.blackberry;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.core.Version;

public class Simulator extends JDE {

	private final static Pattern VERSION_PATTERN = Pattern.compile("\\D*(.*)");

	protected Simulator(IPath path, Version version) {
		super(path, version);
	}

	@Override
	public Version guessVersion() throws IOException {
		return internalGuessVersion(getLocation(), 2);
	}

	private Version internalGuessVersion(IPath path, int maxDepth) {
		if (path.isRoot()) {
			return null;
		}

		// Older simulators are be default installed with the version # + some extra stuff at the end
		String filename = path.lastSegment();
		Version v = new Version(filename);
		if (!v.isValid()) {
			// Where as newer ones are usually located in the JDE directory
			Matcher matcher = VERSION_PATTERN.matcher(filename);
			if (matcher.matches()) {
				String versionStr = matcher.group(1);
				v = new Version(versionStr).truncate(Version.MICRO);
			}
		}

		if (!v.isValid() && maxDepth > 0) {
			return internalGuessVersion(path.removeLastSegments(1), maxDepth - 1);
		}

		return v.isValid() ? v : null;
	}
}
