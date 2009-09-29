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
