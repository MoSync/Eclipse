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
package com.mobilesorcery.sdk.product.intro.actions;

import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.importproject.Activator;
import com.mobilesorcery.sdk.product.intro.ImportExampleProject;

public class ImportExampleAction implements IIntroAction {

	public void run(IIntroSite site, Properties params) {
		IIntroPart intro = PlatformUI.getWorkbench().getIntroManager().getIntro();
		boolean success = false;
		if (params.containsKey("example") && params.containsKey("extension")) { //$NON-NLS-1$ //$NON-NLS-2$
			String example = params.getProperty("example"); //$NON-NLS-1$
			String extension = params.getProperty("extension"); //$NON-NLS-1$
			String extradir = ""; //$NON-NLS-1$
			if (params.containsKey("extradir")) { //$NON-NLS-1$
				extradir = params.getProperty("extradir"); //$NON-NLS-1$
			}
			try {
				ImportExampleProject exProj = new ImportExampleProject(example, extension, extradir);
				success = exProj.doImport();
			} catch (Exception e)	{
				e.printStackTrace();
				StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e), StatusManager.SHOW);
			}
		} else {
			MessageDialog.openError(site.getShell(), Messages.ImportExampleAction_ImportError, Messages.ImportExampleAction_NoProjectSpecified);
		}
		if (success) {
			PlatformUI.getWorkbench().getIntroManager().closeIntro(intro);
		}
	}

}
