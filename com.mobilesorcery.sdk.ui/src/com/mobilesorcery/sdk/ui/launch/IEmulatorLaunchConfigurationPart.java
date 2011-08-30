package com.mobilesorcery.sdk.ui.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Composite;

import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

/**
 * <p>A UI for changing platform specific emulator launch configuration attributes.
 * (Usually what you see whenever you select something in the "Emulator" dropdown
 * in the "Emulated MoSync App" launch configuration dialog.</p>
 * @author mattias.bybro@mosync.com
 *
 */
public interface IEmulatorLaunchConfigurationPart {

	/**
	 * The extension point id to use.
	 */
	public final static String EXTENSION_POINT_ID = "com.mobilesorcery.sdk.ui.launcher";

	/**
	 * <p>Initializes the UI with the current launch configuration.</p>
	 * @param config
	 * @throws CoreException
	 */
	void init(ILaunchConfiguration config) throws CoreException;

	/**
	 * <p>Creates the UI.</p>
	 * <p>Clients may call on {@code updateable} whenever the entire launch configuration
	 * dialog should be update, for example when validation should occur due to UI events.</p>
	 * @param parent
	 * @param updatable
	 * @return
	 */
	Composite createControl(Composite parent, IUpdatableControl updatable);

	/**
	 * <p>Invoked when the launch configuration is to be updated.</p>
	 * @param copy
	 */
	void apply(ILaunchConfigurationWorkingCopy copy);

	/**
	 * <p>Performs validation of this UI.</p>
	 * @return {@code null} if no errors or warnings should be presented to the user.
	 */
	IMessageProvider validate();

}
