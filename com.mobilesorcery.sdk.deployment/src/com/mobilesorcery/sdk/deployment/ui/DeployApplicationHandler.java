package com.mobilesorcery.sdk.deployment.ui;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.deployment.internal.ui.ftp.FTPDeploymentWizard;
import com.mobilesorcery.sdk.ui.MoSyncCommandHandler;


public class DeployApplicationHandler extends MoSyncCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IResource resource = extractFirstResource(selection);
		IProject project = resource == null ? null : resource.getProject();
		if (project != null) {
			MoSyncProject mosyncProject = MoSyncProject.create(project);
			if (mosyncProject != null) {
				FTPDeploymentWizard wizard = new FTPDeploymentWizard();
				wizard.setProject(mosyncProject);
				if (resource.getType() == IResource.FILE) {
				    wizard.setDeploymentFile(resource.getLocation().toFile());
				}
				Shell shell = HandlerUtil.getActiveShell(event);
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.open();
			}
			
		}
		return null;
	}
}
