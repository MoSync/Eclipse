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
/**
 *
 */
package com.mobilesorcery.sdk.profiles.filter.elementfactories;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.IDeviceFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.ProfileFilter;

public class ProfileFilterFactory implements IDeviceFilterFactory {

	public static final String ID = "com.mobilesorcery.mosync.filters.profile";

	public ProfileFilterFactory() {
	}

	@Override
	public IDeviceFilter createFilter(IMemento memento) {
		try {
			Integer mgr = memento.getInteger("mgr");
			ProfileFilter result = new ProfileFilter(
					mgr == null ? MoSyncTool.LEGACY_PROFILE_TYPE : mgr);
			result.setStyle(memento.getInteger("require"));
			result.setVendors(memento.getString("vendors").split(","));
			result.setProfiles(memento.getString("profiles").split(","));
			return result;
		} catch (Exception e) {
			return null;
		}

	}
}