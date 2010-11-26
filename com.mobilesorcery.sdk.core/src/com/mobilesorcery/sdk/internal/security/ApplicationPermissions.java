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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.Path;

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

    private final static String ROOT = "__root";
    
    private TreeSet<String> availablePermissions = new TreeSet<String>();
    private TreeMap<String, Set<String>> availablePermissionLookup = new TreeMap<String, Set<String>>();
    private TreeSet<String> requestedPermissions;
    private MoSyncProject project;

    private boolean isWorkingCopy = false;
    
    public ApplicationPermissions(MoSyncProject project) {
        this.project = project;
        for (int i = 0; i < ICommonPermissions.ALL_PERMISSIONS.length; i++) {
            addAvailablePermission(ICommonPermissions.ALL_PERMISSIONS[i]);
        }
        
        refresh();
    }
    
    /**
     * Refreshes this applicatoin permission object using
     * the associated project's properties.
     */
    public void refresh() {
        init(project.getProperty(APPLICATION_PERMISSIONS_PROP));    	
    }
    
    private void init(String applicationPermissionProperty) {
        requestedPermissions = new TreeSet<String>(Arrays.asList(PropertyUtil.toStrings(applicationPermissionProperty)));
    }
    
    private void addAvailablePermission(String permission) {
        String parent = getParentPermission(permission);
        if (parent == null) {
            parent = ROOT;
        }
        Set<String> lookupSet = availablePermissionLookup.get(parent);
        if (lookupSet == null) {
            lookupSet = new TreeSet<String>();
            availablePermissionLookup.put(parent, lookupSet);
        }
    
        lookupSet.add(permission);
    }
    
    public List<String> getRequestedPermissions() {
        return new ArrayList<String>(requestedPermissions);
    }

    public void resetRequestedPermissions(List<String> requested) {
        this.requestedPermissions = new TreeSet<String>(requested);
        save();
    }

    public void setRequestedPermission(String requested, boolean set) {
        setRequestedPermission(requested, set, true);
    }
    
    public void setRequestedPermissions(List<String> requested, boolean set) {
        for (String oneRequested : requested) {
        	setRequestedPermission(oneRequested, set);
        }
    }
    
    private void setRequestedPermission(String requested, boolean set, boolean setSubPermissions) {
        if (requested == null) {
            return;
        }
        
        if (set) {
            requestedPermissions.add(requested);
            if (setSubPermissions) {
                List<String> subPermissions = getAvailablePermissions(requested);
                requestedPermissions.addAll(subPermissions);
            }
        } else {
            requestedPermissions.remove(requested);
            if (setSubPermissions) {
                List<String> subPermissions = getAvailablePermissions(requested);
                requestedPermissions.removeAll(subPermissions);
            }
        }
        
        String parentPermission = getParentPermission(requested);
        setRequestedPermission(parentPermission, allSubPermissionsSet(parentPermission), false);
        save();
    }
    
    private boolean allSubPermissionsSet(String parentPermission) {
        TreeSet<String> available = new TreeSet<String>(getAvailablePermissions(parentPermission));
        available.removeAll(requestedPermissions);
        return available.isEmpty();
    }

    private void save() {
        if (!isWorkingCopy) {
            String propertyString = toPropertyString(requestedPermissions.toArray(new String[0]));
            project.setProperty(APPLICATION_PERMISSIONS_PROP, propertyString);
        }
    }
    
    public final static String toPropertyString(String... permissions) {
        return PropertyUtil.fromStrings(permissions);
    }

    public List<String> getAvailablePermissions(String parentPermission) {
        if (parentPermission == null) {
            parentPermission = ROOT;
        }
        
        Set<String> availablePermissions = availablePermissionLookup.get(parentPermission);
        ArrayList<String> result = new ArrayList<String>();
        if (availablePermissions != null) {
            result.addAll(availablePermissions);
        }
        return result;
    }
    
    public static String getParentPermission(String key) {
        Path permissionPath = new Path(key);
        return permissionPath.segmentCount() > 1 ? permissionPath.removeLastSegments(1).toPortableString() : null;
    }
    
    public boolean isPermissionRequested(String key) {
        if (key == null) {
            return false;
        }
        
        return requestedPermissions.contains(key) || isPermissionRequested(getParentPermission(key));
    }

    public IApplicationPermissions createWorkingCopy() {
        ApplicationPermissions copy = new ApplicationPermissions(project);
        copy.isWorkingCopy = true;
        return copy;
    }

    public void apply(IApplicationPermissions workingCopy) {
        resetRequestedPermissions(workingCopy.getRequestedPermissions());
    }
    
    public String toString() {
        return requestedPermissions.toString();
    }

    public static IApplicationPermissions getDefaultPermissions(MoSyncProject project) {
        ApplicationPermissions result = new ApplicationPermissions(project);
        result.isWorkingCopy = true;
        result.init(project.getDefaultProperty(APPLICATION_PERMISSIONS_PROP));
        return result;
    }
    
}
