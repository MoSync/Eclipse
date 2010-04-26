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
package com.mobilesorcery.sdk.builder.s60;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.templates.Template;
import com.mobilesorcery.sdk.internal.builder.MoSyncIconBuilderVisitor;
import com.mobilesorcery.sdk.profiles.IProfile;

public class V3Packager extends S60Packager {

	static final int V3_EXE_HEADER_SIZE = 0x9c;

	public V3Packager() {
	}

	public void createPackage(MoSyncProject project, IBuildVariant variant, IBuildResult buildResult) throws CoreException {
		DefaultPackager internal = new DefaultPackager(project, variant);
		IProfile targetProfile = variant.getProfile();
		internal.setParameter("D", shouldUseDebugRuntimes() ? "D" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		try {
			// Some elegant copy 'n' paste from V2Packager.java...
			File packageOutputDir = new File(internal.resolve("%package-output-dir%")); //$NON-NLS-1$
			packageOutputDir.mkdirs();

			String uid = project.getProperty(PropertyInitializer.S60V3_UID);
			if (uid.startsWith("0x")) { //$NON-NLS-1$
				uid = uid.substring(2);
			}

			String appname = project.getName();

			File runtimeDir = new File(internal.resolve("%runtime-dir%")); //$NON-NLS-1$
			String runtimePath = internal.resolve("%runtime-dir%\\MoSync%D%.exe"); //$NON-NLS-1$

			// bin-hack
			try {
				createExe(new File(runtimePath), packageOutputDir, uid, internal);
				createRegRsc(new File(runtimeDir, "MoSync_reg.RSC"), packageOutputDir, uid); //$NON-NLS-1$
				createResourceFile(new File(runtimeDir, "MoSync.RSC"), packageOutputDir, uid, appname); //$NON-NLS-1$
			} catch (RuntimeException e) {
				throw new IOException("Invalid runtime(s)", e);
			}

			// handle icon
			MoSyncIconBuilderVisitor visitor = new MoSyncIconBuilderVisitor();
			visitor.setProject(project.getWrappedProject());
			IResource[] iconFiles = visitor.getIconFiles();
			boolean hasIcon = false;
			if (iconFiles.length > 0) {
				IResource iconFile = iconFiles[0];
				Object xObj = targetProfile.getProperties().get("MA_PROF_CONST_ICONSIZE_X"); //$NON-NLS-1$
				Object yObj = targetProfile.getProperties().get("MA_PROF_CONST_ICONSIZE_Y"); //$NON-NLS-1$
				String sizeStr;
				if (xObj != null && yObj != null) {
					sizeStr = ((Long) xObj) + "x" + ((Long) yObj); //$NON-NLS-1$
				} else {
					sizeStr = "default"; //$NON-NLS-1$
				}
				internal.runCommandLine("%mosync-bin%\\icon-injector", "-src", //$NON-NLS-1$ //$NON-NLS-2$
					iconFile.getLocation().toOSString(),
					"-size", sizeStr, "-platform", "symbian9", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					"-dst", packageOutputDir + "/" + uid + "_icon.mif"); //$NON-NLS-1$
				hasIcon = true;
			}
			
			// write package file
			String template = Util.readFile(runtimeDir.getAbsolutePath() + "/MoSync-template.pkg"); //$NON-NLS-1$
			internal.setParameter("uid", uid); //$NON-NLS-1$
			internal.setParameter("vendor-name", internal.getProjectProperties().getProperty(DefaultPackager.APP_VENDOR_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
			internal.setParameter("has-icon", hasIcon ? "" : ";");
			String resolvedTemplate = Template.preprocess(template, internal.getParameters().toMap());
			File pkgFile = new File(packageOutputDir, uid + ".pkg"); //$NON-NLS-1$
			Util.writeToFile(pkgFile, resolvedTemplate);
			
			// compile sis file
			internal.runCommandLine("%mosync-bin%\\makesis-4.exe", pkgFile.getAbsolutePath()); //$NON-NLS-1$

			File unsignedSis = new File(packageOutputDir, uid + ".sis"); //$NON-NLS-1$
			File renamedAppSis = new File(packageOutputDir, project.getName() + ".sis"); //$NON-NLS-1$
			internal.runCommandLine(new String[] {"%mosync-bin%\\signsis-4.exe", //$NON-NLS-1$
							unsignedSis.getAbsolutePath(),
							renamedAppSis.getAbsolutePath(),
							project.getProperty(PropertyInitializer.S60_CERT_FILE),
							project.getProperty(PropertyInitializer.S60_KEY_FILE),
							project.getProperty(PropertyInitializer.S60_PASS_KEY) }, 
							"*** COMMAND LINE WITHHELD, CONTAINS PASSWORDS ***");

			buildResult.setBuildResult(renamedAppSis);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
		}

	}

	private void createRegRsc(File regrscTemplate, File packageOutputDir, String uidStr) throws IOException {
		byte[] buffer = readFile(regrscTemplate);

		int uid = (int)Long.parseLong(uidStr, 16);

		writeInt(uid, buffer, 8);
		int checksum = CRC16.getChecksumForUID(buffer);
		writeInt(checksum, buffer, 12);

		for (int i = 0; i < uidStr.length(); i++) {
			byte _c = (byte)(uidStr.charAt(i) & 0xff);
			buffer[0x20 + i] = _c;
			buffer[0x3E + i] = _c;
		}

		writeFile(new File(packageOutputDir, uidStr + "_reg.rsc"), buffer); //$NON-NLS-1$
	}

	private void createExe(File exeTemplateFile, File packageOutputDir, String uidStr, DefaultPackager internal) throws IOException {
		File outputFile = new File(packageOutputDir, uidStr + ".exe");	//$NON-NLS-1$
		internal.runCommandLine("%mosync-bin%\\e32hack.exe", exeTemplateFile.getAbsolutePath(),	//$NON-NLS-1$
			outputFile.getAbsolutePath(), uidStr);
	}

	private void createResourceFile(File resourceTemplateFile, File packageOutputDir, String uidStr, String appName) throws IOException {
		if (appName.length() > 62) {
			throw new IOException(Messages.S60Packager_ApplicationNameTooLong);
		}

		byte[] template = readFile(resourceTemplateFile);
		byte v;
		int x;

		int originalAppnameLength = (byte)(template[0x42]);
		if (template[0x42] != template[0x43]) {
			throw new IOException(Messages.S60Packager_InvalidTemplate);
		}

		byte[] buffer = new byte[template.length+2*(appName.length()-originalAppnameLength)];
		System.arraycopy(template, 0, buffer, 0, 0x44);
		v = template[0x11];
		x = v - ((originalAppnameLength + 1) * 4);
		buffer[0x11] = (byte)(x + (appName.length() + 1) * 4);

		buffer[0x42] = (byte)(appName.length() & 0xff);
		buffer[0x43] = (byte)(appName.length() & 0xff);

		int templateLoc = 0x44;
		int bufferLoc = 0x44;

		for (int i = 0; i < appName.length(); i++) {
			buffer[bufferLoc] = (byte)(appName.charAt(i) & 0xff);
			bufferLoc++;
		}

		templateLoc += originalAppnameLength;

		for (int i = 0; i < 9; i++) {
			buffer[bufferLoc++] = template[templateLoc++];
		}

		buffer[bufferLoc++] = (byte)(appName.length() & 0xff);
		buffer[bufferLoc++] = (byte)(appName.length() & 0xff);

		for (int i = 0; i < appName.length(); i++) {
			buffer[bufferLoc] = (byte)(appName.charAt(i) & 0xff);
			bufferLoc++;
		}

		templateLoc += 2 + originalAppnameLength;

		//icon filename and surroundings
		System.arraycopy(template, templateLoc, buffer, bufferLoc, 0x14);
		templateLoc += 0x14;
		bufferLoc += 0x14;
		if (uidStr.length() != 8) {
			throw new IOException("Invalid UID");
		}
		System.arraycopy(uidStr.getBytes(), 0, buffer, bufferLoc, 8);
		templateLoc += 8;
		bufferLoc += 8;
		System.arraycopy(template, templateLoc, buffer, bufferLoc, 9);
		templateLoc += 9;
		bufferLoc += 9;
		
		//read one byte
		v = (byte)(template[templateLoc++]);
		buffer[bufferLoc++] = v;
	
		//copy v+8 bytes
		v += 8;
		System.arraycopy(template, templateLoc, buffer, bufferLoc, v);
		templateLoc += v;
		bufferLoc += v;
	
		//write the last magic word
		v = template[templateLoc];
		x = v - ((originalAppnameLength + 1) * 2);
		buffer[bufferLoc] = (byte)(x + (appName.length() + 1) * 2);
		buffer[bufferLoc + 1] = 0;

		writeFile(new File(packageOutputDir, uidStr + ".rsc"), buffer); //$NON-NLS-1$
	}


}
