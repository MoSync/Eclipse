package com.mobilesorcery.sdk.core;

import java.util.Set;

public interface ICapabilities {

	public boolean hasCapability(String capability);
	
	public Set<String> listCapabilities();

	public Object getCapabilityValue(String capability);
}
