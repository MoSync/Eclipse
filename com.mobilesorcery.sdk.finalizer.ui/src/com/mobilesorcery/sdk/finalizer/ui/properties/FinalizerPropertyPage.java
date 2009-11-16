package com.mobilesorcery.sdk.finalizer.ui.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.finalizer.core.FinalizerParser;
import com.mobilesorcery.sdk.ui.BuildConfigurationsContentProvider;
import com.mobilesorcery.sdk.ui.BuildConfigurationsLabelProvider;

public class FinalizerPropertyPage extends PropertyPage implements
		IWorkbenchPropertyPage, PropertyChangeListener {

	private Group configurationGroup;
	private Button changeConfiguration;
	private ComboViewer configurations;

	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		createConfigurationEditor(main);

		initUI();
		getProject().addPropertyChangeListener(this);    	
		
		return main;			
	}

	private void initUI() {
		boolean enabled = getProject().isBuildConfigurationsSupported();
		
		configurations.setContentProvider(new BuildConfigurationsContentProvider(getProject()));
		configurations.setLabelProvider(new BuildConfigurationsLabelProvider(getProject()));
		configurations.setInput(getProject());
		changeConfiguration.setSelection(enabled && PropertyUtil.getBoolean(getProject(), FinalizerParser.AUTO_CHANGE_CONFIG));
		String buildConfigurationId = getProject().getProperty(FinalizerParser.BUILD_CONFIG);
		if (buildConfigurationId != null) {
			configurations.setSelection(new StructuredSelection(buildConfigurationId));
		}
		configurationGroup.layout();
		
		configurationGroup.setEnabled(enabled);
		changeConfiguration.setEnabled(enabled);
		configurations.getControl().setEnabled(enabled);
	}

	private void createConfigurationEditor(Composite control) {
		configurationGroup = new Group(control, SWT.NONE);
		configurationGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		configurationGroup.setText("&Configuration");
		configurationGroup.setLayout(new GridLayout(1, false));

		changeConfiguration = new Button(configurationGroup, SWT.CHECK);
		changeConfiguration
				.setText("&Automatically switch to this configuration before launching");

		configurations = new ComboViewer(configurationGroup, SWT.READ_ONLY | SWT.BORDER);
	}

	public boolean performOk() {
		PropertyUtil.setBoolean(getProject(), FinalizerParser.AUTO_CHANGE_CONFIG, changeConfiguration.getSelection());
		String buildConfigId = (String) ((IStructuredSelection) configurations.getSelection()).getFirstElement();
		getProject().setProperty(FinalizerParser.BUILD_CONFIG, buildConfigId);
		return true;
	}
	
    private MoSyncProject getProject() {
        IProject wrappedProject = (IProject) getElement();
        MoSyncProject project = MoSyncProject.create(wrappedProject);

        return project;
    }

	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getPropertyName();
		if (MoSyncProject.BUILD_CONFIGURATION_SUPPORT_CHANGED.equals(property) ||
			MoSyncProject.BUILD_CONFIGURATION_CHANGED.equals(property)) {
			initUI();
		}
	}

	public void dispose() {
		getProject().removePropertyChangeListener(this);
		super.dispose();
	}

}
