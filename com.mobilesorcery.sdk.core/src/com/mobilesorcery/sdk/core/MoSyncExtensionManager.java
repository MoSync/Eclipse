package com.mobilesorcery.sdk.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

public class MoSyncExtensionManager {

	public static class ExtensionAlreadyExistsException extends IOException {

		private MoSyncExtension existing;

		public ExtensionAlreadyExistsException(MoSyncExtension existing) {
			this.existing = existing;
		}
		
		public MoSyncExtension getExisting() {
			return existing;
		}
		
	}
	
	private static final MoSyncExtensionManager INSTANCE = new MoSyncExtensionManager();
	private TreeMap<String, MoSyncExtension> extensions = new TreeMap<String, MoSyncExtension>();

	private MoSyncExtensionManager() {
		refresh();
	}

	public void refresh() {
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
	
	public List<MoSyncExtension> getExtensions() {
		return new ArrayList<MoSyncExtension>(extensions.values());
	}
	
	/**
	 * Installs or updates an extension into MoSync
	 * @param extension The extension file, either a directory or a zip file
	 * @param forceUpdate If {@code true}, any existing extension with the same name will
	 * be overwritten
	 * @return
	 * @throws IOException
	 */
	public MoSyncExtension install(File extension, boolean forceUpdate) throws IOException {
		if (!extension.exists()) {
			throw new IOException("Extension does not exist at " + extension.getAbsolutePath());
		}
		
		String extensionInstallName = Util.getNameWithoutExtension(extension.getName());
		
		if (!forceUpdate && MoSyncExtension.findExtension(extensionInstallName) != null) {
			throw new ExtensionAlreadyExistsException(MoSyncExtension.findExtension(extensionInstallName));
		}
		
		File installLocation = MoSyncTool.getDefault()
				.getMoSyncExtensions().append(new Path(extensionInstallName)).toFile();
		
		MoSyncExtension.validateInExtensionsDir(installLocation);
		removeExtensionDir(installLocation);
		
		if (extension.isDirectory()) {
			validateExtensionToBeInstalled(extension);
			Util.copy(new NullProgressMonitor(), extension,
					installLocation, null);
		} else {
			File tmp = new File(installLocation.getAbsoluteFile() + "~");
			removeExtensionDir(tmp);
			Util.unzip(extension, tmp);
			try {
				validateExtensionToBeInstalled(tmp);
			} catch (Exception e) {
				removeExtensionDir(tmp);
			}
			tmp.renameTo(installLocation);
		}
		
		MoSyncExtension result = MoSyncExtension.findExtension(extensionInstallName);
		if (result == null) {
			throw new IOException("Installation of extension failed");
		}
		refresh();
		return result;
	}

	private void validateExtensionToBeInstalled(File extension) throws IOException {
		MoSyncExtension.validateInstallable(extension);
	}

	public void uninstall(MoSyncExtension extension) throws IOException {
		if (!removeExtensionDir(extension.getExtensionRoot().toFile())) {
			throw new IOException("Unable to install extension " + extension);
		}
		extensions.remove(extension.getName());
	}
	
	private boolean removeExtensionDir(File installLocation) {
		return Util.deleteFiles(installLocation, null, 8, new NullProgressMonitor());
	}

}
