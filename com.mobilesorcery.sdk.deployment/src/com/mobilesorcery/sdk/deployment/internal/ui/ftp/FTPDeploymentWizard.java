package com.mobilesorcery.sdk.deployment.internal.ui.ftp;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.wizard.Wizard;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.deployment.DeploymentPlugin;
import com.mobilesorcery.sdk.deployment.IDeploymentStrategy;
import com.mobilesorcery.sdk.deployment.ProjectDeploymentStrategy;

public class FTPDeploymentWizard extends Wizard {

	private FTPDeploymentParamsPage deploymentPage;
	private MoSyncProject project;
	private AddToDeployFilePage addToDeployFilePage;
	private AssignProfilesPage assignProfilesPage;
    private File deploymentFile;
    private ProjectDeploymentStrategy pds;
    private IDeploymentStrategy strategyToEdit;

	public FTPDeploymentWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	public void setProject(MoSyncProject project) {
		this.project = project;
	}
	
	public void setDeploymentFile(File deploymentFile) {
	    if (project == null) {
	        throw new IllegalStateException();
	    }
	    
	    pds = new ProjectDeploymentStrategy(project, deploymentFile);
	    
	    // TODO: Only for now, don't do like this later.
	    List<IDeploymentStrategy> strategies = pds.getStrategies();
	    if (!strategies.isEmpty()) {
	        setStrategyToEdit(strategies.get(0));
	    }
	}
	
	public void setStrategyToEdit(IDeploymentStrategy strategyToEdit) {
	    this.strategyToEdit = strategyToEdit;
	}
	
	public void addPages() {
		deploymentPage = new FTPDeploymentParamsPage();
		deploymentPage.setStrategyToEdit(strategyToEdit);
		assignProfilesPage = new AssignProfilesPage("assign", "Assign profiles to deploy");
		assignProfilesPage.setDeviceFilter(pds == null ? null : pds.getAssignedProfiles(strategyToEdit));
		addToDeployFilePage = new AddToDeployFilePage();
		addPage(deploymentPage);
		addPage(assignProfilesPage);
		addPage(addToDeployFilePage);
	}
	
	public boolean performFinish() {
		IRunnableWithProgress deploymentRunnable = new DeploymentRunnable(project, deploymentPage.getStrategy(), assignProfilesPage.getDeviceFilter(), addToDeployFilePage.getDeployFile());
		try {
			getContainer().run(true, true, deploymentRunnable);
		} catch (Exception e) {
			Policy.getStatusHandler().show(new Status(IStatus.ERROR, DeploymentPlugin.PLUGIN_ID, e.getMessage()), "Could not deploy application");
			return false;
		}
		
		return true;
	}

}
