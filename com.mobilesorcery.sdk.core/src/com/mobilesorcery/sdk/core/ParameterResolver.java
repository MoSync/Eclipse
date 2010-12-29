package com.mobilesorcery.sdk.core;

public interface ParameterResolver {

	/**
	 * Resolves a parameter key into the corresponding value
	 * @param key
	 * @return May return <code>null</code>
	 * @throws ParameterResolverException 
	 */
	public String get(String key) throws ParameterResolverException;
}
