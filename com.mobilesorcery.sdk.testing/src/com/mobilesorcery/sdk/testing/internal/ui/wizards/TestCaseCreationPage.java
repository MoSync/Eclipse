package com.mobilesorcery.sdk.testing.internal.ui.wizards;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.builder.MoSyncBuilderVisitor;
import com.mobilesorcery.sdk.testing.TestPlugin;
import com.mobilesorcery.sdk.testing.project.MoSyncProjectTestManager;

public class TestCaseCreationPage extends WizardPage {

	class PageListener implements Listener {
		public void handleEvent(Event event) {
			if (event.type == SWT.Selection && event.widget == projectButton) {
				handleProjectButtonSelected();
			}
			updateUI();
		}
	}

	private MoSyncProject initialProject;
	private Text sourceFolder;
	private Button projectButton;
	private PageListener listener;
	private Text testCaseName;

	protected TestCaseCreationPage(MoSyncProject initialProject) {
		super("ProjectSelection");
		setTitle("Select project to configure for testing");
		this.initialProject = initialProject;
	}

	public void validate() {
		String message = null;
		int severity = IMessageProvider.INFORMATION;
		
		IResource sourceFolderFile = getSourceFolderFile();
		if (sourceFolderFile.exists()) {
			if (!Util.isEmptyDirectory(sourceFolderFile.getLocation().toFile())) {
				message = MessageFormat.format("Directory {0} is not empty - this may cause build problems.", sourceFolderFile.getFullPath()); 
				severity = IMessageProvider.WARNING;
			}
		}
		
		String testCaseName = this.testCaseName.getText();
		if (Util.isEmpty(testCaseName)) {
			message = "Test case name cannot be empty";
			severity = IMessageProvider.ERROR;
		} else {
			IPath testCasePath = sourceFolderFile.getLocation().append(testCaseName);
			if (testCasePath.segmentCount() > 1) {
				IFile testCaseResource = ResourcesPlugin.getWorkspace().getRoot().getFile(testCasePath);
				if (MoSyncBuilderVisitor.getCFile(testCaseResource, false) == null) {
					message = "Test case name must be a c or c++ file";
					severity = IMessageProvider.ERROR;
				}
			}
		}
		
		if (getTestCaseFile().exists()) {
			message = "Test case file already exists";
			severity = IMessageProvider.ERROR;
		}
		
		setMessage(message, severity);
		setPageComplete(severity != IMessageProvider.ERROR);
	}

	public void createControl(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout(3, false));
		listener = new PageListener();
		createSourceFolderEditor(control);
		createTestCaseEditor(control);

		updateUI();
		setControl(control);
	}

	protected void createSourceFolderEditor(Composite parent) {
		Label sourceFolderLabel = new Label(parent, SWT.NONE);
		sourceFolderLabel.setText("&Source Folder");
		sourceFolder = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		sourceFolder.setLayoutData(gd);
		sourceFolder.setText(createInitialTestFolder());
		sourceFolder.addListener(SWT.Modify, listener);
		projectButton = new Button(parent, SWT.PUSH);
		projectButton.setText("Browse...");
		projectButton.addListener(SWT.Selection, listener);
	}

	private String createInitialTestFolder() {
		if (initialProject != null) {
			return new Path(initialProject.getWrappedProject().getName(), "/test").toPortableString();
		}
		return "";
	}

	private void createTestCaseEditor(Composite parent) {
		Label testCaseNameLabel = new Label(parent, SWT.NONE);
		testCaseNameLabel.setText("Test File &Name");
		
		testCaseName = new Text(parent, SWT.SINGLE | SWT.BORDER);
		testCaseName.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		testCaseName.addListener(SWT.Modify, listener);
	}

	protected void handleProjectButtonSelected() {
		IContainer container = selectContainer();
		if (container == null) {
			return;
		}

		String containerName = container.getFullPath().toPortableString();
		sourceFolder.setText(containerName);
		updateUI();
	}

	private void updateUI() {
		validate();
	}

	private IContainer selectContainer() {
		// MoSyncProjectSelectionDialog dialog = new
		// MoSyncProjectSelectionDialog(getShell());
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), initialProject.getWrappedProject(), true,
				"Select location of test case source file");
		// dialog.setInitialProject(initialProject.getWrappedProject());
		// return dialog.selectProject();
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result != null && result.length > 0) {
				IPath path = (IPath) result[0];
				return (IContainer) ResourcesPlugin.getWorkspace().getRoot().findMember(path, false);
			}
		}

		return null;
	}
	
	
	public IFile configureProject() throws CoreException {
		try {
    		IResource sourceFolderFile = getSourceFolderFile();
    		// TODO: Not finished.
    		IProject project = sourceFolderFile.getProject();
    		MoSyncProjectTestManager tm = new MoSyncProjectTestManager(MoSyncProject.create(project));
    		tm.assignTestResource(sourceFolderFile.getProjectRelativePath(), true);
    		tm.configureProject();
    		
    		return createTestFile();
		} catch (Exception e) {
			if (e instanceof CoreException) {
				throw (CoreException) e;
			}
			throw new CoreException(new Status(IStatus.ERROR, TestPlugin.PLUGIN_ID, e.getMessage(), e));
		}
	}
	
	private IFile createTestFile() throws IOException, CoreException {
		// TODO: Templates? Several suites per project? One small step at a time.
		InputStream is = null;
		try {
			IFile testCaseFile = getTestCaseFile();
			IResource testCaseFolder = getSourceFolderFile();
			if (!testCaseFolder.exists()) {
				IFolder testCaseFolderFolder = (IFolder) testCaseFolder;
				testCaseFolderFolder.create(true, true, null);
			}
			
			URL template = TestPlugin.getDefault().getBundle().getResource("/test_template.c");
			is = template.openStream();
			testCaseFile.create(is, true, null);
			
			return testCaseFile;
		} finally {
			Util.safeClose(is);
		}
	}

	private IResource getSourceFolderFile() {
		Path sourceFolderPath = new Path(sourceFolder.getText());
		if (sourceFolderPath.segmentCount() == 1) {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(sourceFolderPath.lastSegment());
		}
		
		IFolder sourceFolderFile = ResourcesPlugin.getWorkspace().getRoot().getFolder(sourceFolderPath);
		return sourceFolderFile;
	}
	
	private IFile getTestCaseFile() {
		IPath testCasePath = getSourceFolderFile().getFullPath().append(testCaseName.getText());
		IFile testCaseFile = ResourcesPlugin.getWorkspace().getRoot().getFile(testCasePath);
		return testCaseFile;
	}


}
