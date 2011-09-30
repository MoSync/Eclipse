package com.mobilesorcery.sdk.html5;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.web.core.internal.project.JsWebNature;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PrivilegedAccess;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.BundleBuildStep.Factory;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.PackBuildStep;
import com.mobilesorcery.sdk.core.build.ResourceBuildStep;

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
				if (ResourceBuildStep.ID.equals(factory.getId())) {
					newFactories.add(createHTML5PackagerBuildStep());
				}
				newFactories.add(factory);
			}
			sequence.apply(newFactories);
			PrivilegedAccess.getInstance().grantAccess(project, true);
			PropertyUtil.setBoolean(project, JS_PROJECT_SUPPORT_PROP, true);

			configureForJSDT(project.getWrappedProject());
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "Could not create JavaScript/HTML5 project", e));
		}
	}

	private void configureForJSDT(IProject project) throws CoreException {
		addJavaScriptNature(project);
		IJavaScriptProject jsProject = JavaScriptCore.create(project);
		if (jsProject instanceof JavaProject) {
			JavaProject jsProject1 = (JavaProject) jsProject;
			jsProject1.setCommonSuperType(new LibrarySuperType(new Path("org.eclipse.wst.jsdt.launching.baseBrowserLibrary"), jsProject1, "Window"));
		} else {
			CoreMoSyncPlugin.getDefault().logOnce(new IllegalStateException("Invalid JSDT version!!"), "JSDT");
		}
	}

	private void addJavaScriptNature(IProject project) throws CoreException {
		if (project.hasNature(JavaScriptCore.NATURE_ID)) {
			return;
		}

		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[newNatures.length - 1] = JavaScriptCore.NATURE_ID;
		description.setNatureIds(newNatures);
		project.setDescription(description, new NullProgressMonitor());
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
