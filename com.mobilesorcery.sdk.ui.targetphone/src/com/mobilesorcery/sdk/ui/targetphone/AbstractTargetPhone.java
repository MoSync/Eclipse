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

import com.mobilesorcery.sdk.profiles.IProfile;

public abstract class AbstractTargetPhone implements ITargetPhone {

    private final String name;

	private IProfile preferredProfile;
	
	private String transportId;
    
    public AbstractTargetPhone(String name, String transportId) {
    	this.name = name;
    	this.transportId = transportId;
    }
    
	public String getName() {
		return name;
	}

	public IProfile getPreferredProfile() {
		return preferredProfile;
	}

	public ITargetPhoneTransport getTransport() {
		return TargetPhonePlugin.getDefault().getTargetPhoneTransport(transportId);
	}

	public void setPreferredProfile(IProfile preferredProfile) {
		this.preferredProfile = preferredProfile;
	}

}