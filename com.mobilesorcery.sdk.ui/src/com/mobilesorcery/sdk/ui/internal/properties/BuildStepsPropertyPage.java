package com.mobilesorcery.sdk.ui.internal.properties;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.Policy;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.PrivilegedAccess;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.SimpleListEditor;

public class BuildStepsPropertyPage extends MoSyncPropertyPage {

	public BuildStepsPropertyPage() {
		super(false);
	}

	private BuildStepListEditor steps;

	@Override
	protected Control createContents(Composite parent) {
		steps = new BuildStepListEditor(parent, SimpleListEditor.REARRANGEABLE);
		steps.setProject(getProject());
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
