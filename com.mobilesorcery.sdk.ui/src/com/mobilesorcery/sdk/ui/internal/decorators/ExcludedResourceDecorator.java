/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.mobilesorcery.sdk.ui.internal.decorators;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class ExcludedResourceDecorator extends LabelProvider implements
		ILightweightLabelDecorator {

	public ExcludedResourceDecorator() {
	System.err.println("!");	
	}
	
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof IAdaptable) {
			element = ((IAdaptable) element).getAdapter(IResource.class);
		}
		
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			MoSyncProject project = MoSyncProject.create(resource.getProject());
			if (project != null) {
				if (!MoSyncProject.getExclusionFilter(project).accept(resource)) {
					decoration.addOverlay(MosyncUIPlugin.getDefault()
							.getImageRegistry().getDescriptor(
									MosyncUIPlugin.IMG_OVR_EXCLUDED_RESOURCE));
				}
			}
		}
	}


}