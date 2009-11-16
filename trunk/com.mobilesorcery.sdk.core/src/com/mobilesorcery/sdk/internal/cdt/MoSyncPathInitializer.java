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
package com.mobilesorcery.sdk.internal.cdt;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.PathEntryContainerInitializer;
import org.eclipse.cdt.internal.core.model.CProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class MoSyncPathInitializer extends PathEntryContainerInitializer {

    private static MoSyncPathInitializer instance;

	public MoSyncPathInitializer() {
    	instance = this;
    }

    public void initialize(IPath containerPath, ICProject project) throws CoreException {
        CoreModel.setPathEntryContainer(new ICProject[] { project }, new MoSyncIncludePathContainer(project.getProject()), null);
    }

	public static MoSyncPathInitializer getInstance() {
		return instance;
	}

}
