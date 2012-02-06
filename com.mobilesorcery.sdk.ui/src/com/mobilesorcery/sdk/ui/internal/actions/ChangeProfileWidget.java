package com.mobilesorcery.sdk.ui.internal.actions;

import java.beans.PropertyChangeEvent;
import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.PlatformSelectionComposite;
import com.mobilesorcery.sdk.ui.ProfileLabelProvider;
import com.mobilesorcery.sdk.ui.UIUtils;

public class ChangeProfileWidget extends MoSyncProjectWidget {

	private Button profileButton;
	private ProfileLabelProvider lp;

	@Override
	protected Control createControl(Composite parent) {
		lp = new ProfileLabelProvider(SWT.NONE);
		lp.setImageSize(new Point(12, 12));

		attachListeners();
		final Composite dummy = new Composite(parent, SWT.NONE);
		GridLayout layout = UIUtils.newPrefsLayout(1);
		dummy.setLayout(layout);
		GridData dummyData = new GridData(UIUtils.getDefaultFieldSize(),
				SWT.DEFAULT);
		dummy.setLayoutData(dummyData);
		Combo justToGetTheHeight = new Combo(dummy, SWT.READ_ONLY);
		int height = justToGetTheHeight.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 2;

		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.type == SWT.Selection) {
					PlatformSelectionComposite psc = new PlatformSelectionComposite(dummy, SWT.SEARCH | SWT.BACKGROUND);
					psc.setProject(project);
					psc.show(SWT.NONE);
				}
			}
		};
		profileButton = new Button(dummy, SWT.PUSH);
		profileButton.setLayoutData(new GridData(SWT.DEFAULT, height));
		profileButton.addListener(SWT.Selection, listener);
		profileButton.setAlignment(SWT.CENTER);
		GridData profileButtonData = new GridData(UIUtils.getDefaultFieldSize(), height);
		profileButtonData.verticalIndent = 2;
		profileButton.setLayoutData(profileButtonData);
		profileButton.setFont(justToGetTheHeight.getFont());
		dummy.pack();
		justToGetTheHeight.dispose();

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

	public void updateUI(boolean force) {
		String projectName = project == null ? "" : project.getName();
		Image image = project == null ? null : lp.getImage(project.getTargetProfile().getVendor());
		profileButton.setImage(image);
		String profileName = project == null ? "No project selected" : MoSyncTool.toString(project.getTargetProfile());
		profileButton.setText(profileName);
		profileButton.setToolTipText(MessageFormat.format("Set active profile for project {0}", projectName));

		profileButton.setEnabled(project != null);
	}

}
