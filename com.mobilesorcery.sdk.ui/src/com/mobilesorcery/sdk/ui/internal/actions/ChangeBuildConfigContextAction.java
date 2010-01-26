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
package com.mobilesorcery.sdk.ui.internal.actions;

import java.util.Set;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class ChangeBuildConfigContextAction implements IMenuCreator,
		IObjectActionDelegate {

	private MoSyncProject project;

	public ChangeBuildConfigContextAction() {
	}

	public void dispose() {
	}

	public Menu getMenu(Control parent) {
		return null;
	}

	public Menu getMenu(Menu parent) {
		Menu menu = new Menu(parent);
		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				fillMenu((Menu) e.widget);
			}
		});
		return menu;
	}

	protected void fillMenu(Menu menu) {
		MenuItem[] items = menu.getItems();
		for (MenuItem item : items)
			item.dispose();

		if (project.areBuildConfigurationsSupported()) {
			Set<String> cfgs = project.getBuildConfigurations();
			for (String cfg : cfgs) {
				IAction action = new ChangeBuildConfigAction(project, cfg);
				IBuildConfiguration activeCfg = project
						.getActiveBuildConfiguration();
				action.setChecked(activeCfg != null
						&& activeCfg.getId().equals(cfg));

				ActionContributionItem item = new ActionContributionItem(action);
				item.fill(menu, -1);
			}
		} else {
			IAction action = new ActivateBuildConfigurationsAction(project);
			ActionContributionItem item = new ActionContributionItem(action);
			item.fill(menu, -1);
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
	}

	public void selectionChanged(IAction action, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		project = CoreMoSyncPlugin.getDefault().extractProject(ssel.toList());
		action.setMenuCreator(this);
	}

}
