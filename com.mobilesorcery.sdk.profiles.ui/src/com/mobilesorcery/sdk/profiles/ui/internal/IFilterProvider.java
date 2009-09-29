package com.mobilesorcery.sdk.profiles.ui.internal;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;

public interface IFilterProvider {

    String getName();

    /**
     * Returns a filter - may use UI and ask the user.
     */
    IDeviceFilter getFilter();

}
