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
package com.mobilesorcery.sdk.ui;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;

public class ProfileContentProvider implements ITreeContentProvider {
    private final static Object[] EMPTY = new Object[0];
	private MoSyncProject project;

    public ProfileContentProvider() {
    	this(null);
    }

    public ProfileContentProvider(MoSyncProject project) {
		this.project = project;
	}

	public void setProject(MoSyncProject project) {
		this.project = project;
	}


	@Override
	public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IVendor) {
        	IVendor vendor = (IVendor) parentElement;
            return project == null ?
            	vendor.getProfiles() :
            	project.getFilteredProfiles(vendor);
        } else {
            return EMPTY;
        }
    }

    @Override
	public Object getParent(Object element) {
        if (element instanceof IProfile) {
            return ((IProfile) element).getVendor();
        } else {
            return null;
        }
    }

    @Override
	public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }

    @Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof MoSyncTool) {
			MoSyncTool tool = (MoSyncTool) inputElement;
			ProfileManager mgr = tool.getProfileManager(MoSyncTool.LEGACY_PROFILE_TYPE);
			return mgr.getVendors();
		} else if (inputElement instanceof MoSyncProject) {
			MoSyncProject project = (MoSyncProject) inputElement;
			return project.getFilteredVendors();
		}
        return (Object[]) inputElement;
    }

    @Override
	public void dispose() {
    }

    @Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}