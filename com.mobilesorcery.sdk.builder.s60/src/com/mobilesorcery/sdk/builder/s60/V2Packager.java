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

//import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.templates.Template;
import com.mobilesorcery.sdk.profiles.IProfile;

public class V2Packager extends S60Packager {

	private static final int V2_MAGIC2 = 0x1f - 4;
	private static final int V2_SIZE1 = 4;
	private static final int V2_MAGIC1 = 4;

	public V2Packager() {
	}

	public void createPackage(MoSyncProject project, IBuildVariant variant, IBuildResult buildResult) throws CoreException {
		DefaultPackager internal = new DefaultPackager(project, variant);
		internal.setParameter("D", shouldUseDebugRuntimes() ? "D" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		try {
			File packageOutputDir = new File(internal.resolve("%package-output-dir%")); //$NON-NLS-1$
			packageOutputDir.mkdirs();

			String uid = project.getProperty(PropertyInitializer.S60V2_UID);
			if (uid.startsWith("0x")) { //$NON-NLS-1$
				uid = uid.substring(2);
			}

			//int iUid = Integer.parseInt(uid, 16);

			String appname = project.getName();

			File runtimeDir = new File(internal.resolve("%runtime-dir%")); //$NON-NLS-1$
			String runtimePath = internal.resolve("%runtime-dir%\\Mosync%D%.app"); //$NON-NLS-1$

			// Hack away...
			try {
				createAppFile(new File(runtimePath), packageOutputDir, uid);
				createCaptionResourceFile(new File(runtimeDir, "MoSync_caption.rsc"), packageOutputDir, uid, appname); //$NON-NLS-1$
			} catch (RuntimeException e) {
				throw new IOException("Invalid runtime(s)", e);
			}

			Util.copyFile(new NullProgressMonitor(), new File(runtimeDir, "MoSync.rsc"), new File(packageOutputDir, uid + ".rsc")); //$NON-NLS-1$ //$NON-NLS-2$
			
			// Create the launcherapp.pkg
			String template = Util.readFile(runtimeDir.getAbsolutePath() + "/MoSync-template.pkg"); //$NON-NLS-1$
			internal.setParameter("uid", uid); //$NON-NLS-1$
			internal.setParameter("vendor-name", "MOBILE SORCERY"); //$NON-NLS-1$ //$NON-NLS-2$
			String resolvedTemplate = Template.preprocess(template, internal.getParameters().toMap());
			File pkgFile = new File(packageOutputDir, uid + ".pkg"); //$NON-NLS-1$
			Util.writeToFile(pkgFile, resolvedTemplate);

			//internal.getExecutor().setExecutionDirectory(packageOutputDir.getAbsolutePath());
			internal.runCommandLine("%mosync-bin%\\makesis-200.exe", pkgFile.getAbsolutePath()); //$NON-NLS-1$

			// Rename
			File unsignedSis = new File(packageOutputDir, uid + ".sis"); //$NON-NLS-1$
			File renamedAppSis = new File(packageOutputDir, project.getName() + ".sis"); //$NON-NLS-1$
			Util.copyFile(new NullProgressMonitor(), unsignedSis, renamedAppSis);
			unsignedSis.delete();

			buildResult.setBuildResult(renamedAppSis);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, "com.mobilesorcery.builder.s60", Messages.V2Packager_PackageError, e)); //$NON-NLS-1$
		}
	}

	private void createAppFile(File appTemplateFile, File packageOutputDir, String uidStr) throws IOException {
		byte[] buffer = readFile(appTemplateFile);

		int uid = Integer.parseInt(uidStr, 16);
		int oldUid = readInt(buffer, 8);

		//uids with crc
		writeInt(uid, buffer, 8);
		int uidChecksum = CRC16.getChecksumForUID(buffer);
		writeInt(uidChecksum, buffer, 12);

		/*//UTF-16 uid
		for (int i = 0; i < uidStr.length(); i++) {
			buffer[0x344 + 2 * i] = (byte)(uidStr.charAt(i) & 0xff);
			buffer[0x345 + 2 * i] = 0x00;
		}*/

		//uid in code: find & change
		for (int i = 0x20/*V3Packager.V3_EXE_HEADER_SIZE*/; i < buffer.length; i += 4) {
			if (readInt(buffer, i) == oldUid) {
				//printf("found uid @ 0x%x\n", HEADER_SIZE + i * 4);
				writeInt(uid, buffer, i);
			}
		}

		//code section checksum
		int codeOffset = readInt(buffer, 100);
		int codeSize = readInt(buffer, 48);
		int checksum = codeChecksum(buffer, codeOffset, codeSize);
		writeInt(checksum, buffer, 24);

		writeFile(new File(packageOutputDir, uidStr + ".app"), buffer); //$NON-NLS-1$
	}

	private void createCaptionResourceFile(File captionTemplateFile, File packageOutputDir, String uidStr, String appName) throws IOException {
		if (appName.length() > 62) {
			throw new IOException(Messages.S60Packager_ApplicationNameTooLong);
		}

		byte[] template = readFile(captionTemplateFile);

		int originalAppnameLength = (byte)(template[0x16]);
		if (template[0x16] != template[0x17]) {
			throw new IOException(Messages.S60Packager_InvalidTemplate);
		}

		byte[] buffer = new byte[template.length+2*(appName.length()-originalAppnameLength)];
		System.arraycopy(template, 0, buffer, 0, 0x16);
		buffer[0x11] = (byte)(V2_MAGIC1 + appName.length() * 4);

		buffer[0x16] = (byte)(appName.length() & 0xff);
		buffer[0x17] = (byte)(appName.length() & 0xff);

		int templateLoc = 0x18;
		int bufferLoc = 0x18;

		for (int i = 0; i < appName.length(); i++) {
			buffer[bufferLoc] = (byte)(appName.charAt(i) & 0xff);
			bufferLoc++;
		}

		templateLoc += originalAppnameLength;

		buffer[bufferLoc++] = template[templateLoc++];
		
		buffer[bufferLoc++] = (byte)(appName.length() & 0xff);
		buffer[bufferLoc++] = (byte)(appName.length() & 0xff);

		for (int i = 0; i < appName.length(); i++) {
			buffer[bufferLoc] = (byte)(appName.charAt(i) & 0xff);
			bufferLoc++;
		}

		templateLoc += 2 + originalAppnameLength;	
		
		//System.arraycopy(template, templateLoc, buffer, bufferLoc, V2_SIZE1);
		for(int i = 0; i < V2_SIZE1; i++) {
			//System.out.println("byte " + i + ": " + template[templateLoc+i]);
			buffer[bufferLoc+i] = template[templateLoc+i];
		}
		
		bufferLoc += V2_SIZE1 - 2;
		templateLoc += V2_SIZE1;
		
		buffer[bufferLoc] = (byte)(V2_MAGIC2 + 2 * appName.length());
		
		/*
		for(int i = 0; i < buffer.length; i++) {	
			String val = "0" + Integer.toHexString(((int)buffer[i]));
			System.out.print(val.substring(val.length()-2, val.length()) + " ");
			if((i&0x7) == 0x7) System.out.print("\n");
		}
		*/
		
		writeFile(new File(packageOutputDir, uidStr + "_caption.rsc"), buffer); //$NON-NLS-1$
	}

	private int codeChecksum(byte[] buffer, int offset, int length) {
		long checksum = 0;
		for (int i = 0; i < length / 4; i++) {
			checksum += readInt(buffer, offset + 4 * i);
		}

		return (int)checksum & 0xffffffff;
	}

}
