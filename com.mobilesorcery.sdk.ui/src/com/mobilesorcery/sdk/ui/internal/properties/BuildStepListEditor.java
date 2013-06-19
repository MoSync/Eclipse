package com.mobilesorcery.sdk.ui.internal.properties;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.build.CommandLineBuildStep;
import com.mobilesorcery.sdk.core.build.CopyBuildResultBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.ui.IBuildStepEditor;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
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
		IBuildStepEditor editor = MosyncUIPlugin.getDefault().createBuildStepEditor(project, (IBuildStepFactory) selection);
		if (editor != null) {
			boolean doAdd = IBuildStepEditor.OK == editor.edit();
			return doAdd;
		}

		return false;
	}

	@Override
	protected boolean canEdit(Object element) {
		IBuildStepFactory factory = (IBuildStepFactory) element;
		IBuildStepEditor editor = MosyncUIPlugin.getDefault().createBuildStepEditor(project, factory);
		if (editor != null && editor.canEdit()) {
			return true;
		}
		return CopyBuildResultBuildStep.ID.equals(factory.getId()) || isRearrangable(element);
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