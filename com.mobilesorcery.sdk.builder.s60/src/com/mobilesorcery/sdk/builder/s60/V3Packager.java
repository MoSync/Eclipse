package com.mobilesorcery.sdk.builder.s60;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.templates.Template;
import com.mobilesorcery.sdk.profiles.IProfile;

public class V3Packager extends S60Packager {

	private final static String TEMPLATE_UID = "E1223344"; //$NON-NLS-1$

	private static final int V3_MAGIC1 = 0x5B;
	private static final int V3_MAGIC2 = 0x78;
	private static final int V3_SIZE1 = 0x33;
	static final int V3_EXE_HEADER_SIZE = 0x9c;

	public V3Packager() {
	}

	public void createPackage(MoSyncProject project, IProfile targetProfile, IBuildResult buildResult) throws CoreException {
		DefaultPackager internal = new DefaultPackager(project, targetProfile, isFinalizerBuild());
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
			String vendorName = "Mobile Sorcery"; //$NON-NLS-1$

			try {
				createExe(new File(runtimePath), packageOutputDir, uid);
				createRegRsc(new File(runtimeDir, "MoSync_reg.RSC"), packageOutputDir, uid); //$NON-NLS-1$
				createResourceFile(new File(runtimeDir, "MoSync.RSC"), packageOutputDir, uid, appname); //$NON-NLS-1$
			} catch (RuntimeException e) {
				throw new IOException("Invalid runtime(s)", e);
			}
			
			Template template = new Template(getClass().getResource("/templates/mosyncapp.pkg.v3.template")); //$NON-NLS-1$
			internal.setParameter("uid", uid); //$NON-NLS-1$
			internal.setParameter("vendor-name", "MOBILE SORCERY"); //$NON-NLS-1$ //$NON-NLS-2$
			String resolvedTemplate = template.resolve(internal.getParameters().toMap());
			File pkgFile = new File(packageOutputDir, uid + ".pkg"); //$NON-NLS-1$
			Util.writeToFile(pkgFile, resolvedTemplate);

			internal.runCommandLine("%mosync-bin%\\makesis-4.exe", pkgFile.getAbsolutePath()); //$NON-NLS-1$

			File unsignedSis = new File(packageOutputDir, uid + ".sis"); //$NON-NLS-1$
			File renamedAppSis = new File(packageOutputDir, project.getName() + ".sis"); //$NON-NLS-1$
			internal.runCommandLine("%mosync-bin%\\signsis-4.exe", //$NON-NLS-1$
							unsignedSis.getAbsolutePath(),
							renamedAppSis.getAbsolutePath(),
							project.getProperty(PropertyInitializer.S60_CERT_FILE),
							project.getProperty(PropertyInitializer.S60_KEY_FILE),
							project.getProperty(PropertyInitializer.S60_PASS_KEY));

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

	private void createExe(File exeTemplateFile, File packageOutputDir, String uidStr) throws IOException {
		byte[] buffer = readFile(exeTemplateFile);

		int uid = (int)Long.parseLong(uidStr, 16);
		int oldUid = readInt(buffer, 8);

		//uids with crc
		writeInt(uid, buffer, 8);
		int uidChecksum = CRC16.getChecksumForUID(buffer);
		writeInt(uidChecksum, buffer, 12);

		//"secure" uid
		writeInt(uid, buffer, 0x80);

		//header crc
		int headerCRC = 0xc90fdaa2;	//magic number from Symbian's f32image.h
		writeInt(headerCRC, buffer, 0x14);

		CRC32 crc32 = new CRC32();
		crc32.update(buffer, 0, V3_EXE_HEADER_SIZE);
		int newCRC = crc32.getChecksum();
		writeInt(newCRC, buffer, 0x14);

		/*//UTF-16 uid
		for (int i = 0; i < uidStr.length(); i++) {
			buffer[0x126c + 2 * i] = (byte)(uidStr.charAt(i) & 0xff);
			buffer[0x126d + 2 * i] = 0x00;
		}*/

		//uid in code: find & change
		for (int i = V3_EXE_HEADER_SIZE; i < buffer.length; i += 4) {
			if (readInt(buffer, i) == oldUid) {
				//printf("found uid @ 0x%x\n", HEADER_SIZE + i * 4);
				writeInt(uid, buffer, i);
			}
		}

		writeFile(new File(packageOutputDir, uidStr + ".exe"), buffer); //$NON-NLS-1$
	}

	private void createResourceFile(File resourceTemplateFile, File packageOutputDir, String uidStr, String appName) throws IOException {
		if (appName.length() > 62) {
			throw new IOException(Messages.S60Packager_ApplicationNameTooLong);
		}

		byte[] template = readFile(resourceTemplateFile);

		int originalAppnameLength = (byte)(template[0x42]);
		if (template[0x42] != template[0x43]) {
			throw new IOException(Messages.S60Packager_InvalidTemplate);
		}

		byte[] buffer = new byte[template.length+2*(appName.length()-originalAppnameLength)];
		System.arraycopy(template, 0, buffer, 0, 0x44);
		buffer[0x11] = (byte)(V3_MAGIC1 + (appName.length() + 1) * 4);

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

		templateLoc += originalAppnameLength + 2;

		System.arraycopy(template, templateLoc, buffer, bufferLoc, V3_SIZE1);

		templateLoc += V3_SIZE1;
		bufferLoc += V3_SIZE1;// - 2;

		buffer[bufferLoc] = (byte)(V3_MAGIC2 + 2 * (appName.length() + 1));
		buffer[bufferLoc + 1] = 0;

		//System.err.println(Util.toBase16(template));
		//System.err.println(Util.toBase16(buffer));
		//System.err.println(Util.toBase16(readFile(new File("C:\\development\\tools\\MoSync\\examples\\MoTris\\Output\\E4E046D4.rsc"))));

		writeFile(new File(packageOutputDir, uidStr + ".rsc"), buffer); //$NON-NLS-1$
	}


}
