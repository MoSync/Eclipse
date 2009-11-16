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
package com.mobilesorcery.sdk.swtbot;

import org.eclipse.core.resources.IResource;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

public class SWTBotProjectExplorer {

	private SWTWorkbenchBot ui;

	public SWTBotProjectExplorer(SWTWorkbenchBot ui) {
		this.ui = ui;
	}
	
	public void select(IResource resource) {
		SWTBotView projectExplorer = ui.viewByTitle("Project Explorer");
		projectExplorer.setFocus();
		SWTBotTree tree = projectExplorer.bot().tree();
		SWTBotTree selection = tree.select(resource.getLocation().lastSegment());
	}
}
