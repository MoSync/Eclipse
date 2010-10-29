package com.mobilesorcery.sdk.capabilities.ui;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;

import com.mobilesorcery.sdk.capabilities.core.IChangeRequest;

public class CapabilitiesAnalyzerWizard extends Wizard {

	private ChangeRequestsPage changeRequestPage;
	private Map<IProject, IChangeRequest> changeRequests;

	public void addPages() {
		changeRequestPage = new ChangeRequestsPage(changeRequests);
		addPage(changeRequestPage);
	}
	
	public boolean performFinish() {
		for (IProject project : changeRequests.keySet()) {
			IChangeRequest changeRequest = changeRequests.get(project);
			changeRequest.apply();
		}
		
		return true;
	}

	public void setChangeRequests(Map<IProject, IChangeRequest> changeRequests) {
		this.changeRequests = changeRequests;
	}

}
