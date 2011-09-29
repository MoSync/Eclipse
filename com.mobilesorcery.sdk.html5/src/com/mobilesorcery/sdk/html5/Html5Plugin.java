package com.mobilesorcery.sdk.html5;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PrivilegedAccess;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.BundleBuildStep.Factory;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.PackBuildStep;

/**
 * The activator class controls the plug-in life cycle
 */
public class Html5Plugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.html5"; //$NON-NLS-1$

	private static final String JS_PROJECT_SUPPORT_PROP = PLUGIN_ID + ".support";

	public static final String HTML5_TEMPLATE_TYPE = "html5";

	// The shared instance
	private static Html5Plugin plugin;

	/**
	 * The constructor
	 */
	public Html5Plugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Html5Plugin getDefault() {
		return plugin;
	}

	/**
	 * Adds HTML5 support to a {@link MoSyncProject}.
	 * @throws CoreException
	 */
	public void addHTML5Support(MoSyncProject project) throws CoreException {
		try {
			BuildSequence sequence = new BuildSequence(project);
			List<IBuildStepFactory> factories = sequence.getBuildStepFactories();
			ArrayList<IBuildStepFactory> newFactories = new ArrayList<IBuildStepFactory>();
			for (IBuildStepFactory factory : factories) {
				if (PackBuildStep.ID.equals(factory.getId())) {
					newFactories.add(createHTML5PackagerBuildStep());
				}
				newFactories.add(factory);
			}
			sequence.apply(newFactories);
			PrivilegedAccess.getInstance().grantAccess(project, true);
			PropertyUtil.setBoolean(project, JS_PROJECT_SUPPORT_PROP, true);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "Could not create JavaScript/HTML5 project", e));
		}
	}

	public boolean hasHTML5Support(MoSyncProject project) {
		return PropertyUtil.getBoolean(project, JS_PROJECT_SUPPORT_PROP);
	}

	private IBuildStepFactory createHTML5PackagerBuildStep() {
		Factory factory = new Factory();
		factory.setFailOnError(true);
		factory.setName("HTML5/JavaScript bundling");
		factory.setInFile("%current-project%/LocalFiles");
		factory.setOutFile("%current-project%/Resources/LocalFiles.bin");
		return factory;
	}
}
