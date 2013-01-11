package com.mobilesorcery.sdk.extensionsupport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;

public class ExtensionCompiler extends AbstractTool {

	private static ExtensionCompiler instance = null;

	public static ExtensionCompiler getDefault() {
		if (instance == null) {
			instance = new ExtensionCompiler(MoSyncTool.getDefault().getBinary("extcomp"));
		}
		return instance;
	}
	
	public void compile(MoSyncProject project, boolean generateStubs) throws CoreException {
		compile(project, true, generateStubs);
	}
	
	public void generateStubs(MoSyncProject project) throws CoreException {
		compile(project, false, true);
	}
	
	public void compile(MoSyncProject project, boolean generateLib, boolean generateStubs) throws CoreException {
		String extensionName = project.getName();
		String androidPackageName = project.getProperty("android:package.name");
		String androidClassName = "Extension";
		String iosInterfaceName = Character.toUpperCase(extensionName.charAt(0)) + extensionName.substring(1);
		String prefix = PropertyUtil.getBoolean(project, ExtensionSupportPlugin.USE_CUSTOM_PREFIX_PROP) ?
				project.getProperty(ExtensionSupportPlugin.PREFIX_PROP) :
				getDefaultPrefix(project);
		IPath projectPath = project.getWrappedProject().getLocation();
		ArrayList<String> commandLine = new ArrayList<String>();
		commandLine.addAll(Arrays.asList(new String[] { getToolPath().getAbsolutePath(), 
				"--project", projectPath.toOSString(), 
				"--extension", extensionName,
				"--version", project.getProperty(MoSyncBuilder.PROJECT_VERSION),
				"--vendor", project.getProperty(DefaultPackager.APP_VENDOR_NAME_BUILD_PROP),
				"--prefix", prefix,
				"--android-package-name", androidPackageName,
				"--android-class-name", androidClassName,
				"--ios-interface-name", iosInterfaceName }));
		if (generateLib) {
			commandLine.add("--generate-lib");
		}
		if (generateStubs) {
			// Ok, we'll do it here for now:
			File stubsLoc = projectPath.append("stubs").toFile();
			Util.deleteFiles(stubsLoc, null, 8, new NullProgressMonitor());
			commandLine.add("--generate-stubs");
		}
		
		if (execute(commandLine.toArray(new String[commandLine.size()]),
				null, null, MoSyncBuilder.CONSOLE_ID, false) != 0) {
			throw new CoreException(new Status(IStatus.ERROR, ExtensionSupportPlugin.PLUGIN_ID, "IDL compilation failed."));
		}
	}
	
	public static String getDefaultPrefix(MoSyncProject project) {
		String result = project.getName();
		if (result.length() > 2) {
			if (!Character.isUpperCase(result.charAt(0)) || !Character.isUpperCase(result.charAt(1))) {
				char firstChar = Character.toLowerCase(result.charAt(0));
				result = firstChar + result.substring(1);
			}
		}
		return result;
	}
	
	private ExtensionCompiler(IPath toolPath) {
		super(toolPath);
	}

	@Override
	protected String getToolName() {
		return "extcomp";
	}

}
