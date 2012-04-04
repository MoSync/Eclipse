package com.mobilesorcery.sdk.html5.debug;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class JSODDLaunchConfigurationTabGroup extends
		AbstractLaunchConfigurationTabGroup {

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		JSODDMainLaunchConfigurationTab main = new JSODDMainLaunchConfigurationTab();
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { main };
		setTabs(tabs);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy lc) {
		lc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, "org.eclipse.wst.jsdt.debug.core.sourceLocator");
	}

}
