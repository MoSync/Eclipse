package com.mobilesorcery.sdk.ui.internal.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncBuilder;

public class BuildPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public BuildPreferencePage() {
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CoreMoSyncPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		BooleanFieldEditor rebuild = new BooleanFieldEditor(MoSyncBuilder.REBUILD_ON_ERROR, "Always perform full &rebuild on errors", getFieldEditorParent());
		BooleanFieldEditor nativeVerbose = new BooleanFieldEditor(MoSyncBuilder.VERBOSE_BUILDS, "Run build tools with -verbose flag (not applicable to all tools)", getFieldEditorParent());
		addField(rebuild);
		addField(nativeVerbose);
	}


}
