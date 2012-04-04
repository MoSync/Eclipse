package com.mobilesorcery.sdk.html5.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.html5.live.LiveServer;
import com.mobilesorcery.sdk.ui.UIUtils;

public class JSODDMainLaunchConfigurationTab extends AbstractLaunchConfigurationTab {

	private Text timeout;

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(2));
		Label timeoutLabel = new Label(main, SWT.NONE);
		timeoutLabel.setText("Debug Protocol Timeout (seconds):");
		timeout = new Text(main, SWT.BORDER | SWT.SINGLE);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(LiveServer.TIMEOUT_ATTR, LiveServer.DEFAULT_TIMEOUT);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			timeout.setText(Integer.toString(configuration.getAttribute(LiveServer.TIMEOUT_ATTR, LiveServer.DEFAULT_TIMEOUT)));
		} catch (CoreException e) {
			// Ignore.
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		String timeoutStr = timeout.getText();
		try {
			int timeoutValue = Integer.parseInt(timeoutStr);
			configuration.setAttribute(LiveServer.TIMEOUT_ATTR, timeoutValue);
		} catch (Exception e) {
			// Ignore.
		}
	}

	@Override
	public String getName() {
		return "Server Settings";
	}


}
