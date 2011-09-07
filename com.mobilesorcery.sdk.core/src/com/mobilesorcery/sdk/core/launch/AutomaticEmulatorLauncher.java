package com.mobilesorcery.sdk.core.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;

// NOTE: Brittle impl; to get the proper fallback behaviour,
// we rely on excluding unconfigured launchers in these
// methods:
// * launch
// * getVariant
// If they were to be used outside a launching context, bugs are sure to
// follow. FIXME after 2.6 release.
public class AutomaticEmulatorLauncher extends AbstractEmulatorLauncher {

	public final static String ID = "auto";

	public AutomaticEmulatorLauncher() {
		super("Select Automatically");
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor) throws CoreException {
    	String launcherId = findMatchingEmulatorDelegate(launchConfig, mode, true);
    	if (launcherId == null) {
    		throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not find any matching emulator"));
    	}

    	IEmulatorLauncher emulatorLauncher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(launcherId);
    	emulatorLauncher.launch(launchConfig, mode, launch, emulatorId, monitor);
	}

    private static String findMatchingEmulatorDelegate(ILaunchConfiguration config, String mode, boolean excludeUnconfigured) {
    	int bestScore = 0;
    	String result = null;
    	for (String emulatorId : CoreMoSyncPlugin.getDefault().getEmulatorLauncherIds()) {
    		IEmulatorLauncher launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(emulatorId);
    		if (!ID.equals(emulatorId)) {
        		int launchability = launcher.isLaunchable(config, mode);
        		boolean match = launchability == IEmulatorLauncher.LAUNCHABLE || (excludeUnconfigured ? false : launchability == IEmulatorLauncher.REQUIRES_CONFIGURATION);
        		if (match) {
	        		int launcherScore = MoReLauncher.ID.equals(emulatorId) ? 10 : 100;
	        		if (!ID.equals(emulatorId) && launcherScore > bestScore) {
	        			bestScore = launcherScore;
	        			result = emulatorId;
	        		}
        		}
    		}
    	}
    	return result;
	}

	@Override
	public void setDefaultAttributes(ILaunchConfigurationWorkingCopy wc) {
    	for (String emulatorId : CoreMoSyncPlugin.getDefault().getEmulatorLauncherIds()) {
    		IEmulatorLauncher launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(emulatorId);
    		if (!ID.equals(emulatorId)) {
    			launcher.setDefaultAttributes(wc);
    		}
    	}
	}

	@Override
	public IBuildVariant getVariant(ILaunchConfiguration config, String mode) throws CoreException {
		String launcherId = findMatchingEmulatorDelegate(config, mode, true);
		IEmulatorLauncher launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(launcherId);
		return launcher.getVariant(config, mode);
	}

	@Override
	public int isLaunchable(ILaunchConfiguration config, String mode) {
		String launcherId = findMatchingEmulatorDelegate(config, mode, false);
		IEmulatorLauncher launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(launcherId);
		return launcher.isLaunchable(config, mode);
	}

	@Override
	public String configure(ILaunchConfiguration config, String mode) {
		String launcherId = findMatchingEmulatorDelegate(config, mode, false);
		IEmulatorLauncher launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(launcherId);
		return launcher.configure(config, mode);
	}
}
