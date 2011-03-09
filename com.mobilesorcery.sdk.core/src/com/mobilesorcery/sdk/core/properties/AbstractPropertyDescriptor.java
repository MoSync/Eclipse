package com.mobilesorcery.sdk.core.properties;

public abstract class AbstractPropertyDescriptor<PropertyType> implements IPropertyDescriptor<PropertyType>{
	
	private String name;
	private IPropertyStorage<PropertyType> storage;

	public AbstractPropertyDescriptor(String name, IPropertyStorage<PropertyType> storage) {
		this.name = name;
		this.storage = storage;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void load(Object container) {
		storage.load(container);
	}

	@Override
	public void save(Object container) {
		storage.save(container);
	}
	
	@Override
	public PropertyType get(Object container) {
		return storage.get(container);
	}
	
	@Override
	public void set(Object container, PropertyType value) {
		storage.set(container, value);
	}

}
