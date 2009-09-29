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