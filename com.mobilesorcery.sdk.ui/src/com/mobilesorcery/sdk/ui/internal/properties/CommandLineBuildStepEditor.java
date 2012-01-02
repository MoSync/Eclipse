package com.mobilesorcery.sdk.ui.internal.properties;

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
import com.mobilesorcery.sdk.ui.ParameterResolverContentProvider;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public final class CommandLineBuildStepEditor extends BuildStepEditor implements IUpdatableControl {

	private Button runOnce;
	private Button runPerFile;
	private Text filePattern;
	private Text script;
	private Text name;
	private Button failOnError;

	public CommandLineBuildStepEditor(Shell shell) {
		// TODO: We may want more types of build steps, maybe a wizard-like thingy?
		super(shell);
	}

	private CommandLineBuildStep.Factory getFactory() {
		return (Factory) factory;
	}

	@Override
	public void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Edit Command Line Build Step");
	}

	@Override
	public Control createDialogArea(Composite parent) {
		Composite main = (Composite) super.createDialogArea(parent);
		main.setLayoutData(new GridData(GridData.FILL));
		main.setLayout(new GridLayout(2, false));
		Label nameLabel = new Label(main, SWT.NONE);
		nameLabel.setText("Build Step &Name:");
		name = new Text(main, SWT.BORDER | SWT.SINGLE);
		name.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 1, 1));
		runOnce = new Button(main, SWT.RADIO);
		runOnce.setText("Run script &once");
		runOnce.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
		runOnce.setSelection(!getFactory().shouldRunPerFile());
		runPerFile = new Button(main, SWT.RADIO);
		runPerFile.setText("Run script for each file that matches:");
		filePattern = new Text(main, SWT.BORDER | SWT.SINGLE);
		filePattern.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
		script = new Text(main, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData scriptData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		scriptData.heightHint = UIUtils.getDefaultListHeight();
		script.setLayoutData(scriptData);
		UpdateListener listener = new UpdateListener(this);
		runPerFile.addListener(SWT.Selection, listener);
		runOnce.addListener(SWT.Selection, listener);

		name.setText(factory.getName() == null ? "" : factory.getName());
		runPerFile.setSelection(getFactory().shouldRunPerFile());
		filePattern.setText(getFactory().getFilePattern() == null ? "" : getFactory().getFilePattern());
		script.setText(getFactory().getRawScript() == null ? "" : getFactory().getRawScript());

		failOnError = new Button(main, SWT.CHECK);
		failOnError.setText("&Fail on build error");
		failOnError.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
		failOnError.setSelection(getFactory().shouldFailOnError());

		ParameterResolver resolver = CommandLineBuildStep.createParameterResolver(
				MoSyncBuilder.createParameterResolver(project, null));
		ParameterResolverContentProvider.createProposalProvider(script, resolver);

		return main;
	}

	@Override
	public void updateUI() {
		filePattern.setEnabled(runPerFile.getSelection());
	}

	@Override
	public void okPressed() {
		getFactory().setFilePattern(filePattern.getText());
		getFactory().setRunPerFile(runPerFile.getSelection());
		getFactory().setRawScript(script.getText());
		getFactory().setName(name.getText());
		getFactory().setFailOnError(failOnError.getSelection());
		super.okPressed();
	}

}