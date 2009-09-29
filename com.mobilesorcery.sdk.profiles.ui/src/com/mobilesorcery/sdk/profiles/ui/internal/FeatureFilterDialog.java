package com.mobilesorcery.sdk.profiles.ui.internal;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.filter.FeatureFilter;
import com.mobilesorcery.sdk.ui.UIUtils;

public class FeatureFilterDialog extends DeviceFilterDialog<FeatureFilter> {

    private CheckboxTableViewer selectedFeature;
    private Button require;
    private Button disallow;

    public FeatureFilterDialog(Shell shell) {
        super(shell);
        setName(Messages.FeatureFilterDialog_FeatureAltBug);
        setFilter(new FeatureFilter());
    }

    public void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateUI();
    }
    
    public Control createDialogArea(Composite parent) {
        getShell().setText(getName());
        Composite contents = new Composite(parent, SWT.NONE);
        contents.setLayout(new GridLayout(1, false));
        
        require = new Button(contents, SWT.RADIO);
        require.setText(Messages.FeatureFilterDialog_Require);
        require.setSelection(filter.getStyle() == FeatureFilter.REQUIRE);
        
        disallow = new Button(contents, SWT.RADIO);
        disallow.setText(Messages.FeatureFilterDialog_Disallow);
        require.setSelection(filter.getStyle() == FeatureFilter.DISALLOW);
        
        selectedFeature = CheckboxTableViewer.newCheckList(contents, SWT.BORDER | SWT.SINGLE);
        selectedFeature.setContentProvider(new ArrayContentProvider());
        selectedFeature.setLabelProvider(new FeatureLabelProvider());
        selectedFeature.getControl().setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), UIUtils.getDefaultListHeight()));
        
        selectedFeature.setInput(MoSyncTool.getDefault().getAvailableFeatureDescriptions(MoSyncTool.EXCLUDE_CONSTANTS_FILTER));
        
        selectedFeature.setCheckedElements(filter.getFeatureIds());
        
        selectedFeature.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                updateUI();
            }         
        });
        
        GridData selectedFeatureData = new GridData();
        selectedFeatureData.grabExcessHorizontalSpace = true;
        selectedFeatureData.heightHint = 200;
        selectedFeature.getControl().setLayoutData(selectedFeatureData);
        
        return contents;
    }
    

    protected void updateUI() {
        getButton(IDialogConstants.OK_ID).setEnabled(selectedFeature.getCheckedElements().length != 0);
    }

    public void okPressed() {
        Object[] selection = selectedFeature.getCheckedElements();
        String[] castSelection = new String[selection.length];
        for (int i = 0; i < selection.length; i++) {
            castSelection[i] = (String)selection[i];
        }
        
        filter.setFeatureIds(castSelection);
        filter.setStyle(require.getSelection() ? FeatureFilter.REQUIRE : FeatureFilter.DISALLOW);
        super.okPressed();
    }
}
