package com.mobilesorcery.sdk.html5.ui;

import org.eclipse.core.resources.IResource;

import com.mobilesorcery.sdk.core.MoSyncNatureTester;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.html5.Html5Plugin;

public class DebuggingEnableTester extends MoSyncNatureTester {

	public final static String CAN_ENABLE_PROP = "canEnableDebugging";
	public final static String CAN_DISABLE_PROP = "canDisableDebugging";
	
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		MoSyncProject project = extractProject(receiver, property, args, expectedValue);
		if (project == null) {
			return false;
		}
		IResource localFilesFolder = project.getWrappedProject().getFolder(Html5Plugin.getHTML5Folder(project.getWrappedProject()));
		boolean isPreparedForDebugging = localFilesFolder.exists();
		boolean canEnable = !hasDebugSupport(project) && isPreparedForDebugging;
		boolean canDisable = !canEnable && hasDebugSupport(project) && isPreparedForDebugging;
		
		return CAN_ENABLE_PROP.equals(property) ? canEnable : canDisable;
	}
	
	public static boolean hasDebugSupport(MoSyncProject project) {
		if (project == null) {
			return false;
		}
		
		boolean hasSupport = Html5Plugin.getDefault().hasHTML5Support(project);
		if (!hasSupport) {
			return false;
		}
		
		if (!Html5Plugin.getDefault().hasHTML5PackagerBuildStep(project)) {
			return false;
		}
		
		return Html5Plugin.getDefault().isJSODDEnabled(project);
	}
	
}
