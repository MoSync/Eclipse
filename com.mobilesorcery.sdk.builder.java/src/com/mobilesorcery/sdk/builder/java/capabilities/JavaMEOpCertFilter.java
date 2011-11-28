package com.mobilesorcery.sdk.builder.java.capabilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.builder.java.Activator;
import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.core.security.ICommonPermissions;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.filter.AbstractDeviceFilter;

public class JavaMEOpCertFilter extends AbstractDeviceFilter {

	private static final HashSet<String> TAINTED_CAPABILITIES = new HashSet<String>(Arrays.asList(new String[] {
		ICommonPermissions.LOCATION,
		ICommonPermissions.BLUETOOTH,
		ICommonPermissions.CALENDAR,
		ICommonPermissions.CAMERA,
		ICommonPermissions.CONTACTS,
		ICommonPermissions.CALENDAR,
		ICommonPermissions.FILE_STORAGE
	}));

	public static final String FACTORY_ID = Activator.PLUGIN_ID + ".capabilities.elementfactory";

	private String[] requestedCapabilities;

	private boolean doesFilter = false;

	public JavaMEOpCertFilter(String[] requestedCapabilities) {
		init(requestedCapabilities);
	}

	public JavaMEOpCertFilter(ICapabilities requestedCapabilities) {
		ArrayList<String> capabilitiesAsStr = new ArrayList<String>();
		for (String tainted : TAINTED_CAPABILITIES) {
			if (requestedCapabilities.hasCapability(tainted)) {
				capabilitiesAsStr.add(tainted);
			}
		}
		init(capabilitiesAsStr.toArray(new String[0]));
	}

	private void init(String[] requestedCapabilities) {
		// NOTE: we could get away for now with just storing the 'should filter',
		// but we'll soon add some even smarter stuff :)
		this.requestedCapabilities = requestedCapabilities;
		doesFilter = false;
		for (String requestedCapability : requestedCapabilities) {
			doesFilter |= TAINTED_CAPABILITIES.contains(requestedCapability);
		}

		setStyle(DISALLOW);
	}

	public boolean doesFilter() {
		return doesFilter;
	}

	@Override
	public String getFactoryId() {
		return FACTORY_ID;
	}

	@Override
	public void saveState(IMemento memento) {
		for (String requestedCapability : requestedCapabilities) {
			IMemento child = memento.createChild("requested-capability");
			child.putString("id", requestedCapability);
		}
	}

	@Override
	public boolean acceptProfile(IProfile profile) {
		// TODO: Create api to extract packager from runtime id
		boolean platformIsJavaME = profile.getProperties().containsKey("MA_PROF_STRING_PLATFORM_JAVAME");
		return !doesFilter || !platformIsJavaME;
	}

	@Override
	public String toString() {
		return "JavaME profiles that might require operator certificates";
	}
}
