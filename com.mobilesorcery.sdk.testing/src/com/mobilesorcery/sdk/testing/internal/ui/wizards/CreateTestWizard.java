package com.mobilesorcery.sdk.testing.internal.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.util.StatusHandler;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class CreateTestWizard extends Wizard implements INewWizard {

	private MoSyncProject initialProject;
	private TestCaseCreationPage testCaseCreationPage;

	public boolean performFinish() {
		try {
			testCaseCreationPage.configureProject();
			return true;
		} catch (CoreException e) {
			Policy.getStatusHandler().show(e.getStatus(), "Could not create test suite");
			return false;
		}
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
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
