package com.mobilesorcery.sdk.builder.s60;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PackageToolPackager;
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

	/**
	 * Creates a builder for either Symbian 2nd or 3d edition.
	 *
	 * @param uidString Defines which Symbian UID string to use.
	 * @param resultExtension The extension of the resulting package.
	 */
	public V2V3Packager(String uidString, String resultExtension)
	{
		uidPropertyName = uidString;
		this.resultExtension = resultExtension;
	}

	@Override
	public void createPackage(MoSyncProject project,
			IBuildVariant variant, IBuildResult buildResult)
			throws CoreException {
		DefaultPackager internal = new DefaultPackager(project, variant);
		File packageOutputDir = internal.resolveFile("%package-output-dir%"); //$NON-NLS-1$
		String appName = internal.getParameters( ).get( DefaultPackager.APP_NAME );

		super.createPackage(project, variant, buildResult);
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
		String uid = formatUID(project.getProperty(uidPropertyName) );
		commandLine.flag( "--uid" ).with( uid );
	}
}
