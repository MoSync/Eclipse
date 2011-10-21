package com.mobilesorcery.sdk.builder.s60;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;

import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PackageToolPackager;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * A class that contains the shared code between the 2nd and 3d edition
 * Symbian packagers.
 *
 * @author Mattias
 */
public class V2V3Packager extends PackageToolPackager {

	String uidPropertyName;

	String resultExtension;

	private final String uidParameter;

	/**
	 * Creates a builder for either Symbian 2nd or 3d edition.
	 *
	 * @param uidString Defines which Symbian UID string to use.
	 * @param resultExtension The extension of the resulting package.
	 */
	public V2V3Packager(String uidString, String resultExtension, String uidParameter)
	{
		uidPropertyName = uidString;
		this.resultExtension = resultExtension;
		this.uidParameter = uidParameter;
	}

	@Override
	public void createPackage(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult buildResult)
			throws CoreException {
		DefaultPackager internal = new DefaultPackager(project, variant);
		File packageOutputDir = internal.resolveFile("%package-output-dir%"); //$NON-NLS-1$
		String appName = internal.getParameters( ).get( DefaultPackager.APP_NAME );

		super.createPackage(project, session, variant, diff, buildResult);
		// TODO: Extract build result from packager?
		buildResult.setBuildResult(new File(packageOutputDir, appName + resultExtension));
	}

	/**
	 * Removes 0x in front of the UID if necessary.
	 *
	 * @param project To look
	 * @return The UID associated with the project.
	 */
	public static String formatUID(String uid)
	{
		if ( uid.startsWith("0x") ) { //$NON-NLS-1$
			uid = uid.substring( 2 );
		}

		return uid;
	}

	@Override
	protected void addPlatformSpecifics(MoSyncProject project,
			IBuildVariant variant, CommandLineBuilder commandLine) {
		/* Symbian UID */
		String uid = formatUID(project.getProperty(uidPropertyName));
		commandLine.flag(uidParameter).with( uid );

		boolean useProjectSpecificKeys = PropertyUtil.getBoolean(project, PropertyInitializer.S60_PROJECT_SPECIFIC_KEYS);
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		String certFile = useProjectSpecificKeys ?
				project.getProperty(PropertyInitializer.S60_CERT_FILE) :
				prefs.getString(PropertyInitializer.S60_CERT_FILE);
		String pass = useProjectSpecificKeys ?
				project.getProperty(PropertyInitializer.S60_PASS_KEY) :
				prefs.getString(PropertyInitializer.S60_PASS_KEY);
		String keyFile = useProjectSpecificKeys ?
				project.getProperty(PropertyInitializer.S60_KEY_FILE) :
				prefs.getString(PropertyInitializer.S60_KEY_FILE);
		commandLine.flag("--s60cert").with(new File(certFile));
		commandLine.flag("--s60key").with(new File(keyFile));
		commandLine.flag("--s60pass", true).with(pass);
	}

	@Override
	protected File getDefaultIconFile() {
		return null;
	}
}
