package com.mobilesorcery.sdk.core.build;

import org.eclipse.ui.IMemento;

public abstract class AbstractBuildStepFactory implements IBuildStepFactory {

	@Override
	public void load(IMemento memento) {
		// TODO Auto-generated method stub

	}

	@Override
	public void store(IMemento memento) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean requiresPrivilegedAccess() {
		return false;
	}

	@Override
	public boolean isDefault() {
		return true;
	}
	
	public String toString() {
		return getId();
	}
}
