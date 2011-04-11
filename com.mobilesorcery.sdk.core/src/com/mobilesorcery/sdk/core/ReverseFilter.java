package com.mobilesorcery.sdk.core;

public class ReverseFilter<T> implements IFilter<T> {

	private IFilter<T> delegate;

	public ReverseFilter(IFilter<T> delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public boolean accept(T obj) {
		return !delegate.accept(obj);
	}

}
