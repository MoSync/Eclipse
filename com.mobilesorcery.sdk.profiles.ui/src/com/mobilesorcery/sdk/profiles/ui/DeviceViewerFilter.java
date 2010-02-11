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
package com.mobilesorcery.sdk.profiles.ui;

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
