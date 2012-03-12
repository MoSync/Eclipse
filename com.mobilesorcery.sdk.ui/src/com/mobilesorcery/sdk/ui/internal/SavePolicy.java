package com.mobilesorcery.sdk.ui.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.ui.actions.BuildAction;
import org.eclipse.ui.ide.IDE;

import com.mobilesorcery.sdk.core.ISavePolicy;

public class SavePolicy implements ISavePolicy {

	@Override
	public boolean isSaveAllSet() {
		return BuildAction.isSaveAllSet();
	}

	@Override
	public boolean saveAllEditors(IResource[] resources, boolean confirm) {
		return IDE.saveAllEditors(resources, confirm);
	}

}
