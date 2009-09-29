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
