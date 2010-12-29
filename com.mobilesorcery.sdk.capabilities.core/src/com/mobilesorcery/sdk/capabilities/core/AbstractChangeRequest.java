package com.mobilesorcery.sdk.capabilities.core;

import com.mobilesorcery.sdk.core.MoSyncProject;

public abstract class AbstractChangeRequest implements IChangeRequest {

	private MoSyncProject project;
	private CompoundChangeRequest parent;
	private boolean preferredApplyState = true;

	public AbstractChangeRequest(MoSyncProject project) {
		this.project = project;
		
	}

	public MoSyncProject getProject() {
		return project;
	}
	
	public void setParent(CompoundChangeRequest parent) {
		this.parent = parent;
	}
	
	public CompoundChangeRequest getParent() {
		return parent;
	}

	public void setPreferredApplyState(boolean preferredApplyState) {
		this.preferredApplyState = preferredApplyState;
	}
	
	public boolean getPreferredApplyState() {
		return preferredApplyState;
	}

}
