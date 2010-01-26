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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

public class DependencyManager<T> {

	public static final int DEPTH_INFINITE = Integer.MAX_VALUE;

	private HashMap<T, HashSet<T>> dependencyMap = new HashMap<T, HashSet<T>>();

	private HashMap<T, HashSet<T>> reverseDependencyMap = new HashMap<T, HashSet<T>>();
	
	private final Set<T> emptySet = Collections.unmodifiableSet(new HashSet<T>());
	
	public List<T> computeDependenciesOf(T obj) throws CoreException {
		Set<T> result = getDependenciesOf(obj);
		if (result == null) {
			return null; 
		}
		
		return new ArrayList<T>(result);
	}
	
	public Set<T> getDependenciesOf(T obj) {
		Set<T> result = dependencyMap.get(obj);
		result = result == null ? emptySet : result;
		return new HashSet<T>(result);
	}
	
	public Set<T> getDependenciesOf(Collection<T> list) {
		HashSet<T> result = new HashSet<T>();
		for (T obj : list) {
			HashSet<T> intermediateResult = dependencyMap.get(obj);
			if (intermediateResult != null) {
				result.addAll(intermediateResult);
			}
		}
		
		return result;		
	}
	
	public Set<T> getReverseDependenciesOf(T obj) {
		HashSet<T> result = reverseDependencyMap.get(obj);
		return result == null ? emptySet : result;
	}
	
	public void addDependency(T from, T to) {
		Set<T> dependencies = lazyInit(dependencyMap, from, new HashSet<T>());
		Set<T> reverseDependencies = lazyInit(reverseDependencyMap, to, new HashSet<T>());
		dependencies.add(to);
		reverseDependencies.add(from);
	}
	
	public void removeDependency(T from, T to) {
		Set<T> dependencies = lazyInit(dependencyMap, from, new HashSet<T>());
		Set<T> reverseDependencies = lazyInit(reverseDependencyMap, to, new HashSet<T>());
		
		dependencies.remove(to);
		reverseDependencies.remove(from);		
	}
	
	public void clearDependencies(Collection<T> objs) {
		for (T obj : objs) {
			clearDependencies(obj);
		}
	}
	
	/**
	 * Clears all dependencies for a given object.
	 * If B depends on A, then clearDependencies(A)
	 * will remove this dependency. The reverse is not true,
	 * so if A depends on B the dependency is kept.
	 * That is, only outgoing edges are cleared; incoming edges
	 * are kept.
	 * @param obj
	 */
	public void clearDependencies(T obj) {
		Set<T> dependencies = new HashSet<T>(getDependenciesOf(obj));
		
		for (T dependency : dependencies) {
			removeDependency(obj, dependency);
		}
		
		dependencyMap.remove(obj);
	}

	public void setDependencies(T from, Collection<T> toList) {
		clearDependencies(from);
		for (T to : toList) {
			addDependency(from, to);
		}
	}
	
	public void setDependencies(T from, IDependencyProvider<T> provider) throws CoreException {
		Map<T, Collection<T>> dependencies = provider.computeDependenciesOf(from);
       	
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
       		CoreMoSyncPlugin.trace("Setting dependencies of {0} to {1}", from, dependencies);
       	}
       	
		for (T dependency : dependencies.keySet()) {
			setDependencies(dependency, dependencies.get(dependency));
		}	
	}

	public void setDependencies(Collection<T> fromList, IDependencyProvider<T> provider) throws CoreException {
		for (T from : fromList) {
			setDependencies(from, provider);
		}
	}
	
	private <K, V> V lazyInit(Map<K, V> map, K key, V valueIfNoKey) {
		V value = map.get(key);
		if (value == null) {
			value = valueIfNoKey;
			map.put(key, value);
		}
		
		return value;
	}

	public Set<T> getReverseDependenciesOf(T obj, int depth) {
		HashSet<T> result = new HashSet<T>();
		innerGetReverseDependenciesOf(obj, depth, result, new HashSet<T>());
		return result;
	}
	
	public Set<T> getReverseDependenciesOf(List<T> objs, int depth) {
		HashSet<T> result = new HashSet<T>();
		for (T obj : objs) {
			innerGetReverseDependenciesOf(obj, depth, result, new HashSet<T>());
		}
		return result;		
	}

	public void clear() {
		dependencyMap.clear();
		reverseDependencyMap.clear();
	}
	
	private void innerGetReverseDependenciesOf(T obj, int depth, Set<T> result, Set<T> alreadyProcessed) {
		if (depth <= 0) {
			return;
		}
		
		Set<T> reverseDependencies = new HashSet<T>(getReverseDependenciesOf(obj));
		result.addAll(reverseDependencies);
		
		for (T reverseDependency : reverseDependencies) {
			if (!alreadyProcessed.contains(reverseDependency)) {
				alreadyProcessed.add(reverseDependency);
				innerGetReverseDependenciesOf(reverseDependency, depth - 1, result, alreadyProcessed);				
			}			
		}
	}
	
	public String toString() {
		// For debugging.
		StringBuffer result = new StringBuffer();
		for (T dependency : dependencyMap.keySet()) {
			result.append(dependency);
			result.append(" --> ");
			result.append(getDependenciesOf(dependency));
			result.append("\n");
		}
		
		return result.toString();
	}


}
