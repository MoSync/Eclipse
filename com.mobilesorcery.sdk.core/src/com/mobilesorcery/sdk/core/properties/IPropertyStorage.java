package com.mobilesorcery.sdk.core.properties;

public interface IPropertyStorage<PropertyType> {

	public void load(Object container);
	
	public void save(Object container);
	
	public PropertyType get(Object container);
	
	public void set(Object container, PropertyType value);
}