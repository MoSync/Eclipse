package com.mobilesorcery.sdk.testing.internal.ui.wizards;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Set;

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
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.builder.MoSyncBuilderVisitor;
import com.mobilesorcery.sdk.testing.TestPlugin;
import com.mobilesorcery.sdk.testing.project.MoSyncProjectTestManager;
import com.mobilesorcery.sdk.ui.BuildConfigurationsContentProvider;
import com.mobilesorcery.sdk.ui.BuildConfigurationsLabelProvider;

public class TestCaseCreationPage extends WizardPage {

	class PageListener implements Listener {
		public void handleEvent(Event event) {
			if (event.type == SWT.Selection && event.widget == projectButton) {
				handleProjectButtonSelected();
			}
			updateUI();
		}
	}

    private static final String TEST_SETUP_FILE_NAME = "__ide_setup.cpp";

	private MoSyncProject initialProject;
	private Text sourceFolder;
	private Button projectButton;
	private PageListener listener;
	private Text testCaseName;
    private Group buildConfigs;
    private ComboViewer debugBuildCfgs;
    private ComboViewer releaseBuildCfgs;
    private MoSyncProject previousProject;
    private Button createSetupFile;

    private boolean hasSetupFile;

	protected TestCaseCreationPage(MoSyncProject initialProject) {
		super("ProjectSelection");
		setTitle("Select project to configure for testing");
		this.initialProject = initialProject;
	}

	public void validate() {
		String message = null;
		int severity = IMessageProvider.INFORMATION;

        MoSyncProject projectToConfigure = getProjectToConfigure();
        
		IResource sourceFolderFile = getSourceFolderFile();
		
		if (sourceFolderFile == null || !sourceFolderFile.getProject().exists()) {
		    message = "Please specify a valid source folder (project/path)";
		    severity = IMessageProvider.ERROR;
		} else if (sourceFolderFile.exists()) {
		    if (projectToConfigure == null) {
		        message = "The source folder must be located in a MoSync enabled project";
		        severity = IMessageProvider.ERROR;
		    } else {
		        boolean isTestFolder = new MoSyncProjectTestManager(projectToConfigure).isTestResource(sourceFolderFile);
		        if (!isTestFolder && !Util.isEmptyDirectory(sourceFolderFile.getLocation().toFile())) {
		            message = MessageFormat.format("Directory {0} is not empty - this may cause build problems.", sourceFolderFile.getFullPath()); 
		            severity = IMessageProvider.WARNING;
		        }
			} 
		}
		
		String testCaseName = this.testCaseName.getText();
		if (message == null && Util.isEmpty(testCaseName)) {
			message = "Test case name cannot be empty";
			severity = IMessageProvider.ERROR;
		} else if (message == null) {
			IPath testCasePath = sourceFolderFile.getLocation().append(testCaseName);
			if (testCasePath.segmentCount() > 1) {
				IFile testCaseResource = ResourcesPlugin.getWorkspace().getRoot().getFile(testCasePath);
				if (!MoSyncBuilderVisitor.hasExtension(testCaseResource, MoSyncBuilderVisitor.CPP_SOURCE_FILE_EXTS)) {
					message = "Test case name must be a c++ file";
					severity = IMessageProvider.ERROR;
				}
			}
		}
		
		if (getTestCaseFile() != null && getTestCaseFile().exists()) {
			message = "Test case file already exists";
			severity = IMessageProvider.ERROR;
		}
		
		setMessage(message, severity);
		setPageComplete(severity != IMessageProvider.ERROR);
	}

	private MoSyncProject getProjectToConfigure() {
	    IResource sourceFolderFile = getSourceFolderFile();
	    if (sourceFolderFile == null) {
	        return null;
	    }
	    
        return MoSyncProject.create(sourceFolderFile.getProject());
    }

    public void createControl(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout(3, false));
		listener = new PageListener();
		createSourceFolderEditor(control);
		createTestCaseEditor(control);
		createBuildConfigsGroup(control);

		initUI();
		setControl(control);
	}

	private void createBuildConfigsGroup(Composite control) {
	    buildConfigs = new Group(control, SWT.NONE);
	    buildConfigs.setText("Build &Configurations");
	    buildConfigs.setLayout(new GridLayout(2, false));
	    buildConfigs.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
	    Label buildConfigsInfo = new Label(buildConfigs, SWT.WRAP);
	    buildConfigsInfo.setText("Select configurations to base test configurations on:\n" +
	    		"(Include paths, libraries, etc from the selected configurations will be used " +
	    		"by the test configuration.)");
	    buildConfigsInfo.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
	    

        Label releaseBuildCfgsLabel = new Label(buildConfigs, SWT.NONE);
        releaseBuildCfgsLabel.setText("Test:");
        releaseBuildCfgs = new ComboViewer(buildConfigs, SWT.READ_ONLY);
        releaseBuildCfgs.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Label debugBuildCfgsLabel = new Label(buildConfigs, SWT.NONE);
        debugBuildCfgsLabel.setText("Test/Debug:");
	    debugBuildCfgs = new ComboViewer(buildConfigs, SWT.READ_ONLY);
        debugBuildCfgs.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    
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
        
        createSetupFile = new Button(parent, SWT.CHECK);
        createSetupFile.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        createSetupFile.setText("Integrate test runner with IDE (will generate a setup file)");
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

	private void initUI() {
        hasSetupFile = hasSetupFile(initialProject);
	    updateUI();
	}
	
	private boolean hasSetupFile(MoSyncProject project) {
	    if (project == null) {
	        return false;
	    }
	    MoSyncProjectTestManager mgr = new MoSyncProjectTestManager(project);
	    Set<IPath> testResources = mgr.getTestResources();
	    for (IPath testResource : testResources) {
	        IFile setupFile = project.getWrappedProject().getFile(testResource.append(TEST_SETUP_FILE_NAME));
	        if (setupFile.exists()) {
	            return true;
	        }
	    }
	    
        return false;
    }

    private void updateUI() {
		validate();
		initialProject = getSourceFolderFile() == null ? null : MoSyncProject.create(getSourceFolderFile().getProject());
		
		boolean showDebugCfgs = false;
		boolean showReleaseCfgs = false;
        
		if (previousProject != initialProject) {
		    hasSetupFile = hasSetupFile(initialProject);
		    previousProject = initialProject;
		    if (initialProject != null) {
    		    Set<String> debugCfgs = initialProject.getBuildConfigurationsOfType(IBuildConfiguration.DEBUG_TYPE);
    		    Set<String> releaseCfgs = initialProject.getBuildConfigurationsOfType(IBuildConfiguration.RELEASE_TYPE);
    		    Set<String> testCfgs = initialProject.getBuildConfigurationsOfType(TestPlugin.TEST_BUILD_CONFIGURATION_TYPE);
    	        showDebugCfgs = testCfgs.isEmpty() && debugCfgs.size() > 1;
    	        showReleaseCfgs = testCfgs.isEmpty() && releaseCfgs.size() > 1;
    	        
    	        debugBuildCfgs.setContentProvider(new BuildConfigurationsContentProvider(initialProject, IBuildConfiguration.DEBUG_TYPE));
    	        releaseBuildCfgs.setContentProvider(new BuildConfigurationsContentProvider(initialProject, IBuildConfiguration.RELEASE_TYPE));
    
                debugBuildCfgs.setLabelProvider(new BuildConfigurationsLabelProvider(initialProject));
                releaseBuildCfgs.setLabelProvider(new BuildConfigurationsLabelProvider(initialProject));
                
    	        debugBuildCfgs.setInput(initialProject);
    	        releaseBuildCfgs.setInput(initialProject);
    	        
    	        if (debugCfgs.size() > 0) {
    	            debugBuildCfgs.setSelection(new StructuredSelection(debugCfgs.toArray()[0]));
    	        }
    	        if (releaseCfgs.size() > 0) {
    	            releaseBuildCfgs.setSelection(new StructuredSelection(releaseCfgs.toArray()[0]));
    	        }
		    }
		    
		}

        createSetupFile.setSelection(!hasSetupFile);
		createSetupFile.setVisible(!hasSetupFile);
        buildConfigs.setVisible(showDebugCfgs || showReleaseCfgs);
        debugBuildCfgs.getCombo().setEnabled(showDebugCfgs);
        releaseBuildCfgs.getCombo().setEnabled(showReleaseCfgs);
	}

	private IContainer selectContainer() {
		// MoSyncProjectSelectionDialog dialog = new
		// MoSyncProjectSelectionDialog(getShell());
	    IContainer dialogRoot = initialProject == null ? ResourcesPlugin.getWorkspace().getRoot() : initialProject.getWrappedProject();
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), dialogRoot, true,
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
    		IBuildConfiguration debugPrototype = getPrototypeConfiguration(true);
    		IBuildConfiguration releasePrototype = getPrototypeConfiguration(false);
            tm.setPrototypeConfiguration(debugPrototype, true);
            tm.setPrototypeConfiguration(releasePrototype, false);
            tm.assignTestResource(sourceFolderFile.getProjectRelativePath(), true);
            tm.configureProject();
    		
            if (createSetupFile.getSelection() && !hasSetupFile) {
                createSetupFile();
            }
    		return createMainTestFile();
		} catch (Exception e) {
			if (e instanceof CoreException) {
				throw (CoreException) e;
			}
			throw new CoreException(new Status(IStatus.ERROR, TestPlugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

    private IBuildConfiguration getPrototypeConfiguration(boolean isDebug) {
        ComboViewer source = isDebug ? debugBuildCfgs : releaseBuildCfgs;
        IStructuredSelection selection = (IStructuredSelection) source.getSelection();
        if (!selection.isEmpty()) {
            String cfgId = (String) selection.getFirstElement();
            return initialProject.getBuildConfiguration(cfgId);
        }
        
        return null;
    }

    private void createSetupFile() throws IOException, CoreException {
        IFile testSetupFile = getTestSetupFile();
        createTestFile(testSetupFile, "/test_setup_template.c");
    }
    
    private IFile createMainTestFile() throws IOException, CoreException {
        IFile testCaseFile = getTestCaseFile();
        return createTestFile(testCaseFile, "/test_template.c");
    }
    
    private IFile createTestFile(IFile testCaseFile, String templateResource) throws IOException, CoreException {
		// TODO: Templates? Several suites per project? One small step at a time.
		InputStream is = null;
		try {
			if (testCaseFile == null) {
			    throw new CoreException(new Status(IStatus.ERROR, TestPlugin.PLUGIN_ID, "No test case file provided"));
			}
			IResource testCaseFolder = getSourceFolderFile();
			if (!testCaseFolder.exists()) {
				IFolder testCaseFolderFolder = (IFolder) testCaseFolder;
				testCaseFolderFolder.create(true, true, null);
			}
			
			URL template = TestPlugin.getDefault().getBundle().getResource(templateResource);
			is = template.openStream();
			testCaseFile.create(is, true, null);
			
			return testCaseFile;
		} finally {
			Util.safeClose(is);
		}
	}

	private IResource getSourceFolderFile() {
		Path sourceFolderPath = new Path(sourceFolder.getText());
		
		if (sourceFolderPath.segmentCount() == 0) {
		    return null;
		}
		
		if (sourceFolderPath.segmentCount() == 1) {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(sourceFolderPath.lastSegment());
		}
		
		IFolder sourceFolderFile = ResourcesPlugin.getWorkspace().getRoot().getFolder(sourceFolderPath);
		return sourceFolderFile;
	}

    private IFile getTestSetupFile() {
        return getTestFile(TEST_SETUP_FILE_NAME);
    }
    
    private IFile getTestCaseFile() {
        return getTestFile(testCaseName.getText());
	}

    private IFile getTestFile(String name) {
        if (getSourceFolderFile() == null) {
            return null;
        }
        IPath testCasePath = getSourceFolderFile().getFullPath().append(name);
        IFile testCaseFile = ResourcesPlugin.getWorkspace().getRoot().getFile(testCasePath);
        return testCaseFile;    
    }

}
