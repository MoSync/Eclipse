package com.mobilesorcery.sdk.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public abstract class MoSyncCommandHandler extends AbstractHandler {

    private static final int ANY = Integer.MIN_VALUE;

    protected IResource extractFirstResource(ISelection selection) {
        return extractFirstResource(selection, ANY);
    }

    protected IResource extractFirstResource(ISelection selection, int type) {
        	List<IResource> resources = extractResources(selection);
		for (IResource resource : resources) {
			if (type == ANY || resource.getType() == type) {
				return resource;
			}
		}
		
		return null;
	}
	
	protected List<IResource> extractResources(ISelection selection) {
		List elements = ((IStructuredSelection) selection).toList();
		return extractResources(elements);
	}

	private List<IResource> extractResources(List elements) {
		List<IResource> resources = new ArrayList<IResource>();
		for (Object element : elements) {
			if (element instanceof IAdaptable) {
				IResource resource = (IResource) ((IAdaptable) element)
						.getAdapter(IResource.class);
				if (resource != null) {
					resources.add(resource);
				}
			}
		}

		return resources;
	}



}
