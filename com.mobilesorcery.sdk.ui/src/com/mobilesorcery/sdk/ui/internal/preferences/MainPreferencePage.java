package com.mobilesorcery.sdk.ui.internal.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class MainPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private Button useEnv;
    private DirectoryFieldEditor mosyncHomeDir;

    public MainPreferencePage() {
        super("MoSync", CoreMoSyncPlugin.getImageDescriptor("/icons/mosyncproject.png"), GRID);
        
        IPreferenceStore store = CoreMoSyncPlugin.getDefault().getPreferenceStore();
        setPreferenceStore(store);
    }

    protected void createFieldEditors() {    	
        useEnv = new Button(getFieldEditorParent(), SWT.CHECK);
        useEnv.setText("Use system &environment variable to locate MoSync (MOSYNCDIR)");
        useEnv.setSelection(getPreferenceStore().getBoolean(MoSyncTool.MO_SYNC_HOME_FROM_ENV_PREF));
        useEnv.setLayoutData(new GridData(SWT.TOP, SWT.DEFAULT, true, false, 3, 1));

        mosyncHomeDir = new DirectoryFieldEditor(MoSyncTool.MOSYNC_HOME_PREF, "MoSync &Home Directory", getFieldEditorParent());
        mosyncHomeDir.setEmptyStringAllowed(true);
        
        addField(mosyncHomeDir);
        
        useEnv.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateUI();
            }

        });

        updateUI();
    }

    private void updateUI() {
        mosyncHomeDir.setEnabled(!useEnv.getSelection(), getFieldEditorParent());        
        if (useEnv.getSelection()) {
            mosyncHomeDir.setStringValue(MoSyncTool.getMoSyncHomeFromEnv().toOSString());
        }
    }

    public boolean performOk() {
        apply();
        return super.performOk();
    }
    
    public void performApply() {
        apply();
        super.performApply();
    }
    
    private void apply() {
        getPreferenceStore().setValue(MoSyncTool.MO_SYNC_HOME_FROM_ENV_PREF, useEnv.getSelection());
    }
        
    public void init(IWorkbench workbench) {
    }

}
