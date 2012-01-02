package com.mobilesorcery.sdk.ui.internal.properties;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.build.CommandLineBuildStep;
import com.mobilesorcery.sdk.core.build.CopyBuildResultBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.ui.SimpleListEditor;

final class BuildStepListEditor extends
		SimpleListEditor<IBuildStepFactory> {
	private MoSyncProject project;

	BuildStepListEditor(Composite parent, int style) {
		super(parent, style);
		setEditAfterAdd(true);
		getList().setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IBuildStepFactory) element).getName();
			}
		});
	}

	public void setProject(MoSyncProject project) {
		this.project = project;
	}

	@Override
	protected IBuildStepFactory createObject() {
		return new CommandLineBuildStep.Factory();
	}

	@Override
	protected boolean edit(Object selection, boolean add) {
		BuildStepEditor editor = getEditor((IBuildStepFactory) selection);
		if (editor != null) {
			boolean doAdd = CommandLineBuildStepEditor.OK == editor.open();
			return doAdd;
		}

		return false;
	}

	private BuildStepEditor getEditor(IBuildStepFactory factory) {
		Shell shell = getParent().getShell();
		BuildStepEditor editor = null;
		if (factory instanceof CommandLineBuildStep.Factory) {
			editor = new CommandLineBuildStepEditor(shell);
		} else if (factory instanceof CopyBuildResultBuildStep.Factory) {
			editor = new CopyBuildStepEditor(shell);
		}
		if (editor != null) {
			editor.setBuildStepFactory(factory);
			editor.setProject(project);
		}
		return editor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return CopyBuildResultBuildStep.ID.equals(((IBuildStepFactory) element).getId()) || isRearrangable(element);
	}

	@Override
	protected boolean canRemove(Object element) {
		return isRearrangable(element);
	}

	@Override
	protected boolean canMoveUp(Object element) {
		return isRearrangable(element);
	}

	@Override
	protected boolean canMoveDown(Object element) {
		return isRearrangable(element);
	}

	private boolean isRearrangable(Object element) {
		return CommandLineBuildStep.ID.equals(((IBuildStepFactory) element).getId());
	}

}