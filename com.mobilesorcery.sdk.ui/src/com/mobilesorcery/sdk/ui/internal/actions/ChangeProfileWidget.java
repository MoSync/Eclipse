package com.mobilesorcery.sdk.ui.internal.actions;

import java.beans.PropertyChangeEvent;
import java.text.MessageFormat;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.PlatformSelectionComposite;
import com.mobilesorcery.sdk.ui.ProfileLabelProvider;
import com.mobilesorcery.sdk.ui.UIUtils;

public class ChangeProfileWidget extends MoSyncProjectWidget implements
		Listener {

	private Button profileButton;

	@Override
	protected Control createControl(Composite parent) {
		attachListeners();
		Composite dummy = new Composite(parent, SWT.NONE);
		GridLayout layout = UIUtils.newPrefsLayout(1);
		layout.marginTop = 0;
		dummy.setLayout(layout);
		GridData dummyData = new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT);
		dummy.setLayoutData(dummyData);
		profileButton = new Button(dummy, SWT.PUSH);
		profileButton.addListener(SWT.Selection, this);
		GridData profileButtonData = new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT);
		profileButtonData.verticalAlignment = SWT.TOP;
		profileButton.setLayoutData(profileButtonData);
		updateUI(true);
		return dummy;
	}

	@Override
	public boolean shouldUpdateProject(PropertyChangeEvent event) {
		String prop = event.getPropertyName();
		return MosyncUIPlugin.CURRENT_PROJECT_CHANGED == prop
				|| IDeviceFilter.FILTER_CHANGED == prop
				|| CompositeDeviceFilter.FILTER_ADDED == prop
				|| CompositeDeviceFilter.FILTER_REMOVED == prop
				|| MoSyncProject.PROFILE_MANAGER_TYPE_KEY == prop
				|| MoSyncProject.TARGET_PROFILE_CHANGED == prop;
	}

	@Override
	public void updateUI() {
		updateUI(false);
	}

	@Override
	protected void noProjectSelected() {
		profileButton.setText("No project");
		profileButton.setImage(null);
	}

	public void updateUI(boolean force) {
		boolean activeButton = project != null
				&& project.getProfileManager() == MoSyncTool.getDefault()
						.getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE);
		if (activeButton || force) {
			ProfileLabelProvider lp = new ProfileLabelProvider(SWT.NONE);
			lp.setImageSize(new Point(12, 12));
			String text = project == null ? "" : lp.getText(project.getTargetProfile());
			Image image = project == null ? null : lp.getImage(project.getTargetProfile().getVendor());
			String name = project == null ? "" : project.getName();
			profileButton.setText(text);
			profileButton.setImage(image);
			profileButton.setToolTipText(MessageFormat.format(
					"Set target profile for project {0}", name));
		} else {
			noProjectSelected();
		}
		profileButton.setEnabled(activeButton);
	}

	@Override
	public void handleEvent(Event event) {
		PlatformSelectionComposite psc = new PlatformSelectionComposite(profileButton);
		psc.setProject(getProject());
		psc.show();
	}

}
