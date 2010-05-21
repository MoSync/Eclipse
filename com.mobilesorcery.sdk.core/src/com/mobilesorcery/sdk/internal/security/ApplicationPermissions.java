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
package com.mobilesorcery.sdk.internal.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.core.security.ICommonPermissions;

/**
 * The default application permissions implementation; automatically keeps
 * the associated project's properties in sync.
 * @author Mattias Bybro
 *
 */
public class ApplicationPermissions implements IApplicationPermissions {
    
    public final static String APPLICATION_PERMISSIONS_PROP = MoSyncBuilder.BUILD_PREFS_PREFIX + "app.permissions";

    private List<String> availablePermissions;
    private TreeSet<String> requiredPermissions;
    private MoSyncProject project;

    public ApplicationPermissions(MoSyncProject project) {
        this.project = project;
        this.availablePermissions = Arrays.asList(ICommonPermissions.ALL_PERMISSIONS);
        setRequiredPermissions(PropertyUtil.getStrings(project, APPLICATION_PERMISSIONS_PROP));
    }
    
    public List<String> getRequiredPermissions() {
        return new ArrayList<String>(requiredPermissions);
    }

    public void setRequiredPermissions(List<String> required) {
        this.requiredPermissions = new TreeSet<String>(required);
        PropertyUtil.setStrings(project, APPLICATION_PERMISSIONS_PROP, required.toArray(new String[0]));
    }
    
    private void setRequiredPermissions(String[] required) {
        setRequiredPermissions(Arrays.asList(required));
    }

    public List<String> getAvailablePermissions() {
        return availablePermissions;
    }
    
    public boolean isPermissionGranted(String key) {
        return requiredPermissions.contains(key);
    }

    
}
