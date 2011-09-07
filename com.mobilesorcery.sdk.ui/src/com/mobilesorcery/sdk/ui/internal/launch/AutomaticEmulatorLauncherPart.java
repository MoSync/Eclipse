package com.mobilesorcery.sdk.ui.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;
import com.mobilesorcery.sdk.ui.launch.IEmulatorLaunchConfigurationPart;

public class AutomaticEmulatorLauncherPart implements IEmulatorLaunchConfigurationPart {

	@Override
	public void init(ILaunchConfiguration config) throws CoreException {
	}

	@Override
	public Composite createControl(Composite parent, IUpdatableControl updatable) {
		Composite result = new Composite(parent, SWT.NONE);
		result.setLayout(new GridLayout(1, false));
		Label info = new Label(result, SWT.WRAP);
		info.setText("This launch configuration will try to match the current target profile with its corresponding native emulator.\n" +
				"To edit the platform specific behaviour, select another emulator in the drop down, then re-select this item.");
		info.setFont(MosyncUIPlugin.getDefault().getFont(MosyncUIPlugin.FONT_INFO_TEXT));
		info.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return result;
	}

	@Override
	public void apply(ILaunchConfigurationWorkingCopy copy) {
	}

	@Override
	public IMessageProvider validate() {
		return null;
	}

}
