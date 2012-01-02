package com.mobilesorcery.sdk.ui.internal.actions;

import java.beans.PropertyChangeEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
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

	//private Button profileButton;
	private Label icon;
	private Label name;
	private ProfileLabelProvider lp;
	private boolean projectSelected;

	@Override
	protected Control createControl(Composite parent) {
		lp = new ProfileLabelProvider(SWT.NONE);
		lp.setImageSize(new Point(16, 16));

		attachListeners();
		final Composite dummy = new Composite(parent, SWT.NONE);
		GridLayout layout = UIUtils.newPrefsLayout(3);
		layout.marginTop = 2;
		layout.marginWidth = 2;
		dummy.setLayout(layout);
		GridData dummyData = new GridData(UIUtils.getDefaultFieldSize(),
				SWT.DEFAULT);
		dummy.setLayoutData(dummyData);
		Combo justToGetTheHeight = new Combo(dummy, SWT.READ_ONLY);
		int height = justToGetTheHeight.computeSize(SWT.DEFAULT, SWT.DEFAULT).y - 5;
		justToGetTheHeight.dispose();
		/*profileButton = new SquareButton(dummy, SWT.PUSH);
		profileButton.addListener(SWT.Selection, this);
		GridData profileButtonData = new GridData(
				UIUtils.getDefaultFieldSize(), height);
		profileButtonData.verticalAlignment = SWT.CENTER;
		profileButton.setLayoutData(profileButtonData);*/
		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.type == SWT.MouseDown) {
					PlatformSelectionComposite psc = new PlatformSelectionComposite(dummy, SWT.SEARCH | SWT.BACKGROUND);
					psc.setProject(project);
					psc.show(SWT.NONE);
				} else if (event.widget == name) {
					showAsLink(event.type == SWT.MouseEnter);
				}
			}
		};
		icon = new Label(dummy, SWT.BORDER);
		icon.setAlignment(SWT.CENTER);
		icon.setLayoutData(new GridData(24, height));
		name = new Label(dummy, SWT.NONE);
		name.setLayoutData(new GridData(UIUtils.getDefaultFieldSize() / 2, SWT.DEFAULT));
		icon.addListener(SWT.MouseDown, listener);
		name.addListener(SWT.MouseDown, listener);
		name.addListener(SWT.MouseEnter, listener);
		name.addListener(SWT.MouseExit, listener);

		/*profileButton = new Button(dummy, SWT.PUSH);
		profileButton.setLayoutData(new GridData(SWT.DEFAULT, height));
		profileButton.setText("...");
		profileButton.addListener(SWT.Selection, this);*/
		updateUI(true);
		return dummy;
	}

	protected void showAsLink(boolean asLink) {
		if (projectSelected) {
			String profileName = project == null ? "" : lp.getText(project
					.getTargetProfile());
			name.setText(profileName);
		} else {
			name.setText("No project");
		}
		Display d = name.getDisplay();
		name.setCursor(d.getSystemCursor(projectSelected && asLink ? SWT.CURSOR_HAND : SWT.CURSOR_ARROW));
		name.setForeground(projectSelected && asLink ? d.getSystemColor(SWT.COLOR_BLUE) : null);
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
		showAsLink(false);
		icon.setImage(null);
	}

	public void updateUI(boolean force) {
		boolean activeButton = project != null;
				/*&& project.getProfileManagerType() == MoSyncTool.DEFAULT_PROFILE_TYPE;*/
		projectSelected = activeButton || force;
		if (projectSelected) {
			Image image = project == null ? null : lp.getImage(project
					.getTargetProfile().getVendor());
			showAsLink(false);
			icon.setImage(image);
			//profileButton.setToolTipText(MessageFormat.format(
			//		"Set target profile for project {0}", projectName));
		} else {
			noProjectSelected();
		}
		//profileButton.setEnabled(activeButton);
	}

}
