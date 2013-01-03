package com.mobilesorcery.sdk.extensionsupport;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class ExtensionCompiler extends AbstractTool {

	private static ExtensionCompiler instance = null;

	public static ExtensionCompiler getDefault() {
		if (instance == null) {
			instance = new ExtensionCompiler(MoSyncTool.getDefault().getBinary("extcomp"));
		}
		return instance;
	}
	
	public void compile(MoSyncProject project) throws CoreException {
		String extensionName = project.getName();
		String androidPackageName = project.getProperty("android:package.name");
		String androidClassName = "Extension";
		String iosInterfaceName = Character.toUpperCase(extensionName.charAt(0)) + extensionName.substring(1);
		if (execute(new String[] { getToolPath().getAbsolutePath(), 
				"--project", project.getWrappedProject().getLocation().toOSString(), 
				"--extension", extensionName,
				"--version", project.getProperty(MoSyncBuilder.PROJECT_VERSION),
				"--vendor", project.getProperty(DefaultPackager.APP_VENDOR_NAME_BUILD_PROP),
				"--android-package-name", androidPackageName,
				"--android-class-name", androidClassName,
				"--ios-interface-name", iosInterfaceName },
				null, null, MoSyncBuilder.CONSOLE_ID, false) != 0) {
			throw new CoreException(new Status(IStatus.ERROR, ExtensionSupportPlugin.PLUGIN_ID, "IDL compilation failed."));
		}
	}
	
	private ExtensionCompiler(IPath toolPath) {
		super(toolPath);
	}

	@Override
	protected String getToolName() {
		return "extcomp";
	}

}
