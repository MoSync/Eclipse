package com.mobilesorcery.sdk.ui.internal.navigationext;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

public class ReleasePackage implements IAdaptable {

	private final IContainer resource;

	public ReleasePackage(IContainer resource) {
		this.resource = resource;
	}

	public IContainer getUnderlyingResource() {
		return resource;
	}

	@Override
	public String toString() {
		return "Release Packages";
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IResource.class || adapter == IContainer.class) {
			return getUnderlyingResource();
		}
		return null;
	}

}
