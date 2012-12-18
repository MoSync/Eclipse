package com.mobilesorcery.sdk.ui.internal.properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.build.CommandLineBuildStep;
import com.mobilesorcery.sdk.core.build.CommandLineBuildStep.Factory;
import com.mobilesorcery.sdk.core.build.CopyBuildResultBuildStep;
import com.mobilesorcery.sdk.ui.BuildConfigurationsContentProvider;
import com.mobilesorcery.sdk.ui.BuildConfigurationsLabelProvider;
import com.mobilesorcery.sdk.ui.BuildStepEditor;
import com.mobilesorcery.sdk.ui.ParameterResolverContentProvider;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class CopyBuildStepEditor extends BuildStepEditor implements IUpdatableControl {

	private Button suppressCopying;
	private ComboViewer cfgs;
	private Button allButton;
	private Button oneButton;

	private CopyBuildResultBuildStep.Factory getFactory() {
		return (CopyBuildResultBuildStep.Factory) factory;
	}

	@Override
	public void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Edit Copy Build Result Build Step");
	}

	@Override
	public Control createDialogArea(Composite parent) {
		UpdateListener listener = new UpdateListener(this);
		Composite main = (Composite) super.createDialogArea(parent);
		main.setLayoutData(new GridData(GridData.FILL));
		main.setLayout(new GridLayout(2, false));
		suppressCopying = new Button(main, SWT.CHECK);
		suppressCopying.setText("&Do not copy to release package folder");
		suppressCopying.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false, 2, 1));
		allButton = new Button(main, SWT.RADIO);
		allButton.setText("&Build All Configurations");
		allButton.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false, 2, 1));
		oneButton = new Button(main, SWT.RADIO);
		oneButton.setText("&Build Only this Configuration:");
		BuildConfigurationsContentProvider cp = new BuildConfigurationsContentProvider(project);
		cfgs = new ComboViewer(main, SWT.READ_ONLY);
		cfgs.setContentProvider(cp);
		cfgs.setLabelProvider(new BuildConfigurationsLabelProvider(project));
		cfgs.setInput(project);
		cfgs.getControl().addListener(SWT.Selection, listener);

		suppressCopying.addListener(SWT.Selection, listener);
		allButton.addListener(SWT.Selection, listener);
		oneButton.addListener(SWT.Selection, listener);

		suppressCopying.setSelection(!getFactory().isActive());
		String cfgId = getFactory().getConfigurationId();
		if (cfgId != null) {
			cfgs.setSelection(new StructuredSelection(cfgId));
		}
		allButton.setSelection(cfgId == null);
		oneButton.setSelection(cfgId != null);
		return main;
	}

	@Override
	public void updateUI() {
		boolean enableCfgs = !suppressCopying.getSelection();
		cfgs.getControl().setEnabled(enableCfgs && oneButton.getSelection());
		allButton.setEnabled(enableCfgs);
		oneButton.setEnabled(enableCfgs);
		boolean isValid = !oneButton.getSelection() || getCfgId() != null;
		getButton(Dialog.OK).setEnabled(isValid);
	}

	private String getCfgId() {
		String cfgId = (String) ((IStructuredSelection) cfgs.getSelection()).getFirstElement();
		return cfgId;
	}
	@Override
	public void okPressed() {
		boolean useOne = oneButton.getSelection();
		getFactory().setConfigurationId(useOne ? getCfgId() : null);
		getFactory().setActive(!suppressCopying.getSelection());
		super.okPressed();
	}
}
