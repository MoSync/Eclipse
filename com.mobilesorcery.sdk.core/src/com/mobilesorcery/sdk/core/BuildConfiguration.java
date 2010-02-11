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

import java.util.Map;


public class BuildConfiguration implements IBuildConfiguration {

	private MoSyncProject project;
	private String id;
	private NameSpacePropertyOwner properties;

	public BuildConfiguration(MoSyncProject project, String id) {
		this.project = project;
		this.id = id;
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

}
