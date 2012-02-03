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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IUpdater;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.ui.DeviceFilterComposite;
import com.mobilesorcery.sdk.profiles.ui.DeviceViewerFilter;
import com.mobilesorcery.sdk.profiles.ui.internal.actions.FinalizeForProfileAction;
import com.mobilesorcery.sdk.profiles.ui.internal.actions.SetTargetProfileAction;
import com.mobilesorcery.sdk.profiles.ui.internal.actions.ShowProfileInfoAction;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.ProfileContentProvider;
import com.mobilesorcery.sdk.ui.ProfileLabelProvider;

public class ProfilesView extends ViewPart implements PropertyChangeListener {

	class InternalSelectionProvider implements ISelectionProvider {
		// The class is a fix for bug #917 -- if useful for other plugins please refactor
		ListenerList listeners = new ListenerList();
		private ISelection selection = new StructuredSelection();

		@Override
		public void addSelectionChangedListener(
				ISelectionChangedListener listener) {
			listeners.add(listener);
		}

		@Override
		public ISelection getSelection() {
			return selection;
		}

		@Override
		public void removeSelectionChangedListener(
				ISelectionChangedListener listener) {
			listeners.remove(listener);
		}

		@Override
		public void setSelection(ISelection selection) {
			this.selection = selection;
			for (Object listener : listeners.getListeners()) {
				SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
				((ISelectionChangedListener) listener).selectionChanged(event);
			}
		}

	}

    private final SetTargetProfileAction setTargetAction = new SetTargetProfileAction();
    private final ShowProfileInfoAction showProfileInfo = new ShowProfileInfoAction();
    private final FinalizeForProfileAction buildForProfile = new FinalizeForProfileAction();

    private TreeViewer profileTree;
    private ProfileLabelProvider profileLabelProvider;
    private DeviceFilterComposite deviceFilter;
    private Label projectLabel;
    private StackLayout stackLayout;
    private Composite profilePane;
    private Composite main;
    private Composite errorPane;
    private Link errorMessage;
	private MoSyncProject currentProject;
    private Link currentTarget;
	private InternalSelectionProvider selectionProvider;

    public ProfilesView() {
    }

    @Override
	public void setFocus() {

    }

    @Override
	public void init(IViewSite site) throws PartInitException {
        super.init(site);
        MoSyncTool.getDefault().addPropertyChangeListener(this);
    }

    @Override
	public void dispose() {
    	MosyncUIPlugin.getDefault().removeListener(this);
    	profileLabelProvider.dispose();
        MoSyncTool.getDefault().removePropertyChangeListener(this);
    }

    @Override
	public void createPartControl(Composite parent) {
        main = new Composite(parent, SWT.NONE);
        stackLayout = new StackLayout();
        main.setLayout(stackLayout);

        createProfilesControl(main);
        createErrorPane(main);

        updateVisiblePane();
    }

    private void updateVisiblePane() {
        String errorMessage = MoSyncTool.getDefault().validate();
        updateErrorMessage();
        stackLayout.topControl = errorMessage == null ? profilePane : errorPane;
        main.layout();
    }

    private void createErrorPane(Composite parent) {
        errorPane = new Composite(parent, SWT.NONE);
        errorPane.setLayout(new GridLayout(1, false));
        errorMessage = new Link(errorPane, SWT.WRAP);
        errorMessage.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, true, true));
        errorMessage.addSelectionListener(new SelectionAdapter() {
            @Override
			public void widgetSelected(SelectionEvent event) {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(ProfilesView.this.getSite().getShell(), "com.mobilesorcery.sdk.preferences.main", new String[] { "com.mobilesorcery.sdk.preferences.main" }, null); //$NON-NLS-1$ //$NON-NLS-2$
                dialog.open();
            }
        });
        updateErrorMessage();
    }

    private void updateErrorMessage() {
        String errorMessage = MoSyncTool.getDefault().validate();
        if (errorMessage != null) {
            this.errorMessage.setText(MessageFormat.format(Messages.ProfilesView_MoSyncToolConfigurationError, errorMessage));

            errorPane.layout();
        }
    }

    public void createProfilesControl(Composite parent) {
        hookSelectionListener();

        profilePane = new Composite(parent, SWT.NONE);
        profilePane.setLayout(new GridLayout(1, false));

        projectLabel = new Label(profilePane, SWT.NONE);
        GridData projectLabelData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        projectLabel.setLayoutData(projectLabelData);

        currentTarget = new Link(profilePane, SWT.NONE);
        currentTarget.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        currentTarget.addListener(SWT.Selection, new Listener() {
            @Override
			public void handleEvent(Event event) {
                if (currentProject != null) {
                    profileTree.setSelection(new StructuredSelection(currentProject.getTargetProfile()), true);
                    profileTree.getControl().setFocus();
                }
            }
        });

        SashForm main = new SashForm(profilePane, SWT.VERTICAL);
        GridData mainData = new GridData(SWT.FILL, SWT.FILL, true, true);
        main.setLayoutData(mainData);
        // main.setLayout(new FillLayout());

        profileTree = new TreeViewer(main, SWT.BORDER);
        profileLabelProvider = new ProfileLabelProvider(SWT.NONE);
        profileTree.setLabelProvider(profileLabelProvider);
        profileTree.setContentProvider(new ProfileContentProvider());
        profileTree.setInput(MoSyncTool.getDefault().getProfileManager(MoSyncTool.LEGACY_PROFILE_TYPE).getVendors());

        profileTree.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
			public void selectionChanged(SelectionChangedEvent event) {
                setTargetAction.setSelection(event.getSelection());
                showProfileInfo.setSelection(event.getSelection());
                buildForProfile.setSelection(event.getSelection());
            }
        });

        initMenu(profileTree.getControl());
        profileTree.addDoubleClickListener(new IDoubleClickListener() {
            @Override
			public void doubleClick(DoubleClickEvent event) {
                // Double click should expand, unless it's a leaf.
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                if (selection.getFirstElement() instanceof IVendor) {
                    profileTree.setExpandedState(selection.getFirstElement(), !profileTree.getExpandedState(selection.getFirstElement()));
                } else {
                    setTargetAction.run();
                }
            }
        });

        deviceFilter = new DeviceFilterComposite(main, SWT.NONE);

        main.setWeights(new int[] { 3, 2 });

        Link updateProfiles = new Link(profilePane, SWT.NONE);
        updateProfiles.setText(Messages.ProfilesView_UpdateProfilesLink);
        updateProfiles.addSelectionListener(new SelectionAdapter() {
            @Override
			public void widgetSelected(SelectionEvent event) {
            	IUpdater updater = CoreMoSyncPlugin.getDefault().getUpdater();
            	if (updater != null) {
            		updater.update(true);
            	} else {
            		MessageDialog.openError(getSite().getShell(), Messages.ProfilesView_InternalError, Messages.ProfilesView_NoUpdaterPlugin);
            	}
            }
        });

        updateProfiles.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false));

        selectionProvider = new InternalSelectionProvider();
        getSite().setSelectionProvider(selectionProvider);

        projectChanged(null, null);
        currentProjectChanged(false);
    }

    private void initMenu(Control control) {
        final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
			public void menuAboutToShow(IMenuManager manager) {
                IStructuredSelection selection = (IStructuredSelection) profileTree.getSelection();
                if (selection.size() == 1 && selection.getFirstElement() instanceof IProfile) {
                    menuMgr.add(setTargetAction);
                    menuMgr.add(buildForProfile);
                    menuMgr.add(new Separator());
                    menuMgr.add(showProfileInfo);
                }
            }
        });

        control.setMenu(menuMgr.createContextMenu(control));
    }

    private void hookSelectionListener() {
    	MosyncUIPlugin.getDefault().addListener(this);
    }

    private void currentProjectChanged(boolean force) {
        MoSyncProject newProject = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(getSite().getWorkbenchWindow());
        if (force || currentProject != newProject) { // So we really changed to another
                                            // project!
            projectChanged(currentProject, newProject);
        }
    }

    private void projectChanged(MoSyncProject oldProject, final MoSyncProject newProject) {
        if (oldProject != null) {
            oldProject.removePropertyChangeListener(this);
        }

        currentProject = newProject;
        if (newProject != null) {
            newProject.addPropertyChangeListener(this);
        }

        setTargetAction.setCurrentProject(currentProject);
        buildForProfile.setCurrentProject(currentProject);
        profileLabelProvider.setTargetProfileProvider(currentProject);

        profileTree.getTree().getDisplay().asyncExec(new Runnable() {
            @Override
			public void run() {
                if (newProject != null) {
                    profileTree.setInput(newProject.getFilteredVendors());
                    profileTree.setFilters(new ViewerFilter[] { new DeviceViewerFilter(newProject.getDeviceFilter()) });
                    deviceFilter.setCurrentProject(newProject);
                    IProfile target = newProject.getTargetProfile();
                    if (target != null) {
                        profileTree.reveal(newProject.getTargetProfile());
                    }

                    updateProjectText(newProject);
                } else {
                    profileTree.setInput(MoSyncTool.getDefault().getProfileManager(MoSyncTool.LEGACY_PROFILE_TYPE).getVendors());
                    profileTree.setFilters(new ViewerFilter[] { new DeviceViewerFilter( new CompositeDeviceFilter()) });
                    profileTree.refresh();
                }

                profileTree.getControl().setEnabled(newProject != null);
                deviceFilter.setEnabled(newProject != null);

                updateTargetProfileLink(newProject != null ? newProject.getTargetProfile() : null);
            }
        });

        selectionProvider.setSelection(newProject == null ? new StructuredSelection() : new StructuredSelection(newProject.getWrappedProject()));
    }

    protected void updateProjectText(MoSyncProject project) {
        projectLabel.setText(MessageFormat.format(Messages.ProfilesView_ProfilesForProject, project.getName()));
    }

    @Override
	public void propertyChange(PropertyChangeEvent event) {
    	if (profilePane.isDisposed()) {
    		return;
    	}
    	MoSyncProject currentProject = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(getSite().getWorkbenchWindow());
    	if (event.getPropertyName() == MosyncUIPlugin.CURRENT_PROJECT_CHANGED) {
    	    currentProjectChanged(false);
    	} else
        if (event.getPropertyName() == MoSyncTool.MOSYNC_HOME_UPDATED) {
            updateVisiblePane();
            if (currentProject != null) {
            	profileTree.setFilters(new ViewerFilter[] { new DeviceViewerFilter(currentProject.getDeviceFilter()) });
            	profileTree.setInput(MoSyncTool.getDefault().getProfileManager(MoSyncTool.LEGACY_PROFILE_TYPE).getVendors());
            }
        } else if (event.getPropertyName() == MoSyncProject.TARGET_PROFILE_CHANGED) {
            Object oldValue = event.getOldValue();
            Object newValue = event.getNewValue();
            if (oldValue != null) {
                profileTree.refresh(event.getOldValue());
            }

            if (newValue != null) {
                profileTree.refresh(event.getNewValue());
            }

            updateTargetProfileLink(currentProject.getTargetProfile());
        } else if (event.getSource() instanceof IDeviceFilter) {
        	deviceFilter.setCurrentProject(currentProject);
            profileTree.setFilters(new ViewerFilter[] { new DeviceViewerFilter(currentProject.getDeviceFilter()) });
            profileTree.setInput(currentProject == null ? MoSyncTool.getDefault().getProfileManager(MoSyncTool.LEGACY_PROFILE_TYPE).getVendors() : currentProject.getFilteredVendors());
        } else if (event.getPropertyName() == MoSyncTool.PROFILES_UPDATED ||
        		event.getPropertyName() == MoSyncProject.PROFILE_MANAGER_TYPE_KEY) {
            profileTree.getControl().getDisplay().asyncExec(new Runnable() {
                @Override
				public void run() {
                	currentProjectChanged(true);
                }
            });
        }
    }

    private void updateTargetProfileLink(IProfile targetProfile) {
        if (targetProfile != null) {
            currentTarget.setText(MessageFormat.format(Messages.ProfilesView_CurrentProfile, targetProfile));
        } else {
            currentTarget.setText(Messages.ProfilesView_NoProfileSelected);
        }
    }

    @Override
	public Object getAdapter(Class adapter) {
        if (IResource.class.equals(adapter) || IProject.class.equals(adapter)) {
            return currentProject == null ? null : currentProject.getWrappedProject();
        }

        if (IProfile.class.equals(adapter)) {
            return currentProject == null ? null : currentProject.getTargetProfile();
        }

        return super.getAdapter(adapter);
    }

}
