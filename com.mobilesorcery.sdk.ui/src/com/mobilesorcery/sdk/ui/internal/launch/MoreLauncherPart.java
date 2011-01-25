package com.mobilesorcery.sdk.ui.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;
import com.mobilesorcery.sdk.ui.launch.IEmulatorLaunchConfigurationPart;

public class MoreLauncherPart implements IEmulatorLaunchConfigurationPart {

	private Text widthText;
	private Text heightText;
	private Button useTargetProfile;
	private Group screenSizeGroup;

	@Override
	public void init(ILaunchConfiguration config) throws CoreException {
		useTargetProfile.setSelection(config.getAttribute(
				ILaunchConstants.SCREEN_SIZE_OF_TARGET, true));
		widthText.setText(config.getAttribute(
				ILaunchConstants.SCREEN_SIZE_WIDTH, "176"));
		heightText.setText(config.getAttribute(
				ILaunchConstants.SCREEN_SIZE_HEIGHT, "220"));
	}

	@Override
	public Composite createControl(Composite parent, IUpdatableControl updatable) {
		createResolutionEditor(parent, updatable);
		return screenSizeGroup;
	}

	private void createResolutionEditor(Composite control, IUpdatableControl updatable) {
		screenSizeGroup = new Group(control, SWT.NONE);
		screenSizeGroup.setText("Screen Size");
		screenSizeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridLayout layout = new GridLayout(2, false);
		screenSizeGroup.setLayout(layout);

		useTargetProfile = new Button(screenSizeGroup, SWT.CHECK);
		useTargetProfile
				.setText("If possible, &Use Screen Size of Currently Selected Target Profile");
		useTargetProfile.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true,
				false, 2, 1));

		Label widthL = new Label(screenSizeGroup, SWT.NONE);
		widthL.setText("&Width");

		Label heightL = new Label(screenSizeGroup, SWT.NONE);
		heightL.setText("&Height");

		widthText = new Text(screenSizeGroup, SWT.BORDER | SWT.SINGLE);
		heightText = new Text(screenSizeGroup, SWT.BORDER | SWT.SINGLE);
		widthText.setLayoutData(new GridData(110, SWT.DEFAULT));
		heightText.setLayoutData(new GridData(110, SWT.DEFAULT));

		UpdateListener listener = new UpdateListener(updatable);
		widthText.addListener(SWT.Modify, listener);
		heightText.addListener(SWT.Modify, listener);
		useTargetProfile.addListener(SWT.Selection, listener);
	}

	@Override
	public void apply(ILaunchConfigurationWorkingCopy copy) {
		copy.setAttribute(ILaunchConstants.SCREEN_SIZE_OF_TARGET,
				useTargetProfile.getSelection());
		copy.setAttribute(ILaunchConstants.SCREEN_SIZE_WIDTH, widthText
				.getText().trim());
		copy.setAttribute(ILaunchConstants.SCREEN_SIZE_HEIGHT, heightText
				.getText().trim());

	}

}
