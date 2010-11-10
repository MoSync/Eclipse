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
package com.mobilesorcery.sdk.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;


public class BuildConfiguration implements IBuildConfiguration, IAdaptable {

    public final static Comparator<IBuildConfiguration> DEFAULT_COMPARATOR = new Comparator<IBuildConfiguration>() {
        public int compare(IBuildConfiguration cfg1, IBuildConfiguration cfg2) {
            return cfg1.getId().compareTo(cfg2.getId());
        }
    };
    
    private final static Set<String> DEFAULT_TYPE_SET = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(RELEASE_TYPE)));
    private final static Set<String> DEFAULT_DEBUG_TYPE_SET = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(DEBUG_TYPE)));

    public static final String TYPE_EXTENSION_POINT = "com.mobilesorcery.core.buildconfigurationtypes";
    
	private MoSyncProject project;
	private String id;
	private NameSpacePropertyOwner properties;
    private HashSet<String> types;

	public BuildConfiguration(MoSyncProject project, String id, String... types) {
		this.project = project;
		this.id = id;
		this.types = new HashSet(Arrays.asList(types));
		properties = new NameSpacePropertyOwner(project, id);
	}

	public String getId() {
		return id;
	}

	public NameSpacePropertyOwner getProperties() {
		return properties;
	}

	public IBuildConfiguration clone(String id) {
		BuildConfiguration clone = new BuildConfiguration(project, id);
		clone.types = types;
		Map<String, String> propertiesClone = getProperties().getProperties(this.id);
		clone.getProperties().applyProperties(propertiesClone);
		return clone;
	}

	/**
	 * Utility method for retrieving a unique configuration id
	 */
	public static String createUniqueId(MoSyncProject project, String suggestedId) {
		String uniqueId = suggestedId;
        int i = 2;

        while (project.getBuildConfiguration(uniqueId) != null) {
            uniqueId = suggestedId + i;
            i++;
        }
        
        return uniqueId;
	}

    public Object getAdapter(Class adapter) {
        if (IBuildConfiguration.class.equals(adapter)) {
            return this;
        } else if (MoSyncProject.class.equals(adapter)) {
            return project;
        }
        
        return null;
    }

    public Set<String> getTypes() {
        if (types.isEmpty()) {
            return DEBUG_ID.equals(id) ? DEFAULT_DEBUG_TYPE_SET : DEFAULT_TYPE_SET;
        }
        return types;
    }
    
    public void setTypes(Collection<String> types) {
        this.types = new HashSet<String>(types);
    }

    public String toString() {
    	return id;
    }
}
