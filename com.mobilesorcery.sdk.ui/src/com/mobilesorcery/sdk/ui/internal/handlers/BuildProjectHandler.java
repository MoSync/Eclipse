/*  Copyright (C) 2010 Mobile Sorcery AB

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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.actions.BuildAction;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.ui.MoSyncCommandHandler;

/**
 * A command handler for building. This handler accepts the
 * command parameter <code>com.mobilesorcery.sdk.buildtype</code>
 * having the values as per defined in <code>IncrementalProjectBuilder</code>.
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class BuildProjectHandler extends MoSyncCommandHandler {

    public BuildProjectHandler() {

    }

    @Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        // TODO: command parameter not fully impl
        String buildType = event.getParameter("com.mobilesorcery.sdk.buildtype");
        int buildTypeVal = IncrementalProjectBuilder.FULL_BUILD;
        if (buildType != null) {
            try {
                buildTypeVal = Integer.parseInt(buildType);
            } catch (Exception e) {
                CoreMoSyncPlugin.getDefault().log(e);
            }
        }
        if (selection != null) {
            BuildAction fullBuildAction = new BuildAction(HandlerUtil.getActiveWorkbenchWindow(event), buildTypeVal);
            // This will actually init the correct set of projects to build:
            fullBuildAction.isEnabled();
            fullBuildAction.run();
        }
        return null;
    }

    @Override
	public void setEnabled(Object context) {
        setBaseEnabled(isEnabled());
    }

    @Override
	public boolean isEnabled() {
        // TODO: Support menu commands.
        boolean enabled = !ResourcesPlugin.getWorkspace().isAutoBuilding();
        return enabled;
    }



}
