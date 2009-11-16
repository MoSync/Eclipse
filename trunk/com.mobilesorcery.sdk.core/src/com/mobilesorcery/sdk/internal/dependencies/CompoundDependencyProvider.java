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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

public class CompoundDependencyProvider<T> implements IDependencyProvider<T> {

	private IDependencyProvider<T>[] providers;

	public CompoundDependencyProvider(IDependencyProvider<T>... providers) {
		this.providers = providers;
	}

	public Map<T, Collection<T>> computeDependenciesOf(T obj) throws CoreException {
		HashMap<T, Collection<T>> result = new HashMap<T, Collection<T>>();
		for (int i = 0; i < providers.length; i++) {
			Map<T, Collection<T>> providerDependencies = providers[i].computeDependenciesOf(obj);
			for (T providerDependency : providerDependencies.keySet()) {
				Collection<T> intermediateResult = result.get(providerDependency);
				if (intermediateResult == null) {
					result.put(providerDependency, providerDependencies.get(providerDependency));
				} else {
					intermediateResult.addAll(providerDependencies.get(providerDependency));
				}
			}
		}
		
		return result;
	}

}
