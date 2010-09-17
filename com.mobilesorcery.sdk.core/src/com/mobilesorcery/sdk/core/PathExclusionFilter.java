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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.internal.StringMatcher;

public class PathExclusionFilter implements IFilter<IResource> {

	private final static int NEUTRAL = 0;
	private final static int INCLUDE = 1;
	private final static int EXCLUDE = -INCLUDE;

	static class InternalFilter {
		private String filespec;
		private StringMatcher matcher;
		private boolean exclude;

		public InternalFilter(String filespec, boolean exclude) {
			this.filespec = filespec;
			Path filePath = new Path(filespec);
			this.matcher = new StringMatcher(filePath.toPortableString(), true, false);
			this.exclude = exclude;
		}
		
		private InternalFilter(InternalFilter prototype) {
			this(prototype.filespec, prototype.exclude);
		}

		public int accept(IResource resource) {
		    IResource directoryLocation = resource.getProject().findMember(new Path(filespec));
			boolean isDirectoryFilter = directoryLocation != null && directoryLocation.getLocation().toFile().isDirectory();
			boolean match = matcher.match(resource.getProjectRelativePath().toPortableString());
			if (match) {
				return exclude ? EXCLUDE : INCLUDE;
			} else if (isDirectoryFilter && resource.getType() != IResource.PROJECT && resource.getType() != IResource.ROOT) {
				return accept(resource.getParent());
			}
			
			return NEUTRAL;
		}

		public static InternalFilter parse(String filespec) {
			if (filespec.charAt(0) == '+') {
				return parse(filespec.substring(1)).inverse();
			} else if (filespec.charAt(0) == '-') {
				return parse(filespec.substring(1));
			}
			
			return new InternalFilter(filespec, true);
		}

		public InternalFilter inverse() {
			InternalFilter copy = new InternalFilter(this);
			copy.exclude = !exclude;
			return copy;
		}
		
		public boolean equals(Object o) {
			if (o instanceof InternalFilter) {
				return equals((InternalFilter) o);
			}
			
			return false;
		}
		
		public boolean equals(InternalFilter filter) {
			if (filter == null) {
				return false;
			}
			
			return exclude == filter.exclude && filespec.equals(filter.filespec);
		}
		
		public int hashCode() {
			return new Boolean(exclude).hashCode() ^ filespec.hashCode();
		}

		public String getFileSpec() {
			String prefix = exclude ? "" : "+";
			return prefix + filespec;
		}
		
		public String toString() {
			return getFileSpec();
		}
	}
	
	// Todo: create 'internal filters' instead of string[]
	private List<InternalFilter> filters = new ArrayList<InternalFilter>();

	PathExclusionFilter(String[] filespecs) {
		filters = parseFilters(filespecs, true);
	}

	private PathExclusionFilter(List<InternalFilter> filters) {
		this.filters = normalize(filters);
	}
	
	private List<InternalFilter> normalize(List<InternalFilter> filters) {
		HashSet<InternalFilter> added = new HashSet<InternalFilter>();
		ArrayList<InternalFilter> newFilters = new ArrayList<InternalFilter>();
		
		for (InternalFilter filter : filters) {
			// No duplicates, please
			if (!added.contains(filter)) {
				// And some may cancel each other out
				if (added.contains(filter.inverse())) {
					newFilters.remove(filter.inverse());
					added.remove(filter.inverse());
				} else {
					newFilters.add(filter);
					added.add(filter);
				}
			}
		}
		
		return newFilters;
	}
	
	private List<InternalFilter> parseFilters(String[] filespecs, boolean exclude) {
		List<InternalFilter> filters = new ArrayList<InternalFilter>();
		for (int i = 0; i < filespecs.length; i++) {
			if (!Util.isEmpty(filespecs[i])) {
				InternalFilter parsedFilter = InternalFilter.parse(filespecs[i]);
				if (!exclude) {
					parsedFilter = parsedFilter.inverse();
				}
				
				filters.add(parsedFilter);
			}
		}
	
		return normalize(filters);
	}

	public static PathExclusionFilter parse(String[] filespecs) {
		return new PathExclusionFilter(filespecs);
	}

	public boolean accept(IResource resource) {
		return !inverseAccept(resource);
	}
	
	public boolean inverseAccept(IResource resource) {
		int state = NEUTRAL;
		for (int i = 0; i < filters.size(); i++) {
			int newState = filters.get(i).accept(resource);
			state = newState == NEUTRAL ? state : newState;
		}

		boolean result = state == EXCLUDE;
		return result;
	}

	public PathExclusionFilter addExclusions(List<String> filespecs, boolean excluded) {
		List<InternalFilter> newFilespecs = new ArrayList<InternalFilter>(filters);
		newFilespecs.addAll(parseFilters(filespecs.toArray(new String[0]), excluded));
		return new PathExclusionFilter(newFilespecs);
	}


	/**
	 * <p>Convenience method to set the excluded / included state
	 * of a set of resources. This method will automatically determine
	 * which project each resource belongs to and update the corresponding
	 * exclusion filter (if applicable).</p>
	 * @param resources
	 * @param excluded
	 * @return The number of resources that was actually added to the list of
	 * excluded/included list; if a filter already filters a resource, it will
	 * not be added.
	 */
	public static int setExcluded(List<IResource> resources, boolean excluded) {
		int added = 0;
		for (IResource resource : resources) {
			MoSyncProject project = MoSyncProject.create(resource.getProject());
			IPath path = resource.getProjectRelativePath();
            PathExclusionFilter filter = MoSyncProject.getExclusionFilter(project, true);
            PathExclusionFilter filterWithoutStandardExcludes = MoSyncProject.getExclusionFilter(project, false);
			boolean accept = filter.accept(resource);
			boolean shouldAdd = excluded ? accept : !accept;
			if (shouldAdd) {
				added++;
				filterWithoutStandardExcludes = filterWithoutStandardExcludes.addExclusions(Arrays.asList(path.toPortableString()), excluded);
				MoSyncProject.setExclusionFilter(project, filterWithoutStandardExcludes);				
			}
		}
		
		return added;
	}
	
	/**
	 * Returns a string array that can be used to construct
	 * a new, equals path exclusion filter.
	 * @return
	 */
	public String[] getFileSpecs() {
		ArrayList<String> result = new ArrayList<String>();
		for (InternalFilter filter : filters) {
			result.add(filter.getFileSpec());
		}
		
		return result.toArray(new String[0]);
	}
	
	public String toString() {
	    return Util.join(getFileSpecs(), " ");
	}

}
