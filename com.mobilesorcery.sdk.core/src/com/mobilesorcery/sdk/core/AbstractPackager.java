package com.mobilesorcery.sdk.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

public abstract class AbstractPackager implements IPackager {

	private Map<String, String> parameters = new HashMap<String, String>();

	public void setParameter(String param, String value) throws CoreException {
		parameters.put(param, value);
	}
	
	protected Map<String, String> getParameters() {
		return parameters;
	}
	
	public boolean isFinalizerBuild() {
		return Boolean.parseBoolean(parameters.get(MoSyncBuilder.IS_FINALIZER_BUILD));
	}
	
	public boolean shouldUseDebugRuntimes() {
		return Boolean.parseBoolean(parameters.get(MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS));
	}

}
