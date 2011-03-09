package com.mobilesorcery.sdk.ui.internal.preferences;

import java.util.HashSet;
import java.util.TreeSet;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PrivilegedAccess;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class PrivilegedAccessPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IUpdatableControl {

	private HashSet<MoSyncProject> shouldHaveAccess = new HashSet<MoSyncProject>();
	private TreeSet<MoSyncProject> availableProjects = new TreeSet<MoSyncProject>(MoSyncProject.NAME_COMPARATOR);
	private CheckboxTableViewer accessList;
	private Button selectAll;
	
	public PrivilegedAccessPreferencePage() {
		super("Scripts");
	}

	@Override
	public void init(IWorkbench workbench) {
		for (String projectId : MoSyncProject.listAllProjects()) {
			MoSyncProject project = MoSyncProject.create(projectId);
			if (project != null && MoSyncBuilder.requiresPrivilegedAccess(project)) {
				availableProjects.add(project);
				if (PrivilegedAccess.getInstance().hasAccess(project)) {
					shouldHaveAccess.add(project);
				}
			}
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(1));
		Label instructions = new Label(main, SWT.NONE);
		instructions.setText("Check projects that should have privileged access to run scripts during building.\n(Only projects that requires such privileged access are shown.)");
		final UpdateListener updateListener = new UpdateListener(this);
		accessList = CheckboxTableViewer.newCheckList(main, SWT.BORDER);
		accessList.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		accessList.setContentProvider(new ArrayContentProvider());
		accessList.setCheckStateProvider(new ICheckStateProvider() {
			
			@Override
			public boolean isGrayed(Object element) {
				return false;
			}
			
			@Override
			public boolean isChecked(Object element) {
				return shouldHaveAccess.contains(element);
			}
		});
		
		accessList.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				MoSyncProject project = (MoSyncProject) event.getElement();
				if (event.getChecked()) {
					shouldHaveAccess.add(project);
				} else {
					shouldHaveAccess.remove(project);
				}
				updateListener.handleEvent(null);
			}
			
		});
		
		accessList.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((MoSyncProject) element).getName();
			}
		});
		
		accessList.setInput(availableProjects.toArray());
		
		selectAll = new Button(main, SWT.CHECK);
		selectAll.setText("Select/deselect &All");
		selectAll.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				updateListener.setActive(false);
				boolean allChecked = selectAll.getSelection();
				accessList.setAllChecked(allChecked);
				if (allChecked) {
					shouldHaveAccess.addAll(availableProjects);
				} else {
					shouldHaveAccess.clear();
				}
				updateListener.setActive(true);
			}
			
		});
		
		updateUI();
		return main;
	}

	public void updateUI() {
		boolean allSelected = availableProjects.size() == shouldHaveAccess.size();
		boolean noneSelected = shouldHaveAccess.size() == 0;
		selectAll.setSelection(allSelected);
		selectAll.setGrayed(!allSelected && !noneSelected);
	}
	
	@Override
	public boolean performOk() {
		for (MoSyncProject project : availableProjects) {
			PrivilegedAccess.getInstance().grantAccess(project, shouldHaveAccess.contains(project));
		}
		return super.performOk();
	}
}
