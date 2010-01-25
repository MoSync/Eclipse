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
package com.mobilesorcery.sdk.profiles.filter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbench;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.profiles.ICompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * Sort of an AND filter.
 * 
 * @author Mattias
 * 
 */
public class CompositeDeviceFilter extends AbstractDeviceFilter implements ICompositeDeviceFilter, PropertyChangeListener {

    private static final String CRITERIA = "criteria";    

    private static final String FILTER = "filter";    

    private ArrayList<IDeviceFilter> filters = new ArrayList<IDeviceFilter>();

    public CompositeDeviceFilter() {

    }

    public CompositeDeviceFilter(IDeviceFilter[] filters) {
        this.filters.addAll(Arrays.asList(filters));
    }

    public void addFilter(IDeviceFilter filter) {
        this.filters.add(filter);
        filter.addPropertyChangeListener(this);
        notifyListeners(new PropertyChangeEvent(this, FILTER_ADDED, null, filter));
    }

    public void removeFilter(IDeviceFilter filter) {
        this.filters.remove(filter);
        filter.removePropertyChangeListener(this);
        notifyListeners(new PropertyChangeEvent(this, FILTER_REMOVED, filter, null));
    }

    public IDeviceFilter[] getFilters() {
        return filters.toArray(new IDeviceFilter[0]);
    }

    public void update(IDeviceFilter child) {
        notifyListeners(new PropertyChangeEvent(this, FILTER_CHANGED, null, child));
    }
    
    public boolean acceptProfile(IProfile profile) {
        for (Iterator<IDeviceFilter> filterIterator = filters.iterator(); filterIterator.hasNext();) {
            IDeviceFilter filter = filterIterator.next();
            if (!filter.accept(profile)) {
                return false;
            }
        }
        
        return true;
    }
        
    public void saveState(IMemento memento) {
    	if (filters.size() > 0) {
    		IMemento criteria = memento.createChild(CRITERIA);
      
	        for (Iterator<IDeviceFilter> filterIterator = filters.iterator(); filterIterator.hasNext();) {
	            IDeviceFilter filter = filterIterator.next();
	            IMemento child = criteria.createChild(FILTER);
	            child.putString("type", filter.getFactoryId());
	            filter.saveState(child);
	        }
    	}
    }

    public static CompositeDeviceFilter read(IMemento memento) {
        CompositeDeviceFilter result = new CompositeDeviceFilter();

        IMemento criteria = memento.getChild(CRITERIA);
        if (criteria != null) {
            IMemento[] children = criteria.getChildren(FILTER);
            for (int i = 0; i < children.length; i++) {
                IMemento child = children[i];
                String factoryId = child.getString("type");
                IDeviceFilterFactory factory = CoreMoSyncPlugin.getDefault().getDeviceFilterFactory(factoryId);
                if (factory != null) {
                    IDeviceFilter filter = factory.createFilter(child);
                    if (filter != null) {
                        result.addFilter(filter);
                    }
                }
            }
        }
        
        return result;
    }

    public String getFactoryId() {
        return "com.mobilesorcery.mosync.filters.composite";
    }

	public void propertyChange(PropertyChangeEvent event) {
		// Propagate.
		notifyListeners(event);
	}

}
