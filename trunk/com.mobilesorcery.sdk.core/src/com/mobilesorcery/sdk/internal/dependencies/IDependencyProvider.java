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

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;


/**
 * An interface for computing/providing dependencies.
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface IDependencyProvider<T> {
	
	/**
	 * Given an object, computes a dependency map, where each key object points
	 * to a collection of objects dependent upon it.
	 * @param obj
	 * @return
	 * @throws CoreException If for some reason, the dependencies
	 * could not be resolved
	 */
	public Map<T, Collection<T>> computeDependenciesOf(T obj) throws CoreException;

}
