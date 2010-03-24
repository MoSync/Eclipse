/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.builder.java;

import com.mobilesorcery.sdk.internal.builder.MoSyncIconBuilderVisitor;
import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;

public class JavaPackager extends AbstractPackager {

	public JavaPackager() {
	}

	public void createPackage(MoSyncProject project, IBuildVariant variant, IBuildResult buildResult) throws CoreException {
		DefaultPackager internal = new DefaultPackager(project, variant);
		IProfile targetProfile = variant.getProfile();
		
		internal.setParameters(getParameters());
		internal.setParameter("D", shouldUseDebugRuntimes() ? "D" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		try {
			File packageOutputDir = internal.resolveFile("%package-output-dir%"); //$NON-NLS-1$
			packageOutputDir.mkdirs();

			File projectJar = new File(internal.resolve("%package-output-dir%\\%project-name%.jar")); //$NON-NLS-1$
			File projectJad = new File(internal.resolve("%package-output-dir%\\%project-name%.jad")); //$NON-NLS-1$

			projectJar.delete();
			projectJad.delete();

			File manifest = new File(internal.resolve("%compile-output-dir%\\META-INF\\manifest.mf")); //$NON-NLS-1$
			createManifest(project.getName(), manifest);

			// Need to set execution dir, o/w zip will not understand what we
			// really want.
			internal.getExecutor().setExecutionDirectory(manifest.getParentFile().getParent());
			internal.runCommandLine("cmd", "/c", "copy", "%runtime-dir%\\MoSyncRuntime%D%.jar", projectJar.getAbsolutePath(), "/y"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			internal.runCommandLine("cmd", "/c", "copy", "%runtime-dir%\\config.h", packageOutputDir.getAbsolutePath(), "/y"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

			internal.runCommandLine("%mosync-bin%\\zip", "-j", "-9", projectJar.getAbsolutePath(), "%compile-output-dir%\\program"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			internal.runCommandLine("%mosync-bin%\\zip", "-r", "-9", projectJar.getAbsolutePath(), "META-INF"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			File resources = new File(internal.resolve("%compile-output-dir%\\resources")); //$NON-NLS-1$
			if (resources.exists()) { // TODO: Old resources?
				internal.runCommandLine("%mosync-bin%\\zip", "-j", "-9", projectJar.getAbsolutePath(), "%compile-output-dir%\\resources"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}

			createJAD(project.getWrappedProject().getName(), projectJad, projectJar);

			MoSyncIconBuilderVisitor visitor = new MoSyncIconBuilderVisitor();
			visitor.setProject(project.getWrappedProject());
			IResource[] iconFiles = visitor.getIconFiles();
			if (iconFiles.length > 0) {
				Object xObj = targetProfile.getProperties().get("MA_PROF_CONST_ICONSIZE_X"); //$NON-NLS-1$
				Object yObj = targetProfile.getProperties().get("MA_PROF_CONST_ICONSIZE_Y"); //$NON-NLS-1$
				if (xObj != null && yObj != null) {
					String sizeStr = ((Long) xObj) + "x" + ((Long) yObj); //$NON-NLS-1$
					internal.runCommandLine("%mosync-bin%\\icon-injector", "-src", iconFiles[0].getLocation().toOSString(), "-size", sizeStr, "-platform", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						"j2me", "-dst", projectJar.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

			buildResult.setBuildResult(projectJar);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, "com.mobilesorcery.sdk.builder.java", Messages.JavaPackager_PackageError, e)); //$NON-NLS-1$
		}
	}

	private void createManifest(String projectName, File manifest) throws IOException {
		manifest.getParentFile().mkdirs();

		DefaultPackager.writeFile(manifest, getManifest(projectName));
	}

	private void createJAD(String projectName, File jad, File jar) throws IOException {
		long jarSize = jar.length();
		String jarName = jar.getName();
		String jadString = getManifest(projectName) + "MIDlet-Jar-Size: " + Long.toString(jarSize) + "\n" + "MIDlet-Jar-URL: " + jarName + "\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		DefaultPackager.writeFile(jad, jadString);
	}

	private String getManifest(String projectName) {
		return "Manifest-Version: 1.0\n" + "MIDlet-Vendor: Mobile Sorcery\n" + "MIDlet-Name: " + projectName + "\n" + "MIDlet-1: " + projectName + ", " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				+ projectName + ".png, MAMidlet\n" + "MIDlet-Version: 1.0\n" + "MicroEdition-Configuration: CLDC-1.1\n" + "MicroEdition-Profile: MIDP-2.0\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

}
