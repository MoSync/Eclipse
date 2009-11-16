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
package com.mobilesorcery.sdk.importproject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

public class WizardOldMobuildProjectsImportPage extends WizardPage implements PropertyChangeListener {

    private final class ProjectContentProvider implements ITreeContentProvider {
        public Object[] getChildren(Object parentElement) {
            return new Object[0];
        }

        public Object getParent(Object element) {
            return null;
        }

        public boolean hasChildren(Object element) {
            return false;
        }

        public Object[] getElements(Object inputElement) {
            return (Object[]) inputElement;
        }

        public void dispose() {
            // TODO Auto-generated method stub

        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // TODO Auto-generated method stub

        }
    }

    public class ProjectLabelProvider extends LabelProvider {
        public String getText(Object element) {
            if (element instanceof File) {
                return (Util.getNameWithoutExtension((File)element));
            }

            return super.getText(element);            
        }
    }

    private CheckboxTreeViewer projects;
    protected boolean scanUponFocusOut = true;
    private Button importResources;
    private Button donotCopyProject;

    protected WizardOldMobuildProjectsImportPage() {
        super("wizardoldproject", Messages.WizardOldMobuildProjectsImportPage_ImportExisting, Activator.getImageDescriptor(Activator.IMPORT_PAGE_IMAGE)); //$NON-NLS-1$
    }

    public void createControl(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = 0;
        main.setLayout(layout);

        setControl(main);

        Label directoryLabel = new Label(main, SWT.NONE);
        directoryLabel.setText(Messages.WizardOldMobuildProjectsImportPage_ScanDirectory);
        final Text directoryText = new Text(main, SWT.BORDER | SWT.SINGLE);
        directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        if (MoSyncTool.getDefault().isValid()) {
            directoryText.setText(MoSyncTool.getDefault().getMoSyncHome().append("projects").toOSString()); //$NON-NLS-1$
        }

        Button browse = new Button(main, SWT.PUSH);
        browse.setText(Messages.WizardOldMobuildProjectsImportPage_Browse);
        setButtonLayoutData(browse);

        browse.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                if (new File(directoryText.getText()).exists()) {
                    dialog.setFilterPath(directoryText.getText());
                }

                dialog.setMessage(Messages.WizardOldMobuildProjectsImportPage_SelectScanPaths);
                dialog.setText(Messages.WizardOldMobuildProjectsImportPage_SelectRootPath);

                String dir = dialog.open();
                if (dir != null) {
                    directoryText.setText(dir);
                    scanDirectoryToFindProjects(new File(dir.trim()));
                }
            }
        });

        directoryText.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event) {
                scanUponFocusOut = true;
            }
        });

        directoryText.addListener(SWT.FocusOut, new Listener() {
            public void handleEvent(Event event) {
                if (scanUponFocusOut) {
                    scanDirectoryToFindProjects(new File(directoryText.getText()));
                }
            }
        });

        Label projectsLabel = new Label(main, SWT.NONE);
        projectsLabel.setText(Messages.WizardOldMobuildProjectsImportPage_ImportProjects);
        projectsLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false, 3, 1));
        
        projects = new CheckboxTreeViewer(main, SWT.BORDER);
        projects.setLabelProvider(new ProjectLabelProvider());
        projects.setContentProvider(new ProjectContentProvider());

        GridData projectsData = new GridData(GridData.FILL_BOTH);
        projectsData.horizontalSpan = 2;
        projectsData.verticalSpan = 2;
        projects.getControl().setLayoutData(projectsData);
        
        Button selectAll = new Button(main, SWT.PUSH);
        selectAll.setText(Messages.WizardOldMobuildProjectsImportPage_SelectAll);
        setButtonLayoutData(selectAll);
        selectAll.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                projects.setAllChecked(true);
            }            
        });
        
        Button deselectAll = new Button(main, SWT.PUSH);        
        deselectAll.setText(Messages.WizardOldMobuildProjectsImportPage_DeselectAll);
        GridData deselectAllData = setButtonLayoutData(deselectAll);
        deselectAllData.verticalAlignment = SWT.BEGINNING;
        deselectAll.setLayoutData(deselectAllData);
        deselectAll.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                projects.setAllChecked(false);
            }
        });
        
        importResources = new Button(main, SWT.RADIO);
        importResources.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
        importResources.setText(Messages.WizardOldMobuildProjectsImportPage_CopyAllFiles);
        importResources.setSelection(true);
        
        Button importOnlyProjectResources = new Button(main, SWT.RADIO);
        importOnlyProjectResources.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
        importOnlyProjectResources.setText(Messages.WizardOldMobuildProjectsImportPage_CopyProjectFiles);
        importOnlyProjectResources.setSelection(false);
        
        donotCopyProject = new Button(main, SWT.RADIO);
        donotCopyProject.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
        donotCopyProject.setText(Messages.WizardOldMobuildProjectsImportPage_DoNotCopy);
        donotCopyProject.setSelection(false);
    }

    private void scanDirectoryToFindProjects(File root) {
        try {
            scanUponFocusOut = false;
            FindProjectsRunnable findProjects = new FindProjectsRunnable(root);
            findProjects.addFinishedListener(this);
            getContainer().run(true, true, findProjects);
        } catch (Exception e) {
            StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
    }

    public void propertyChange(final PropertyChangeEvent event) {
        if (FindProjectsRunnable.FINISHED == event.getPropertyName()) {
            projects.getControl().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    Object input = event.getNewValue();
                    if (input != null) {
                        projects.setInput(input);
                    }
                }
            });
        }

    }
    
    public File[] getProjectDescriptionFiles() {
        Object[] checked = projects.getCheckedElements();
        File[] result = new File[checked.length];
        for (int i = 0; i < checked.length; i++) {
            result[i] = (File) checked[i];
        }
        return result;
    }

    public int getCopyStrategy() {
        if (importResources.getSelection()) {
            return ImportProjectsRunnable.COPY_ALL_FILES;
        } else if (donotCopyProject.getSelection()) {
            return ImportProjectsRunnable.DO_NOT_COPY;
        } else {
            return ImportProjectsRunnable.COPY_ONLY_FILES_IN_PROJECT_DESC;
        }
    }
}
