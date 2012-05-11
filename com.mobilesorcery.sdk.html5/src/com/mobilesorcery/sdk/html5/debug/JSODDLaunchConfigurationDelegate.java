package com.mobilesorcery.sdk.html5.debug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.internal.core.launching.JavaScriptProcess;
import org.eclipse.wst.jsdt.debug.internal.core.model.JavaScriptDebugTarget;

import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadListeningConnector;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadVirtualMachine;

public class JSODDLaunchConfigurationDelegate implements
		ILaunchConfigurationDelegate2 {

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
		JavaScriptProcess process = new JSODDProcess(vm, launch, "Reload");
		launch.addProcess(process);

		JavaScriptDebugTarget target = new JavaScriptDebugTarget(vm, process,
				launch, true, true);
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

	public static boolean launchDefault() throws CoreException {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunch[] launches = manager.getLaunches();
		for (ILaunch launch : launches) {
			if (isDefaultLaunch(launch) && !launch.isTerminated()) {
				return true;
			}
		}

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
				wc.doSave();
				cfg = wc;
			}
		}

		if (cfg != null) {
			DebugUITools.launch(cfg, ILaunchManager.DEBUG_MODE);
		}

		return false;
	}

	private static boolean isDefaultLaunch(ILaunch launch) throws CoreException {
		ILaunchConfiguration launchCfg = launch.getLaunchConfiguration();
		if (LAUNCH_CONFIG_TYPE.equals(launchCfg.getType().getIdentifier())) {
			return launchCfg.getAttribute(DEFAULT_LAUNCH_CONFIG, false);
		}
		return false;
	}
}
