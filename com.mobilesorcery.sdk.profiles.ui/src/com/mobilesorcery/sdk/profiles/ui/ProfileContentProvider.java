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
package com.mobilesorcery.sdk.profiles.ui;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;

public class ProfileContentProvider implements ITreeContentProvider {
    private final static Object[] EMPTY = new Object[0];

    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IVendor) {
            return ((IVendor) parentElement).getProfiles();
        } else {
            return EMPTY;
        }
    }

    public Object getParent(Object element) {
        if (element instanceof IProfile) {
            return ((IProfile) element).getVendor();
        } else {
            return null;
        }
    }

    public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }

    public Object[] getElements(Object inputElement) {
        return (Object[]) inputElement;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}