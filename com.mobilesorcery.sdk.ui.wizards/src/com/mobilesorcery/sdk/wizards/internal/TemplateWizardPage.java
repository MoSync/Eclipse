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
package com.mobilesorcery.sdk.wizards.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.templates.ProjectTemplate;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.wizards.Activator;

public class TemplateWizardPage extends WizardPage {

    private final class ProjectTemplateLabelProvider extends CellLabelProvider {
		public String getText(Object o) {
		    if (o instanceof ProjectTemplate) {
		        return ((ProjectTemplate)o).getName();
		    }
		    return "?";
		}

		public String getToolTipText(Object element) {
			if (element instanceof ProjectTemplate) {
				String desc = ((ProjectTemplate) element).getDescription();
				if (desc != null) {
					return desc;
				}
			}
			
			return "";
		}

		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			cell.setText(getText(element));
		}
	}

	private final class InnerSelectionListener implements SelectionListener {
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }

        public void widgetSelected(SelectionEvent e) {
        	if (!templateTable.getControl().isDisposed()) {
        		descriptionText.setVisible(useTemplate.getSelection());
        		templateTable.getControl().setEnabled(useTemplate.getSelection());
        		setPageComplete(!useTemplate.getSelection() || !templateTable.getSelection().isEmpty());
        	}
        }
    }

    protected TemplateWizardPage() {
        super("SelectTemplate");
        setTitle("MoSync Project Template");
        setDescription("Select a project template");
        setImageDescriptor(ImageDescriptor.createFromFile(this.getClass(), "/icons/new.png"));
    }

    private Button useTemplate;
    private TableViewer templateTable;
	private Text descriptionText;

    public ProjectTemplate getProjectTemplate() {
        IStructuredSelection selection = (IStructuredSelection) templateTable.getSelection();
        return useTemplate.getSelection() ? (ProjectTemplate) selection.getFirstElement() : null;
    }

    public void createControl(Composite parent) {
        Composite control = new Composite(parent, SWT.NONE);
        control.setLayout(new GridLayout(1, false));
        
        useTemplate = new Button(control, SWT.CHECK);
        useTemplate.setText("Use template");
        useTemplate.setSelection(true);
        setPageComplete(false);
        
        templateTable = new TableViewer(control);
        templateTable.setLabelProvider(new ProjectTemplateLabelProvider());
        
        templateTable.setContentProvider(new ArrayContentProvider());
        templateTable.setInput(Activator.getDefault().getProjectTemplates(null).toArray());
        
        templateTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        
        SelectionListener listener = new InnerSelectionListener();
        useTemplate.addSelectionListener(listener);
        templateTable.getTable().addSelectionListener(listener);
        templateTable.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                if (getWizard().canFinish()) {        // Kind of ugly...            
                    if (getWizard().performFinish()) {
                        getWizard().getContainer().getShell().dispose();
                    }
                }
            }            
        });

        ColumnViewerToolTipSupport.enableFor(templateTable, SWT.NONE);
        
        descriptionText = new Text(control, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        GridData descriptionTextData = new GridData(GridData.FILL_HORIZONTAL);
        descriptionTextData.heightHint = UIUtils.getRowHeight(3);
        descriptionText.setLayoutData(descriptionTextData);
        updateDescriptionText(null);
        
        templateTable.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				ProjectTemplate template = (ProjectTemplate) selection.getFirstElement();
				updateDescriptionText(template);
			}
		});        

        setControl(control);
    }
    
    private void updateDescriptionText(ProjectTemplate template) {
    	String description = "";
    	
    	if (template != null && template.getDescription() != null) {
    		description = template.getDescription();    	
    	}
    	
    	descriptionText.setText(description);
    }

}
