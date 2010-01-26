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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.FeatureFilterFactory;

public class FeatureFilter extends AbstractDeviceFilter {

    private HashSet<String> featureIds = new HashSet<String>();

    public FeatureFilter() {
    }

    /**
     * 
     * @param featureIds For instance <code>MA_PROF_BUG_PLATFORMREQUESTHTTPLAUNCHREQUIRESEXIT</code> 
     */
    public void setFeatureIds(String[] featureIds) {
        this.featureIds = new HashSet(Arrays.asList(featureIds));       
    }

    public void addFeatureId(String featureId) {
        featureIds.add(featureId);
    }

    public String[] getFeatureIds() {
        return featureIds.toArray(new String[0]);
    }
    
    public boolean acceptProfile(IProfile profile) {
        boolean acceptIfRequired = acceptProfileIfRequired(profile);
        return required == acceptIfRequired; 
    }
    
    private boolean acceptProfileIfRequired(IProfile profile) {
        for (Iterator<String> featureIds = this.featureIds.iterator(); featureIds.hasNext(); ) {
            String featureId = featureIds.next();
            if (!acceptProfileIfRequired(profile, featureId)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean acceptProfileIfRequired(IProfile profile, String featureId) {
        Object result = profile.getProperties().get(featureId);
        if (result instanceof Boolean) {
            return ((Boolean)result).booleanValue();
        }
        
        return false;
    }
    
    public String toString() {        
        String[] featureIds = this.featureIds.toArray(new String[0]);
        String[] descs = new String[featureIds.length];
        for (int i = 0; i < featureIds.length; i++) {
            descs[i] = MoSyncTool.getDefault().getFeatureDescription(featureIds[i]);
        }
        
        String featureStr = descs.length == 1 ? " feature: " : " features: ";
        return (required ? "Required" : "Disallowed") + featureStr + Util.join(descs, ", ");
    }

    public void saveState(IMemento memento) {
        memento.putString("feature-ids", Util.join(featureIds.toArray(new String[0]), ","));
        memento.putInteger("require", required ? REQUIRE : DISALLOW);
    }

    public String getFactoryId() {
        return FeatureFilterFactory.ID;
    }


}
