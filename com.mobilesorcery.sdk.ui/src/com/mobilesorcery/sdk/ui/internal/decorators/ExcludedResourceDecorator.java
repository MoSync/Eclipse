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

import org.eclipse.cdt.debug.internal.ui.OverlayImageDescriptor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class ExcludedResourceDecorator extends LabelProvider implements
		ILabelDecorator {

	public final static String ID = "com.mobilesorcery.sdk.ui.decorator.excluderesource";
	
	public ExcludedResourceDecorator() {
		
	}

	public Image decorateImage(Image image, Object element) {
		if (isExcluded(element)) {
			ImageDescriptor[] overlays = new ImageDescriptor[] { null, null,
					null, null };
			overlays[OverlayImageDescriptor.TOP_RIGHT] = MosyncUIPlugin
					.getDefault().getImageRegistry().getDescriptor(
							MosyncUIPlugin.IMG_OVR_EXCLUDED_RESOURCE);
			OverlayImageDescriptor ovr = new OverlayImageDescriptor(image,
					overlays, new Point(image.getImageData().width, image
							.getImageData().height));
			return ovr.createImage();
		}
		
		return null;
	}

	public String decorateText(String text, Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	boolean isExcluded(Object element) {
		if (element instanceof IAdaptable) {
			element = ((IAdaptable) element).getAdapter(IResource.class);
		}

		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			MoSyncProject project = MoSyncProject.create(resource.getProject());
			if (project != null) {
				return !MoSyncProject.getExclusionFilter(project).accept(
						resource);
			}
		}

		return false;
	}
	
    public void addListener(ILabelProviderListener listener) {
        super.addListener(listener);
    }
    
    public void updateDecorations() {
    	fireLabelProviderChanged(new LabelProviderChangedEvent(this));
    }

}