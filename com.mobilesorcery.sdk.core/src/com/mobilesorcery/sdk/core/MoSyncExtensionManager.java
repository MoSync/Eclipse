package com.mobilesorcery.sdk.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

public class MoSyncExtensionManager {

	private static final MoSyncExtensionManager INSTANCE = new MoSyncExtensionManager();
	private HashMap<String, MoSyncExtension> extensions = new HashMap<String, MoSyncExtension>();

	private MoSyncExtensionManager() {
		refresh();
	}

	private void refresh() {
		IPath extensionsPath = MoSyncTool.getDefault().getMoSyncExtensions();
		File[] extensionPaths = extensionsPath.toFile().listFiles();
		for (File extensionPath : extensionPaths) {
			MoSyncExtension extension = MoSyncExtension.findExtension(extensionPath.getName());
			if (extension != null) {
				String name = extension.getName();
				extensions.put(name, extension);
			}
		}
	}

	public static MoSyncExtensionManager getDefault() {
		return INSTANCE;
	}

	public MoSyncExtension getExtension(String extensionName) {
		return extensions.get(extensionName);
	}
	
	/**
	 * Installs an extension into MoSync
	 * @param extension The extension file, either a directory or a zip file
	 * @return
	 * @throws IOException
	 */
	public MoSyncExtension install(File extension) throws IOException {
		if (!extension.exists()) {
			throw new IOException("Extension does not exist at " + extension.getAbsolutePath());
		}
		
		String extensionInstallName = Util.getNameWithoutExtension(extension.getName());

		IPath installLocation = MoSyncTool.getDefault()
				.getMoSyncExtensions().append(new Path(extensionInstallName));

		if (extension.isDirectory()) {
			Util.deleteFiles(installLocation.toFile(), null, 3, new NullProgressMonitor());
			Util.copy(new NullProgressMonitor(), extension,
					installLocation.toFile(), null);
		} else {
			throw new UnsupportedOperationException();
		}
		
		MoSyncExtension result = MoSyncExtension.findExtension(extensionInstallName);
		if (result == null) {
			throw new IOException("Installation of extension failed");
		}
		refresh();
		return result;
	}
}
