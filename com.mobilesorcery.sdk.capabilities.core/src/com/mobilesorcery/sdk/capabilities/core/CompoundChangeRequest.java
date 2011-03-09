package com.mobilesorcery.sdk.capabilities.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.mobilesorcery.sdk.core.MoSyncProject;

public class CompoundChangeRequest extends AbstractChangeRequest {

	private List<IChangeRequest> changeRequests;
	private HashSet<IChangeRequest> changeRequestsToApply;
	private String message;

	public CompoundChangeRequest(MoSyncProject project, List<IChangeRequest> changeRequests) {
			super(project);
		this.changeRequests = changeRequests;
		changeRequestsToApply = new HashSet<IChangeRequest>();
		changeRequestsToApply.addAll(changeRequests);
		for (IChangeRequest changeRequest : changeRequests) {
			setThisAsParent(changeRequest);
		}
	}
	
	private void setThisAsParent(IChangeRequest changeRequest) {
		if (changeRequest instanceof AbstractChangeRequest) {
			((AbstractChangeRequest) changeRequest).setParent(this);
			setShouldApply(changeRequest, ((AbstractChangeRequest) changeRequest).getPreferredApplyState());
		}
	}

	public CompoundChangeRequest(String message, MoSyncProject project, List<IChangeRequest> changeRequests) {
		this(project, changeRequests);
		setMessage(message);
	}

	public CompoundChangeRequest(String message, MoSyncProject project) {
		this(message, project, new ArrayList<IChangeRequest>());
	}
	
	public void addChangeRequest(IChangeRequest changeRequest) {
		changeRequests.add(changeRequest);
		setThisAsParent(changeRequest);
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
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
	
	public void setAllShouldApply(boolean shouldApply, boolean recursive) {
		for (IChangeRequest changeRequest : changeRequests) {
			setShouldApply(changeRequest, shouldApply);
			if (recursive && changeRequest instanceof CompoundChangeRequest) {
				((CompoundChangeRequest) changeRequest).setAllShouldApply(shouldApply, recursive);
			}
		}
	}
	
	public boolean shouldApply(IChangeRequest changeRequest) {
		return changeRequestsToApply.contains(changeRequest);
	}

	public IChangeRequest[] getChangeRequests() {
		return changeRequests.toArray(new IChangeRequest[0]);
	}

	public String toString() {
		return getMessage();
	}


}
