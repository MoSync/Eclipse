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
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;

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
    	IEmulatorLauncher launcher = findBestMatch(launchConfig, mode);
       	if (launcher == null) {
    		throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not find any matching emulator"));
    	}

    	launcher.launch(launchConfig, mode, launch, emulatorId, monitor);
	}

	/**
	 * <p>Returns the best matching emulator launcher.</p>
	 * <p>Determines the best match in this order:</p>
	 * <ol>
	 * <li>The session emulator launcher (See {@link EmulatorLaunchConfigurationDelegate#getSessionLauncher(ILaunchConfiguration)}</li>
	 * <li>The preferred emulator launcher (set in the preferences)</li>
	 * <li>The best match according to the {@link IEmulatorLauncher#getLaunchType(IPackager)} score</li>
	 * </ol>
	 * @param config
	 * @param mode
	 * @return
	 */
	public IEmulatorLauncher findBestMatch(ILaunchConfiguration config, String mode) {
		try {
			IEmulatorLauncher sessionLauncher = EmulatorLaunchConfigurationDelegate.getSessionLauncher(config);
			if (sessionLauncher != null) {
				return sessionLauncher;
			}

			// First we need to find the 'real' platform
			IBuildVariant variant = getVariantDefault(config, mode);
			IPackager packager = variant.getProfile().getPackager();
			IEmulatorLauncher preferredLauncher = CoreMoSyncPlugin.getDefault().getPreferredLauncher(packager.getId());
			if (preferredLauncher != null) {
				return preferredLauncher;
			}

	    	int bestScore = 0;
	    	IEmulatorLauncher result = null;
	    	for (String emulatorId : CoreMoSyncPlugin.getDefault().getEmulatorLauncherIds()) {
	    		IEmulatorLauncher launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(emulatorId);
	    		if (!ID.equals(emulatorId)) {
	        		int launchability = launcher.isLaunchable(config, mode);
	        		boolean match = launchability == IEmulatorLauncher.LAUNCHABLE || launchability == IEmulatorLauncher.REQUIRES_CONFIGURATION;
	        		if (match) {
		        		int launcherScore = launcher.getLaunchType(packager);
		        		if (launcherScore > bestScore) {
		        			bestScore = launcherScore;
		        			result = launcher;
		        		}
	        		}
	    		}
	    	}
	    	return result;
		} catch (CoreException e) {
			// Not found.
			CoreMoSyncPlugin.getDefault().log(e);
			return null;
		}

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
		IEmulatorLauncher launcher = findBestMatch(config, mode);
		return launcher.getVariant(config, mode);
	}

	@Override
	public int isLaunchable(ILaunchConfiguration config, String mode) {
		IEmulatorLauncher launcher = findBestMatch(config, mode);
		return launcher.isLaunchable(config, mode);
	}

	@Override
	public IEmulatorLauncher configure(ILaunchConfiguration config, String mode) {
		IEmulatorLauncher launcher = findBestMatch(config, mode);
		return launcher.configure(config, mode);
	}

	@Override
	public int getLaunchType(IPackager packager) {
		return LAUNCH_TYPE_AUTO_SELECT;
	}
}
