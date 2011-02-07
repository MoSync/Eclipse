package com.mobilesorcery.sdk.core;

import java.util.List;

public abstract class ParameterResolver {

	/**
	 * Resolves a parameter key into the corresponding value
	 * @param key
	 * @return May return <code>null</code>
	 * @throws ParameterResolverException 
	 */
	public abstract String get(String key) throws ParameterResolverException;
	
	/**
	 * Returns the list of prefixes of this parameter resolver.
	 * A prefix is either:
	 * <ul>
	 * <li>A full key (for example <code>mosync-dir</code></li>
	 * <li>or, a colon-separated paramterized key (for example <code>%project:<i>some-project</i></code>)
	 * </ul>
	 * @return The list of prefixes; in the case of parameterized keys the prefix will end with a colon (<code>:</code>).
	 */
	public abstract List<String> listPrefixes();
	
	/**
	 * <p>Given a prefix, tries to list the available parameters.</p>
	 * <p>Clients may override.</p>
	 * @param prefix
	 * @return The list of available parameters, or <code>null</code> if not applicable.
	 */
	public List<String> listAvailableParameters(String prefix) {
		return null;
	}

	public static String getPrefix(String key) {
		String[] prefixAndParam = key.split(":", 2);
		return prefixAndParam.length == 2 ? prefixAndParam[0] + ":" : null;
	}
	
	public static String getParameter(String key) {
		String[] prefixAndParam = key.split(":", 2);
		return prefixAndParam.length == 2 ? prefixAndParam[1] : null;
	}
}
