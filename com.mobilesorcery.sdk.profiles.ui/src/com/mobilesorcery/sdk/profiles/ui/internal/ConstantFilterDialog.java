/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.profiles.ui.internal;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.filter.ConstantFilter;
import com.mobilesorcery.sdk.profiles.filter.ConstantFilter.RelationalOp;

public class ConstantFilterDialog extends DeviceFilterDialog<ConstantFilter> {

    private final class Listener implements SelectionListener, ModifyListener {
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }

        public void widgetSelected(SelectionEvent event) {
            if (event.widget.getData() instanceof RelationalOp) {
                selectedOp = (RelationalOp) event.widget.getData();
            }
            
            updateUI();
        }

        public void modifyText(ModifyEvent event) {
            if (event.widget == thresholdText) {
                updateThresholdFromText();
            }
            
            updateUI();
        }
    }

    private static final RelationalOp[] OPS = new RelationalOp[] { ConstantFilter.GT, ConstantFilter.LT, ConstantFilter.EQ, ConstantFilter.NEQ };

    private RelationalOp selectedOp;

    private TableViewer constants;

    private Long threshold;

    private Text thresholdText;
    
    public ConstantFilterDialog(Shell shell) {
        super(shell);
        setName(Messages.ConstantFilterDialog_ConstantCriterion);
        setFilter(new ConstantFilter());
    }

    public void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateUI();
    }
    
    private void updateThresholdFromText() {
        try {
            threshold = new Long(Long.parseLong(thresholdText.getText().trim()));
        } catch (NumberFormatException e) {
            threshold = null;
        }
    }
    
    private void updateUI() {
        getButton(IDialogConstants.OK_ID).setEnabled(!constants.getSelection().isEmpty() && selectedOp != null && threshold != null); 
    }
    
    public Control createDialogArea(Composite parent) {
        getShell().setText(getName());
        Listener listener = new Listener();
        
        Composite contents = new Composite(parent, SWT.NONE);        
        contents.setLayout(new GridLayout(2, true));
        
        constants = new TableViewer(contents, SWT.BORDER | SWT.SINGLE);
        constants.setContentProvider(new ArrayContentProvider());
        constants.setLabelProvider(new FeatureLabelProvider());
        constants.setInput(MoSyncTool.getDefault().getAvailableFeatureDescriptions(MoSyncTool.INCLUDE_CONSTANTS_FILTER));
        String initConstantFeature = filter.getConstantFeature();
        if (initConstantFeature != null) {
            constants.setSelection(new StructuredSelection(initConstantFeature));
        }

        constants.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        Composite relationalOps = new Composite(contents, SWT.BORDER);
        relationalOps.setLayout(new GridLayout(1, false));
        
        Button[] opButtons = new Button[OPS.length];
        selectedOp = filter.getRelationalOp();
        for (int i = 0; i < OPS.length; i++) {
            opButtons[i] = new Button(relationalOps, SWT.RADIO);
            opButtons[i].setData(OPS[i]);
            opButtons[i].setText(OPS[i].getDescription());
            opButtons[i].setSelection(selectedOp == OPS[i]);
            opButtons[i].addSelectionListener(listener);
        }
        
        Label valueLabel = new Label(relationalOps, SWT.NONE);
        valueLabel.setText(Messages.ConstantFilterDialog_Threshold);
        thresholdText = new Text(relationalOps, SWT.BORDER | SWT.SINGLE);
        thresholdText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
        threshold = filter.getThreshold();
        thresholdText.setText(Long.toString(filter.getThreshold()));
        thresholdText.addModifyListener(listener);
        
        return contents;
    }
    
    public void okPressed() {
        filter.setRelationalOp(selectedOp);
        String selectedFeature = (String) ((IStructuredSelection)constants.getSelection()).getFirstElement();
        filter.setConstantFeature(selectedFeature);
        filter.setThreshold(threshold);
        super.okPressed();
    }
}
