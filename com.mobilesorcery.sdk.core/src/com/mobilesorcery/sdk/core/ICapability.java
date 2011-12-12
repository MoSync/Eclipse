package com.mobilesorcery.sdk.core;

public interface ICapability {

	public String getName();

	public Object getValue();

	public String getType();

	public CapabilityFragmentation getFragmentation();

	public CapabilityState getState();

}
