package com.mobilesorcery.sdk.importproject;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.importproject.messages"; //$NON-NLS-1$
	public static String FindProjectsRunnable_ScanningDirectories;
	public static String FindProjectsRunnable_ScanningProgress;
	public static String ImportProjectsRunnable_10;
	public static String ImportProjectsRunnable_ImportFailed;
	public static String ImportProjectsRunnable_ImportProgress;
	public static String ImportProjectsRunnable_SomeProjectsFailed;
	public static String ImportProjectsRunnable_UniqueNameCreationFailed;
	public static String WizardOldMobuildProjectsImportPage_Browse;
	public static String WizardOldMobuildProjectsImportPage_CopyAllFiles;
	public static String WizardOldMobuildProjectsImportPage_CopyProjectFiles;
	public static String WizardOldMobuildProjectsImportPage_DeselectAll;
	public static String WizardOldMobuildProjectsImportPage_DoNotCopy;
	public static String WizardOldMobuildProjectsImportPage_ImportExisting;
	public static String WizardOldMobuildProjectsImportPage_ImportProjects;
	public static String WizardOldMobuildProjectsImportPage_ScanDirectory;
	public static String WizardOldMobuildProjectsImportPage_SelectAll;
	public static String WizardOldMobuildProjectsImportPage_SelectRootPath;
	public static String WizardOldMobuildProjectsImportPage_SelectScanPaths;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
