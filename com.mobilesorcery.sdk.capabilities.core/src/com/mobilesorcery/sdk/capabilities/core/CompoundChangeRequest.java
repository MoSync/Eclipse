package com.mobilesorcery.sdk.capabilities.core;

import java.util.HashSet;
import java.util.List;

import com.mobilesorcery.sdk.core.MoSyncProject;

public class CompoundChangeRequest extends AbstractChangeRequest {

	private List<IChangeRequest> changeRequests;
	private HashSet<IChangeRequest> changeRequestsToApply;

	public CompoundChangeRequest(MoSyncProject project, List<IChangeRequest> changeRequests) {
		super(project);
		this.changeRequests = changeRequests;
		changeRequestsToApply = new HashSet<IChangeRequest>();
		changeRequestsToApply.addAll(changeRequests);
		for (IChangeRequest changeRequest : changeRequests) {
			if (changeRequest instanceof AbstractChangeRequest) {
				((AbstractChangeRequest) changeRequest).setParent(this);
			}
		}
	}

	public void apply() {
		for (IChangeRequest changeRequest : changeRequests) {
			if (shouldApply(changeRequest)) {
				changeRequest.apply();
			}
		}
	}

	public void setShouldApply(IChangeRequest changeRequest, boolean shouldApply) {
		if (shouldApply) {
			changeRequestsToApply.add(changeRequest);
		} else {
			changeRequestsToApply.remove(changeRequest);
		}
	}
	
	public boolean shouldApply(IChangeRequest changeRequest) {
		return changeRequestsToApply.contains(changeRequest);
	}

	public IChangeRequest[] getChangeRequests() {
		return changeRequests.toArray(new IChangeRequest[0]);
	}


}
