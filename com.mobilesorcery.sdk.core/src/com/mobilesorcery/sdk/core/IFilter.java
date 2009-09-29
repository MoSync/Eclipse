package com.mobilesorcery.sdk.core;

/**
 * <p>A generic filter class.</p>
 * @author Mattias Bybro
 *
 * @param <T> The type this filter should apply to
 */
public interface IFilter<T> {

	public boolean accept(T obj);
	
}
