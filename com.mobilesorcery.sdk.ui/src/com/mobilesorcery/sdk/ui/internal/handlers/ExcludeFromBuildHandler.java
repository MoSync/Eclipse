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
package com.mobilesorcery.sdk.ui.internal.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.services.IEvaluationService;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.PathExclusionFilter;

public class ExcludeFromBuildHandler extends AbstractHandler {

	public final static String COMMAND_ID = "com.mobilesorcery.sdk.ui.excludefrombuild";
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			List elements = ((IStructuredSelection) selection).toList();
			List<IResource> resources = extractResources(elements);

			PathExclusionFilter.setExcluded(resources, shouldExclude());

			refreshCommandState(event);
			fireHandlerChanged(new HandlerEvent(this, true, false));

		}

		return null;
	}

	private void refreshCommandState(ExecutionEvent event) {
		final IWorkbenchWindow ww = HandlerUtil.getActiveWorkbenchWindow(event);
		if (ww != null) {
			final IEvaluationService service = (IEvaluationService) ww
					.getService(IEvaluationService.class);
			if (service != null) {
				service.requestEvaluation("com.mobilesorcery.sdk.testers.isExcludableFromBuild");
			}
		}

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


	/**
	 * @return true if exclude, false if include
	 */
	protected boolean shouldExclude() {
		return true;
	}

}
