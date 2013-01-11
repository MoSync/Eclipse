package com.mobilesorcery.sdk.extensionsupport;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.PackBuildStep;
import com.mobilesorcery.sdk.core.build.ResourceBuildStep;
import com.mobilesorcery.sdk.extensionsupport.ExtensionSupportBuildStep.Factory;

/**
 * The activator class controls the plug-in life cycle
 */
public class ExtensionSupportPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.extensionsupport"; //$NON-NLS-1$

	private static final String EXT_PROP_PREFIX = MoSyncBuilder.BUILD_PREFS_PREFIX + "extensions.";
	
	public static final String PREFIX_PROP = EXT_PROP_PREFIX + "prefix";
	
	public static final String USE_CUSTOM_PREFIX_PROP = PREFIX_PROP + ".default";

	public static final String GENERATE_JS_PROP = EXT_PROP_PREFIX + "js";

	// The shared instance
	private static ExtensionSupportPlugin plugin;
	
	/**
	 * The constructor
	 */
	public ExtensionSupportPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ExtensionSupportPlugin getDefault() {
		return plugin;
	}

	public void addExtensionBuildsteps(MoSyncProject project) throws CoreException {
		try {
			BuildSequence sequence = new BuildSequence(project);
			List<IBuildStepFactory> factories = sequence
					.getBuildStepFactories();
			ArrayList<IBuildStepFactory> newFactories = new ArrayList<IBuildStepFactory>();
			Factory idlStep = new ExtensionSupportBuildStep.Factory();
			idlStep.setPhase(ExtensionSupportBuildStep.IDL_PHASE);
			Factory packStep = new ExtensionSupportBuildStep.Factory();
			packStep.setPhase(ExtensionSupportBuildStep.PACK_PHASE);
			newFactories.add(idlStep);
			for (IBuildStepFactory factory : factories) {
				newFactories.add(factory);
				if (PackBuildStep.ID.equals(factory.getId())) {
					newFactories.add(packStep);
				}
			}
			sequence.apply(newFactories);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
					"Could not create Extension project", e));
		}
	}

}
