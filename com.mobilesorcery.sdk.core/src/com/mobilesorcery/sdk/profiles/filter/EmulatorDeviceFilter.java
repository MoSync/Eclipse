/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.profiles.filter;

import org.eclipse.ui.IMemento;

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
