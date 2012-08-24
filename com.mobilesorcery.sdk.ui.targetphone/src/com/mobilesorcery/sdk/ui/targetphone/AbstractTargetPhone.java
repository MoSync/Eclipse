/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.mobilesorcery.sdk.ui.targetphone;

import java.util.HashMap;

import com.mobilesorcery.sdk.profiles.IProfile;

public abstract class AbstractTargetPhone implements ITargetPhone {

    private String name;

	private final HashMap<Integer, IProfile> preferredProfile = new HashMap<Integer, IProfile>();

	private final String transportId;

    public AbstractTargetPhone(String name, String transportId) {
    	this.name = name;
    	this.transportId = transportId;
    }

	@Override
	public String getName() {
		return name;
	}

	protected void setName(String name) {
	    this.name = name;
	}

	@Override
	public IProfile getPreferredProfile(int profileManagerType) {
		return preferredProfile.get(profileManagerType);
	}

	@Override
	public ITargetPhoneTransport getTransport() {
		return TargetPhonePlugin.getDefault().getTargetPhoneTransport(transportId);
	}

	@Override
	public void setPreferredProfile(int profileType, IProfile preferredProfile) {
		this.preferredProfile.put(profileType, preferredProfile);
	}

}
