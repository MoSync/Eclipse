package com.mobilesorcery.sdk.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.IFilter;

public class PathExclusionFilter implements IFilter<IResource> {
	
	private String[] filespecs;

	PathExclusionFilter(String[] filespecs) {
		this.filespecs = filespecs;
	}

	public static PathExclusionFilter parse(String[] filespecs) {
		return new PathExclusionFilter(filespecs);
	}

	public boolean accept(IResource resource) {
		return !inverseAccept(resource);
	}
	
	public boolean inverseAccept(IResource resource) {
		for (int i = 0; i < filespecs.length; i++) {
			if (accept(resource, filespecs[i])) {
				return true;
			}
		}
		
		return false;
	}
	
	protected boolean accept(IResource resource, String filespec) {
		// Just simple exclusion list for now.
		Path filePath = new Path(filespec);
		IResource fileResource = resource.getProject().findMember(filePath, false);
		return resource.equals(fileResource);
	}

}
