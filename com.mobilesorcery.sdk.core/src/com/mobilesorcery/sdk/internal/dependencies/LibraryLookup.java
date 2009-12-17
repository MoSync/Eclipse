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
package com.mobilesorcery.sdk.internal.dependencies;

import java.io.File;
import java.util.HashMap;

import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.core.Pair;

/**
 * <p>A class for handling library dependencies. If a library, external or
 * internal to the Eclipse workspace, is recompiled, then all projects
 * that depend on that library should be aware of this (however, those
 * projects should <i>not</i> be rebuilt automatically, so we do not
 * implement this as a dependency provider.)</p>
 * <p>The current implementation do only care about <i>newer</i> libraries,
 * so the use case is 'build lib then build project'.</p>
 * @author Mattias Bybro
 *
 */
public class LibraryLookup {

	public static final int NO_LIBRARY_LOCATIONS = -1;
	
	private IPath[] libraryPaths;
	private IPath[] libraries;

	public LibraryLookup(IPath[] libraryPaths, IPath[] libraries) {
		setLibraries(libraries);
		setLibraryPaths(libraryPaths);
	}
	
	void setLibraryPaths(IPath[] libraryPaths) {
		this.libraryPaths = libraryPaths;
	}
	
	void setLibraries(IPath[] libraries) {
		this.libraries = libraries;
	}
	
	public File[] resolveLibraryLocations() {
		if (libraries == null || libraries.length == 0 || libraryPaths == null || libraryPaths.length == 0) {
			return new File[0];
		}
		
		HashMap<IPath, File> locations = new HashMap<IPath, File>();
		
		// To be safe, we need to actually traverse
		// all paths every time...
		for (int i = 0; i < libraries.length; i++) {
			for (int j = 0; j < libraryPaths.length; j++) {
				File file = new File(libraryPaths[j].toFile(), libraries[i].toOSString());
				if (file.exists()) {
					locations.put(libraries[i], file);
					break;
				}
			}
		}
		
		return locations.values().toArray(new File[0]);
	}
	
	/**
	 * Returns the last time one of the resolved libraries
	 * were touched.
	 * @return If there are no resolved libraries, <code>NO_LIBRARY_LOCATIONS</code>
	 * is returned.
	 */
	public long getLastTouched() {
		File[] libraryLocations = resolveLibraryLocations();
		long lastTouched = NO_LIBRARY_LOCATIONS;
		for (int i = 0; i < libraryLocations.length; i++) {
			long libraryModified = libraryLocations[i].lastModified();
			if (libraryModified > lastTouched) {
				lastTouched = libraryModified;
			}
		}
		
		return lastTouched;
	}
	
}
