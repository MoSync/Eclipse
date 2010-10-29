package com.mobilesorcery.sdk.capabilities.core.internal;

import java.util.ArrayList;

import com.mobilesorcery.sdk.capabilities.core.CompoundChangeRequest;
import com.mobilesorcery.sdk.capabilities.core.ICapabilities;
import com.mobilesorcery.sdk.capabilities.core.ICapabilitiesMatcher;
import com.mobilesorcery.sdk.capabilities.core.IChangeRequest;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class DefaultCapabilitiesMatcher implements ICapabilitiesMatcher {

	private ArrayList<ICapabilitiesMatcher> matchers = new ArrayList<ICapabilitiesMatcher>();
	
	public DefaultCapabilitiesMatcher() {

	}
	
	public IChangeRequest match(MoSyncProject project, ICapabilities requestedCapabilites) {
		ArrayList<IChangeRequest> changeRequests = new ArrayList<IChangeRequest>();
		
		for (ICapabilitiesMatcher matcher : matchers) {
			IChangeRequest changeRequest = matcher.match(project, requestedCapabilites);
			if (changeRequest != null) {
				changeRequests.add(changeRequest);
			}
		}
		
		return changeRequests.isEmpty() ? null : new CompoundChangeRequest(project, changeRequests);
	}

	public void add(ICapabilitiesMatcher matcher) {
		matchers.add(matcher);
	}

}
