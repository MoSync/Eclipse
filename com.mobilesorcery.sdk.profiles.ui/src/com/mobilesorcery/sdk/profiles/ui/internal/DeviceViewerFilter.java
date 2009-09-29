package com.mobilesorcery.sdk.profiles.ui.internal;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;

public class DeviceViewerFilter extends ViewerFilter {

    private IDeviceFilter filter;

    public DeviceViewerFilter(IDeviceFilter filter) {
        this.filter = filter;
    }
    
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        return filter.accept(element);
    }

}
