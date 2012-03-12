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

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.templates.IProjectTemplateExtension;
import com.mobilesorcery.sdk.core.templates.ProjectTemplate;
import com.mobilesorcery.sdk.core.templates.TemplateManager;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.wizards.Activator;

public class TemplateWizardPage extends WizardPage {

	private final class TemplateCategoryLabelProvider extends LabelProvider {

		private final HashMap<String, Image> cachedImages = new HashMap<String, Image>();

		@Override
		public Image getImage(Object element) {
			if (element instanceof IProjectTemplateExtension) {
				String name = ((IProjectTemplateExtension) element).getName();
				Image image = cachedImages.get(name);
				if (!cachedImages.containsKey(name)) {
					ImageDescriptor imageDesc = ((IProjectTemplateExtension) element).getImage();
					image = imageDesc == null ? null : imageDesc.createImage();
					image = MosyncUIPlugin.resize(image, 32, 32, true, true);
					cachedImages.put(name, image);
				}
				return image;
			}
			return null;
		}

		@Override
		public void dispose() {
			for (Image cachedImage : cachedImages.values()) {
				if (cachedImage != null) {
					cachedImage.dispose();
				}
			}
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IProjectTemplateExtension) {
				return ((IProjectTemplateExtension) element).getName();
			}
			return "";
		}

	}
    private final class ProjectTemplateLabelProvider extends CellLabelProvider {
		public String getText(Object o) {
		    if (o instanceof ProjectTemplate) {
		        return ((ProjectTemplate)o).getName();
		    }
		    return "?";
		}

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof ProjectTemplate) {
				String desc = ((ProjectTemplate) element).getDescription();
				if (desc != null) {
					return desc;
				}
			}

			return "";
		}

		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			cell.setText(getText(element));
		}
	}

	private final class InnerSelectionListener implements SelectionListener {
        @Override
		public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }

        @Override
		public void widgetSelected(SelectionEvent e) {
        	if (!templateTable.getControl().isDisposed()) {
        		//descriptionText.setVisible(useTemplate.getSelection());
        		//templateTable.getControl().setEnabled(useTemplate.getSelection());
        		updatePageComplete();
        	}
        }
    }

    protected TemplateWizardPage() {
        super("SelectTemplate");
        setTitle("MoSync Project Template");
        setDescription("Select a project template");
        setImageDescriptor(ImageDescriptor.createFromFile(this.getClass(), "/icons/wizardimg.png"));
    }

    //private Button useTemplate;
    private TableViewer templateTable;
	private Text descriptionText;
	private TableViewer categoryTable;

    public ProjectTemplate getProjectTemplate() {
        IStructuredSelection selection = (IStructuredSelection) templateTable.getSelection();
        return (ProjectTemplate) selection.getFirstElement();
        //return useTemplate.getSelection() ? (ProjectTemplate) selection.getFirstElement() : null;
    }

    @Override
	public void createControl(Composite parent) {
        Composite control = new Composite(parent, SWT.NONE);
        control.setLayout(new GridLayout(2, false));

        setPageComplete(false);

        categoryTable = new TableViewer(control);
        categoryTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (templateTable.getTable().getItemCount() > 0) {
					templateTable.getTable().select(0);
				}
				updateUI();
			}
		});

        TableColumn column1 = new TableColumn(categoryTable.getTable(), SWT.NONE);
        column1.setWidth(UIUtils.getDefaultFieldSize());

        categoryTable.getControl().setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.FILL, false, true));
        categoryTable.setLabelProvider(new TemplateCategoryLabelProvider());
        categoryTable.setContentProvider(new ArrayContentProvider());
        categoryTable.setInput(getTemplateExtensions());

        templateTable = new TableViewer(control);
        templateTable.setLabelProvider(new ProjectTemplateLabelProvider());

        templateTable.setContentProvider(new ArrayContentProvider());

        templateTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

        SelectionListener listener = new InnerSelectionListener();

        templateTable.getTable().addSelectionListener(listener);
        templateTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
			public void doubleClick(DoubleClickEvent event) {
                if (getWizard().canFinish()) {        // Kind of ugly...
                    if (getWizard().performFinish()) {
                        getWizard().getContainer().getShell().dispose();
                    }
                }
            }
        });

        ColumnViewerToolTipSupport.enableFor(templateTable, SWT.NONE);

        descriptionText = new Text(control, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
        GridData descriptionTextData = new GridData(GridData.FILL_HORIZONTAL);
        descriptionTextData.horizontalSpan = 2;
        descriptionTextData.heightHint = UIUtils.getRowHeight(3);
        descriptionText.setLayoutData(descriptionTextData);
        updateDescriptionText(null);

/*        useTemplate = new Button(control, SWT.CHECK);
        useTemplate.setText("Use template");
        useTemplate.setSelection(true);
        useTemplate.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false, 2, 1));
        useTemplate.addSelectionListener(listener);*/

        templateTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				ProjectTemplate template = (ProjectTemplate) selection.getFirstElement();
				updateDescriptionText(template);
			}
		});

        setControl(control);
        updateUI();
    }

    protected void updateUI() {
    	IStructuredSelection selection = (IStructuredSelection) categoryTable.getSelection();
		IProjectTemplateExtension selected = (IProjectTemplateExtension) selection.getFirstElement();
		if (selected != null) {
			templateTable.setInput(getTemplates(selected.getType()));
		}
		updatePageComplete();
	}

	private void updatePageComplete() {
		setPageComplete(/*!useTemplate.getSelection() || */!templateTable.getSelection().isEmpty());
	}

	private ProjectTemplate[] getTemplates(String type) {
		return TemplateManager.getDefault().getProjectTemplates(type).toArray(new ProjectTemplate[0]);
	}

	private IProjectTemplateExtension[] getTemplateExtensions() {
		ArrayList<IProjectTemplateExtension> result = new ArrayList<IProjectTemplateExtension>();
		for (String type : TemplateManager.getDefault().getTemplateTypes()) {
	    	IProjectTemplateExtension ext = TemplateManager.getDefault().getExtensionForType(type);
	    	if (ext != null) {
	    		result.add(ext);
	    	}
		}
		return result.toArray(new IProjectTemplateExtension[0]);
    }

	private void updateDescriptionText(ProjectTemplate template) {
    	String description = "";

    	if (template != null && template.getDescription() != null) {
    		description = template.getDescription();
    	}

    	descriptionText.setText(description);
    }

}
