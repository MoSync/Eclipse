package com.mobilesorcery.sdk.core;

public interface IProvider<ObjectType, KeyType> {

	public ObjectType get(KeyType key);
	
}
