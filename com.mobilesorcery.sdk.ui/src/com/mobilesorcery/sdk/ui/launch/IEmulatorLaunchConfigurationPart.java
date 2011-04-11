package com.mobilesorcery.sdk.ui.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;

import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;
import com.mobilesorcery.sdk.ui.internal.launch.MoSyncLaunchParamsTab;

public interface IEmulatorLaunchConfigurationPart {

	public final static String EXTENSION_POINT_ID = "com.mobilesorcery.sdk.ui.launcher";

	void init(ILaunchConfiguration config) throws CoreException;

	Composite createControl(Composite parent, IUpdatableControl updatable);

	void apply(ILaunchConfigurationWorkingCopy copy);
	
}
