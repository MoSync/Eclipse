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

import java.util.Set;

import org.eclipse.core.resources.IResource;

public class ResourceFilter implements IFilter<IResource> {

	private Set<IResource> acceptedResources;

	public ResourceFilter(Set<IResource> acceptedResources) {
		this.acceptedResources = acceptedResources;
	}
	
	public boolean accept(IResource resource) {
		return acceptedResources != null && acceptedResources.contains(resource);
	}

}
