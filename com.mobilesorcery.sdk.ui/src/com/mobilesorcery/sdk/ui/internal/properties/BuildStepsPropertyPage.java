package com.mobilesorcery.sdk.ui.internal.properties;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.DefaultParameterResolver;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProjectParameterResolver;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.PrivilegedAccess;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.CommandLineBuildStep;
import com.mobilesorcery.sdk.core.build.CommandLineBuildStep.Factory;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.ParameterResolverContentProvider;
import com.mobilesorcery.sdk.ui.SimpleListEditor;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class BuildStepsPropertyPage extends MoSyncPropertyPage {

	private final class CommandLineBuildStepEditor extends Dialog implements IUpdatableControl {

		private Factory factory;
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
			runOnce.setSelection(!factory.shouldRunPerFile());
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
			runPerFile.setSelection(factory.shouldRunPerFile());
			filePattern.setText(factory.getFilePattern() == null ? "" : factory.getFilePattern());
			script.setText(factory.getScript() == null ? "" : factory.getScript());

			failOnError = new Button(main, SWT.CHECK);
			failOnError.setText("&Fail on build error");
			failOnError.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
			failOnError.setSelection(factory.shouldFailOnError());

			ParameterResolver resolver = CommandLineBuildStep.createParameterResolver(
					MoSyncBuilder.createParameterResolver(getProject(), null));
			ParameterResolverContentProvider.createProposalProvider(script, resolver);

			return main;
		}

		public void setBuildStepFactory(CommandLineBuildStep.Factory factory) {
			this.factory = factory;
		}

		@Override
		public void updateUI() {
			filePattern.setEnabled(runPerFile.getSelection());
		}

		@Override
		public void okPressed() {
			factory.setFilePattern(filePattern.getText());
			factory.setRunPerFile(runPerFile.getSelection());
			factory.setScript(script.getText());
			factory.setName(name.getText());
			factory.setFailOnError(failOnError.getSelection());
			super.okPressed();
		}

	}

	private final class BuildStepListEditor extends
			SimpleListEditor<IBuildStepFactory> {
		private BuildStepListEditor(Composite parent, int style) {
			super(parent, style);
			setEditAfterAdd(true);
			getList().setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((IBuildStepFactory) element).getName();
				}
			});
		}

		@Override
		protected IBuildStepFactory createObject() {
			return new CommandLineBuildStep.Factory();
		}

		@Override
		protected boolean edit(Object selection, boolean add) {
			CommandLineBuildStepEditor editor = new CommandLineBuildStepEditor(getParent().getShell());
			if (selection instanceof CommandLineBuildStep.Factory) {
				editor.setBuildStepFactory((Factory) selection);
				boolean doAdd = CommandLineBuildStepEditor.OK == editor.open();
				if (doAdd) {

				}
				return doAdd;
			}

			return false;
		}

		@Override
		protected boolean canEdit(Object element) {
			return isEditable(element);
		}

		@Override
		protected boolean canRemove(Object element) {
			return isEditable(element);
		}

		@Override
		protected boolean canMoveUp(Object element) {
			return isEditable(element);
		}

		@Override
		protected boolean canMoveDown(Object element) {
			return isEditable(element);
		}

		private boolean isEditable(Object element) {
			return CommandLineBuildStep.ID.equals(((IBuildStepFactory) element).getId());
		}
	}

	private BuildStepListEditor steps;

	@Override
	protected Control createContents(Composite parent) {
		steps = new BuildStepListEditor(parent, SimpleListEditor.REARRANGEABLE);
		BuildSequence sequence = new BuildSequence(getProject());
		steps.setInput(sequence.getBuildStepFactories());
		return steps;
	}

	@Override
	public boolean performOk() {
		BuildSequence newSequence = new BuildSequence(getProject());
		try {
			newSequence.apply(steps.getEditedInput());
			boolean requiresPrivilegedAccess = MoSyncBuilder.requiresPrivilegedAccess(newSequence);
			// We always auto-grant access for the user
			PrivilegedAccess.getInstance().grantAccess(getProject(), requiresPrivilegedAccess);
		} catch (IOException e) {
			Policy.getStatusHandler().show(new Status(IStatus.ERROR, MosyncUIPlugin.PLUGIN_ID, e.getMessage(), e), "Could not save build step info");
			return false;
		}
		return super.performOk();
	}

	@Override
	public void performDefaults() {
		BuildSequence defaultSequence = new BuildSequence(getProject());
		defaultSequence.setToDefault();
		steps.setInput(defaultSequence.getBuildStepFactories());
	}

}
