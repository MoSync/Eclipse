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
