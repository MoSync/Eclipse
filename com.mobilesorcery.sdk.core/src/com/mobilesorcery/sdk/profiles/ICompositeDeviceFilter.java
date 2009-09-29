package com.mobilesorcery.sdk.profiles;

public interface ICompositeDeviceFilter extends IDeviceFilter {

    public static final String FILTER_ADDED = "filter.added";

    public static final String FILTER_REMOVED = "filter.removed";    
    
    void addFilter(IDeviceFilter filter);    
    
    void removeFilter(IDeviceFilter filter);    
    
    IDeviceFilter[] getFilters();

    /**
     * Sends an update event to all listeners of this filter
     * @param filter
     */
    void update(IDeviceFilter child);
    
}
