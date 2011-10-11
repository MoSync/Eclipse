package com.mobilesorcery.sdk.ui.internal;

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Util;

public class MemoryLowDialog extends IconAndMessageDialog {

	private static boolean isOpen;

	public MemoryLowDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("MEMORY LOW!");
	}

	@Override
	public Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		initMessage();
		createMessageArea(contents);
		return contents;
	}

	private void initMessage() {
		String installDir = getInstallDir();
		String memoryStats = getMemoryStats();
		String installDirStr = installDir == null ? "" : " (" + installDir + ")";
		message = MessageFormat.format("LOW MEMORY!\n" +
				"This may cause the MoSync IDE to crash or freeze!\n\n" +
				"To remedy this, increase the memory allocated the the JVM. These settings are found in the mosync.ini file in the installation directory{0}.\n\n" +
				"   -XmxSIZE - Change the heap size\n" +
				"   -XX:MaxPermSize=SIZE - Change the perm gen size\n\n" +
				"Example (they must be on separate lines in the mosync.ini file):\n\n" +
				"   -Xmx1024m\n" +
				"   -XX:MaxPermSize=256m\n\n" +
				"(NOTE: The above settings may be JVM specific; they work for Sun JVMs)\n\n" +
				"Details:\n" +
				"{1}\n(* denotes that the memory is running out)", installDirStr, memoryStats);
	}

	private String getInstallDir() {
		Location installLocation = Platform.getInstallLocation();
		String installDir = null;
		if (installLocation != null) {
			URL installURL = installLocation.getURL();
			if (installURL != null) {
				installDir = installURL.getFile();
			}
		}
		return installDir;
	}

	private String getMemoryStats() {
		StringBuffer result = new StringBuffer();
		List<MemoryPoolMXBean> monitoredPools = CoreMoSyncPlugin.getLowMemoryManager().getMonitoredPools();
		for (MemoryPoolMXBean monitoredPool : monitoredPools) {
			boolean exceeded = monitoredPool.isUsageThresholdSupported() && monitoredPool.isUsageThresholdExceeded();
			String exceededStr = exceeded ? "*" : "";
			MemoryUsage usage = monitoredPool.getUsage();
			String used = usage == null ? "?" : Util.dataSize(usage.getUsed());
			String max = usage == null ? "?" : Util.dataSize(usage.getMax());
			String stats = MessageFormat.format("{0}{1} - Used: {2}, Available: {3}",
					exceededStr, monitoredPool.getName(), used, max);
			result.append(stats);
			result.append('\n');
		}
		return result.toString();
	}

	@Override
	public void createButtonsForButtonBar(Composite parent) {
    	createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected Image getImage() {
		return getWarningImage();
	}

	public static synchronized void open(Display display) {
		if (isOpen) {
			return;
		}
		isOpen = true;
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				IShellProvider sp = PlatformUI.getWorkbench().getModalDialogShellProvider();
				Shell shell = sp.getShell();
				MemoryLowDialog dialog = new MemoryLowDialog(shell);
				dialog.open();
				isOpen = false;
			}
		});
	}
}
