package com.mobilesorcery.sdk.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class ChangeBuildConfigContextAction implements 
IMenuCreator, IObjectActionDelegate {

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
				fillMenu((Menu)e.widget);
			}
		});
		return menu;
	}

	protected void fillMenu(Menu widget) {
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
