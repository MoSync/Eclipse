package com.mobilesorcery.sdk.testing.internal.ui.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.UIUtils;

public class CreateTestWizard extends Wizard implements INewWizard {

	private MoSyncProject initialProject;
	private TestCaseCreationPage testCaseCreationPage;
    private IWorkbench workbench;

	public boolean performFinish() {
		try {
			IFile testCaseFileToShow = testCaseCreationPage.configureProject();
			UIUtils.openResource(workbench, testCaseFileToShow);
            BasicNewFileResourceWizard.selectAndReveal(testCaseFileToShow, workbench.getActiveWorkbenchWindow());
			return true;
		} catch (CoreException e) {
			Policy.getStatusHandler().show(e.getStatus(), "Could not create test suite");
			CoreMoSyncPlugin.getDefault().log(e);
			return false;
		}
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	    this.workbench = workbench;
		this.initialProject = CoreMoSyncPlugin.getDefault().extractProject(selection);
	}

	protected MoSyncProject getInitialProject() {
		return initialProject;
	}
	
	public void addPages() {
		testCaseCreationPage = new TestCaseCreationPage(getInitialProject());
		addPage(testCaseCreationPage);
		//addPage(page);
	}
	
}
