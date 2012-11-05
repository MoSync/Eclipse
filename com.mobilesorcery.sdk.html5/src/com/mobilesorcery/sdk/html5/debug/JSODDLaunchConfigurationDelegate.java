package com.mobilesorcery.sdk.html5.debug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.wst.jsdt.debug.internal.core.launching.JavaScriptProcess;
import org.eclipse.wst.jsdt.debug.internal.core.model.JavaScriptDebugTarget;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadListeningConnector;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;

public class JSODDLaunchConfigurationDelegate implements
		ILaunchConfigurationDelegate2 {

	public static final String ID = "com.mobilesorcery.html5.jsodd.launchconfigurationtype";
	
	private static HashMap<ITargetPhone, ILaunch> associatedLaunches = new HashMap<ITargetPhone, ILaunch>();
	
	private final class JSODDProcess extends JavaScriptProcess {
		private IStreamsProxy streams = null;
		private final ReloadVirtualMachine vm;

		private JSODDProcess(ReloadVirtualMachine vm, ILaunch launch, String name) {
			super(launch, name);
			this.vm = vm;
		}

		@Override
		public IStreamsProxy getStreamsProxy() {
			if (streams == null) {
				streams = new JSODDStreamsProxy(vm);
			}

			return streams;
		}
	}

	private static final String DEFAULT_LAUNCH_CONFIG = "default.launch.config";
	private static final String LAUNCH_CONFIG_TYPE = "com.mobilesorcery.html5.jsodd.launchconfigurationtype";

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		try {
			runJSDTRemoteConnector(launch);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode)
			throws CoreException {
		Launch launch = new Launch(configuration, mode, null);
		setDefaultSourceLocator(launch, configuration);
		return launch;
	}

	protected void setDefaultSourceLocator(ILaunch launch,
			ILaunchConfiguration configuration) throws CoreException {
		IPersistableSourceLocator sourceLocator = null;
		if (launch.getSourceLocator() == null) {
			String id = configuration.getAttribute(
					ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String) null);
			if (id == null) {
				id = "org.eclipse.wst.jsdt.debug.core.sourceLocator";
			}
			sourceLocator = DebugPlugin.getDefault().getLaunchManager()
					.newSourceLocator(id);
			if (sourceLocator != null) {
				sourceLocator.initializeDefaults(configuration);
				launch.setSourceLocator(sourceLocator);
			}
		}
	}

	public void runJSDTRemoteConnector(ILaunch launch) throws IOException {
		// This is more or less extracted from JSDTs remote java script launcher
		ReloadListeningConnector connector = ReloadListeningConnector
				.getDefault();
		Map arguments = new HashMap<String, String>();
		ReloadVirtualMachine vm = (ReloadVirtualMachine) connector.accept(arguments);
		
		// TODO: refactor
		JavaScriptProcess process = new JSODDProcess(vm, launch, "JavaScript On-device Debug");
		launch.addProcess(process);

		final boolean isDebugging = CoreMoSyncPlugin.getDefault().isDebugging();
		JavaScriptDebugTarget target = new JavaScriptDebugTarget(vm, process,
				launch, true, true) {
			/* Deadlock issue! See https://bugs.eclipse.org/bugs/show_bug.cgi?id=249951
			// for more info. What may happen is this:
			// 1) The target will initialize breakpoints, locking the target object
			// 2) This will trigger an autobuild job (with a delay of >100 ms), and
			// if the build job starts before 1) above has completed, this job will
			// also lock the target object at this method (via notifications)
			// and we have a deadlock. */
			public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
				// So we just remove the synchronized keyword. Now, there could of
				// course be more like this one (sigh!)
				// http://bertrandmeyer.com/2011/06/20/concurrent-programming-is-easy/
				if (isDebugging) {
					CoreMoSyncPlugin.trace("Breakpoint changed, recv'd event @ " + Thread.currentThread().getName());
				}
			}
		};
		launch.addDebugTarget(target);
		vm.setDebugTarget(target);
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		// We never really build; this is just for launching the debugger
		return false;
	}

	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		return true;
	}

	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		// TODO Only JS ODD enabled projects here
		return true;
	}

	public static boolean launchDefault(String terminateToken) throws CoreException {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		// TODO: Kill old ones.

		ILaunchConfigurationType type = manager.getLaunchConfigurationType(LAUNCH_CONFIG_TYPE);
		ILaunchConfiguration cfg = null;
		if (type != null) {
			ILaunchConfiguration[] configurations = manager.getLaunchConfigurations(type);
			for (int i = 0; i < configurations.length; i++) {
				ILaunchConfiguration configuration = configurations[i];
				if (configuration.getAttribute(DEFAULT_LAUNCH_CONFIG, false)) {
					cfg = configuration;
				}
			}

			if (cfg == null) {
				ILaunchConfigurationWorkingCopy wc = type.newInstance(null, "Default JavaScript On-Device Debug");
				wc.setAttribute(DEFAULT_LAUNCH_CONFIG, true);
				wc.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
				wc.doSave();
				
				cfg = wc;
			}
		}

		ILaunch launch = null;
		if (cfg != null) {
			launch = DebugUITools.buildAndLaunch(cfg, ILaunchManager.DEBUG_MODE, new NullProgressMonitor());
			if (launch != null) {
				launch.setAttribute(Html5Plugin.TERMINATE_TOKEN_LAUNCH_ATTR, terminateToken);
			}
			ILaunch[] previousLaunches = manager.getLaunches();
			for (ILaunch previousLaunch : previousLaunches) {
				// To avoid spamming the user with spurious timeout messages...
				if (previousLaunch != launch && 
					previousLaunch.getLaunchConfiguration().getType().getIdentifier().equals(LAUNCH_CONFIG_TYPE) &&
					Util.equals(previousLaunch.getAttribute(Html5Plugin.TERMINATE_TOKEN_LAUNCH_ATTR), terminateToken)) {
					previousLaunch.setAttribute(Html5Plugin.SUPPRESS_TIMEOUT_LAUNCH_ATTR, Boolean.TRUE.toString());
				}
			}
		}

		return launch != null;
	}
	
	public static void killLaunch(String terminateToken) throws CoreException {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunch[] launches = manager.getLaunches();
		for (ILaunch launch : launches) {
			// To avoid spamming the user with spurious timeout messages...
			if (launch.getLaunchConfiguration().getType().getIdentifier().equals(LAUNCH_CONFIG_TYPE) &&
				Util.equals(launch.getAttribute(Html5Plugin.TERMINATE_TOKEN_LAUNCH_ATTR), terminateToken)) {
				launch.terminate();
			}
		}
	}

	private static boolean isDefaultLaunch(ILaunch launch) throws CoreException {
		ILaunchConfiguration launchCfg = launch.getLaunchConfiguration();
		if (LAUNCH_CONFIG_TYPE.equals(launchCfg.getType().getIdentifier())) {
			return launchCfg.getAttribute(DEFAULT_LAUNCH_CONFIG, false);
		}
		return false;
	}
}
