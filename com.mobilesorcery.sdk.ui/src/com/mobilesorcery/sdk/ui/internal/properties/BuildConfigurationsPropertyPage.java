package com.mobilesorcery.sdk.ui.internal.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import com.mobilesorcery.sdk.core.BuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.BuildConfigurationsContentProvider;
import com.mobilesorcery.sdk.ui.BuildConfigurationsLabelProvider;

public class BuildConfigurationsPropertyPage extends PropertyPage {

	public class EditDialog extends Dialog {

		private IBuildConfiguration configuration;
		private Text name;
		protected EditDialog(Shell parentShell) {
			super(parentShell);			
		}

		public void setBuildConfiguration(IBuildConfiguration configuration) {
			this.configuration = configuration;
		}
		
		public Control createDialogArea(Composite parent) {
			getShell().setText("Edit Build Configuration");
			Composite main = (Composite) super.createDialogArea(parent);
			Label nameLabel = new Label(main, SWT.NONE);
			nameLabel.setText("Configuration &Name:");
			name = new Text(main, SWT.SINGLE | SWT.BORDER);
			name.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			name.setText(configuration.getId());
			name.selectAll();			
			return main;
		}
		
		public void okPressed() {
			IBuildConfiguration newConfiguration = configuration.clone(name.getText());
			boolean wasActive = getProject().getActiveBuildConfiguration() == configuration;
			getProject().installBuildConfiguration(newConfiguration);
			getProject().deinstallBuildConfiguration(configuration.getId());
			if (wasActive) {
				getProject().setActiveBuildConfiguration(newConfiguration.getId());
			}
			super.okPressed();
		}
	}

	abstract class DefaultListener implements Listener {
		protected MoSyncProject project;

		public DefaultListener(MoSyncProject project) {
			this.project = project;
		}

		public void handleEvent(Event event) {
			doHandleEvent(event);
			updateUI();
		}

		protected abstract void doHandleEvent(Event event);
	}
	
	class ActivateButtonListener extends DefaultListener {
		public ActivateButtonListener(MoSyncProject project) {
			super(project);
		}

		public void doHandleEvent(Event event) {
			if (!project.areBuildConfigurationsSupported() && project.getBuildConfigurations().isEmpty()) {
				project.activateBuildConfigurations();
			} else {
				project.setBuildConfigurationsSupported(!project.areBuildConfigurationsSupported());				
			}
		}
	}
	
	public class SetActiveListener extends DefaultListener implements IDoubleClickListener {
		public SetActiveListener(MoSyncProject project) {
			super(project);
		}

		public void doHandleEvent(Event event) {
			setActive();
		}

		public void doubleClick(DoubleClickEvent event) {
			setActive();
		}

		private void setActive() {
			if (getSelectedConfiguration() != null) {
				getProject().setActiveBuildConfiguration(getSelectedConfiguration());
			}
		}
	}
	
	public class AddListener extends DefaultListener {
		public AddListener(MoSyncProject project) {
			super(project);
		}

		public void doHandleEvent(Event event) {
			String uniqueId = BuildConfiguration.createUniqueId(getProject(), "Configuration");
			getProject().installBuildConfiguration(uniqueId);
		}

	}
	
	public class DuplicateListener extends AddListener {
		public DuplicateListener(MoSyncProject project) {
			super(project);
		}

		public void doHandleEvent(Event event) {
			String cfg = getSelectedConfiguration();
			if (cfg != null) {
				String uniqueId = BuildConfiguration.createUniqueId(getProject(), cfg);
				IBuildConfiguration newConfig = getProject().getBuildConfiguration(cfg).clone(uniqueId);
				getProject().installBuildConfiguration(newConfig);
			}
		}
	}
	
	public class EditListener extends DefaultListener {

		public EditListener(MoSyncProject project) {
			super(project);
		}

		protected void doHandleEvent(Event event) {
			Display d = event.display;
			Shell shell = new Shell(d);
			EditDialog dialog = new EditDialog(shell);
			dialog.setBuildConfiguration(project.getBuildConfiguration(getSelectedConfiguration()));
			dialog.open();
			shell.dispose();
		}
		
	}

	public class RemoveListener extends DefaultListener {
		public RemoveListener(MoSyncProject project) {
			super(project);
		}

		public void doHandleEvent(Event event) {
			if (getSelectedConfiguration() != null) {
				getProject().deinstallBuildConfiguration(getSelectedConfiguration());
			}
		}
	}


	private Button activateButton;
	private TableViewer viewer;
	private Button setActiveButton;
	private Button addButton;
	private Button duplicateButton;
	private Button deleteButton;
	private Button editButton;

	protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));

        activateButton = new Button(main, SWT.CHECK);
        activateButton.setText("Activate &Configurations");
        activateButton.addListener(SWT.Selection, new ActivateButtonListener(getProject()));
        activateButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        viewer = new TableViewer(main, SWT.BORDER);
        viewer.setContentProvider(new BuildConfigurationsContentProvider(getProject()));
        viewer.setLabelProvider(new BuildConfigurationsLabelProvider(getProject()));
        viewer.setInput(getProject());
        GridData viewerData = new GridData(GridData.FILL_BOTH);
        viewerData.verticalSpan = 5;
        viewer.getControl().setLayoutData(viewerData);
        viewer.addDoubleClickListener(new SetActiveListener(getProject()));
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateUI();
			}
		});
        
        setActiveButton = new Button(main, SWT.PUSH);
        setActiveButton.setText("&Set Active");
        setActiveButton.addListener(SWT.Selection, new SetActiveListener(getProject()));
        setActiveButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        
        addButton = new Button(main, SWT.PUSH);
        addButton.setText("&Add");
        addButton.addListener(SWT.Selection, new AddListener(getProject()));
        addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

        duplicateButton = new Button(main, SWT.PUSH);
        duplicateButton.setText("&Duplicate");
        duplicateButton.addListener(SWT.Selection, new DuplicateListener(getProject()));
        duplicateButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

        editButton = new Button(main, SWT.PUSH);
        editButton.setText("&Edit...");
        editButton.addListener(SWT.Selection, new EditListener(getProject()));
        editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        
        deleteButton = new Button(main, SWT.PUSH);
        deleteButton.setText("&Remove");
        deleteButton.addListener(SWT.Selection, new RemoveListener(getProject()));
        deleteButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        
        updateUI();
        return main;
	}
	
	protected void updateUI() {
		activateButton.setSelection(getProject().areBuildConfigurationsSupported());
		viewer.refresh();
		viewer.getControl().setEnabled(getProject().areBuildConfigurationsSupported());
		
		boolean enableActions = getProject().areBuildConfigurationsSupported();
		boolean hasSelection = !viewer.getSelection().isEmpty();
		setActiveButton.setEnabled(enableActions && hasSelection);
		addButton.setEnabled(enableActions);
		duplicateButton.setEnabled(enableActions && hasSelection);
		deleteButton.setEnabled(enableActions && hasSelection);
		editButton.setEnabled(enableActions && hasSelection);
		viewer.getControl().setFocus();
	}

	public String getSelectedConfiguration() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		return (String) selection.getFirstElement();
	}
	
    private MoSyncProject getProject() {
        IProject wrappedProject = (IProject) getElement();
        MoSyncProject project = MoSyncProject.create(wrappedProject);

        return project;
    }


}
