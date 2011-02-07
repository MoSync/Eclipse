package com.mobilesorcery.sdk.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ParameterResolverException extends Exception {

	private String key;

	public ParameterResolverException(String key, String msg) {
		super("Key " + key + ": " + msg);
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public static CoreException toCoreException(ParameterResolverException e) {
		return new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
	}
}
