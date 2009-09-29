package com.mobilesorcery.sdk.profiles;

import java.beans.PropertyChangeListener;

import org.eclipse.ui.IPersistableElement;

public interface IDeviceFilter extends IPersistableElement {
    
    public static final String FILTER_CHANGED = "filter.changed";
    
    public boolean accept(Object vendorOrProfile);
    
    public void addPropertyChangeListener(PropertyChangeListener listener);
    
    public void removePropertyChangeListener(PropertyChangeListener listener);

}
