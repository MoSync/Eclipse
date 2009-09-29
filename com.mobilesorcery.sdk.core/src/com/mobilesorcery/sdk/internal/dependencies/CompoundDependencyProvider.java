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
