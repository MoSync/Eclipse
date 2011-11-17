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
package com.mobilesorcery.sdk.profiles.ui;

import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;
import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.ICompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.ConstantFilter;
import com.mobilesorcery.sdk.profiles.filter.FeatureFilter;
import com.mobilesorcery.sdk.profiles.filter.ProfileFilter;
import com.mobilesorcery.sdk.profiles.ui.internal.ConstantFilterDialog;
import com.mobilesorcery.sdk.profiles.ui.internal.DeviceFilterDialog;
import com.mobilesorcery.sdk.profiles.ui.internal.FeatureFilterDialog;
import com.mobilesorcery.sdk.profiles.ui.internal.Messages;
import com.mobilesorcery.sdk.profiles.ui.internal.ProfileFilterDialog;
import com.mobilesorcery.sdk.profiles.ui.internal.SelectFilterTypeDialog;

public class DeviceFilterComposite extends Composite {

    private ICompositeDeviceFilter filter;
    private final TableViewer filterTable;
    private final Button remove;
    private final Button edit;
    private final Button add;

    PropertyChangeSupport listeners = new PropertyChangeSupport(this);
    private final Label deviceCountLabel;
	private MoSyncProject currentProject;

    public DeviceFilterComposite(Composite parent, int style) {
        super(parent, style);

        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        setLayout(layout);

        Composite buttonRow = new Composite(this, SWT.NONE);
        buttonRow.setLayout(new GridLayout(3, true));
        add = new Button(buttonRow, SWT.PUSH);
        add.setText(Messages.DeviceFilterComposite_Add);
        constrainSize(add, 85, SWT.DEFAULT);

        remove = new Button(buttonRow, SWT.PUSH);
        remove.setText(Messages.DeviceFilterComposite_Remove);
        constrainSize(remove, 85, SWT.DEFAULT);

        edit = new Button(buttonRow, SWT.PUSH);
        edit.setText(Messages.DeviceFilterComposite_Edit);
        constrainSize(edit, 85, SWT.DEFAULT);

        add.addSelectionListener(new SelectionAdapter() {
            @Override
			public void widgetSelected(SelectionEvent e) {
                SelectFilterTypeDialog dialog = new SelectFilterTypeDialog(getShell());
                int result = dialog.open();
                if (result == IDialogConstants.OK_ID && dialog.getFilter() != null) {
                    filter.addFilter(dialog.getFilter());
                    updateUI(true);
                }
            }
        });

        edit.addSelectionListener(new SelectionAdapter() {
            @Override
			public void widgetSelected(SelectionEvent e) {
                editSelectedFilter();
            }
        });

        filterTable = new TableViewer(this);
        filterTable.setContentProvider(new ArrayContentProvider());
        filterTable.setLabelProvider(new LabelProvider());
        filterTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
			public void selectionChanged(SelectionChangedEvent event) {
                updateUI(false);
            }
        });

        GridData filterTableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        filterTable.getControl().setLayoutData(filterTableData);
        filterTable.getControl().addKeyListener(new KeyListener() {
            @Override
			public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.DEL) {
                    removeSelectedFilters();
                }
            }

            @Override
			public void keyReleased(KeyEvent e) {
            }
        });

        remove.addSelectionListener(new SelectionAdapter() {
            @Override
			public void widgetSelected(SelectionEvent e) {
                removeSelectedFilters();
            }
        });

        deviceCountLabel = new Label(this, SWT.TRAIL);
        deviceCountLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        updateUI(true);
    }

    protected void editSelectedFilter() {
        IStructuredSelection selection = (IStructuredSelection) filterTable.getSelection();
        Object element = selection.getFirstElement();
        if (element instanceof IDeviceFilter) {
            IDeviceFilter filter = (IDeviceFilter) element;
            DeviceFilterDialog dialog = createDialogForFilter(filter);
            if (DeviceFilterDialog.OK == dialog.open()) {
                this.filter.update(filter);
                updateUI(true);
                updateDeviceCount();
            }
        }
    }

    private DeviceFilterDialog createDialogForFilter(IDeviceFilter filter) {
        // TODO! Hard-coded?
        DeviceFilterDialog dialog = null;
        if (filter instanceof FeatureFilter) {
            dialog = new FeatureFilterDialog(getShell());
        } else if (filter instanceof ConstantFilter) {
            dialog = new ConstantFilterDialog(getShell());
        } else if (filter instanceof ProfileFilter) {
            dialog = new ProfileFilterDialog(getShell());
        } else {
            return null;
        }

        dialog.setFilter(filter);

        return dialog;
    }

    protected boolean isEditable(IDeviceFilter filter) {
		return filter instanceof FeatureFilter ||
		       filter instanceof ConstantFilter ||
		       filter instanceof ProfileFilter;
	}

    protected void removeSelectedFilters() {
        IStructuredSelection selection = (IStructuredSelection) filterTable.getSelection();
        for (Iterator selected = selection.iterator(); selected.hasNext(); ) {
            filter.removeFilter((IDeviceFilter)selected.next());
            updateUI(true);
        }
    }

    /**
     * Will only set the filter if it is an <code>ICompositeDeviceFilter</code>,
     * will ignore otherwise.
     * @param currentProject
     */
    public void setCurrentProject(MoSyncProject currentProject) {
    	this.filter = currentProject.getDeviceFilter();
    	this.currentProject = currentProject;
        updateUI(true);
    }

    private void updateUI(final boolean updateCount) {
        filterTable.getControl().getDisplay().asyncExec(new Runnable() {
            @Override
			public void run() {
                boolean noFilters = filter == null || filter.getFilters().length == 0;
                filterTable.setInput(noFilters ?
                        new String[] { Messages.DeviceFilterComposite_NoFilter } :
                        filter.getFilters());

                remove.setEnabled(!noFilters);

                IStructuredSelection selection = (IStructuredSelection) filterTable.getSelection();
                edit.setEnabled(selection.size() == 1 &&
                		selection.getFirstElement() instanceof IDeviceFilter &&
                		isEditable((IDeviceFilter) selection.getFirstElement()) &&
                		!noFilters);
                if (updateCount) {
                    updateDeviceCount();
                }
            }
        });
    }

	private void updateDeviceCount() {
        deviceCountLabel.setText(getDeviceCountText());
    }

    protected String getDeviceCountText() {
        int selected = currentProject.getProfileManager().getProfiles(filter).length;
        int total = currentProject.getProfileManager().getProfiles().length;
        return MessageFormat.format("{0} of {1} included", selected, total);
    }

    @Override
	public void setEnabled(boolean enabled) {
        filterTable.getControl().setEnabled(enabled);
        add.setEnabled(enabled);
        remove.setEnabled(enabled);
        edit.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    /**
     * <p>
     * Utility method for setting the layout data of a control to have a certain
     * size.
     * </p>
     * <p>
     * <emph>Works only with GridLayouts</emph>
     * </p>
     *
     * @param control
     *      the control
     * @param hSize
     *      the horizontal size in pixels, or SWT.DEFAULT if unspecified
     * @param vSize
     *      the vertical size in pixels, or SWT.DEFAULT if unspecified
     */
    // TODO: Move somewhere else, and rewrite (because it's also in project X)
    protected void constrainSize(Control control, int hSize, int vSize) {
        if (control.getParent().getLayout() instanceof GridLayout) {
            GridData data = (GridData) control.getLayoutData();
            if (data == null) {
                data = new GridData();
            }

            if (vSize != SWT.DEFAULT) {
                data.heightHint = vSize;
            }

            if (hSize != SWT.DEFAULT) {
                data.widthHint = hSize;
            }

            control.setLayoutData(data);
        }
    }

}
