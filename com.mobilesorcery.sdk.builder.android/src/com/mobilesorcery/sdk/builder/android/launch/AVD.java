package com.mobilesorcery.sdk.builder.android.launch;

/**
 * Represents an Android Virtual Device (AVD).
 * @author mattias.bybro@mosync.com
 *
 */
public class AVD {

	public static final int UNKNOWN_API_LEVEL = -1;

	private final String name;
	private final int apiLevel;

	AVD(String name, int apiLevel) {
		this.name = name;
		this.apiLevel = apiLevel;
	}

	public String getName() {
		return name;
	}

	public int getAPILevel() {
		return apiLevel;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof AVD) {
			AVD other = (AVD) o;
			return getName().equals(other.getName());
		}
		return false;
	}
	@Override
	public String toString() {
		return name + (apiLevel == UNKNOWN_API_LEVEL ? "" : " (" + apiLevel + ")");
	}
}
