package com.mobilesorcery.sdk.profiles.filter;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * A filter that either filters out emulated a devices,
 * or non-emulated devices.
 * @author Mattias Bybro
 *
 */
public class EmulatorDeviceFilter extends AbstractDeviceFilter {

	public final static int EXCLUDE_EMULATORS = 1;
	public final static int EXCLUDE_NON_EMULATORS = 2;

	private int type;
	
	public EmulatorDeviceFilter(int type) {
		this.type = type;
	}
	
	public boolean acceptProfile(IProfile profile) {		
		return type == EXCLUDE_EMULATORS ? !profile.isEmulator() : profile.isEmulator();
	}

	public String getFactoryId() {
		throw new UnsupportedOperationException();
	}

	public void saveState(IMemento memento) {
		throw new UnsupportedOperationException();
	}

}
