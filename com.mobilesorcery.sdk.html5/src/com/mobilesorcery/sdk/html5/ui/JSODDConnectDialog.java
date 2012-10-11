package com.mobilesorcery.sdk.html5.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.print.attribute.standard.MediaSize.ISO;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.live.ILiveServerListener;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.ProgressAndStepDialog;

public class JSODDConnectDialog extends ProgressAndStepDialog {

	private static final class LiveServerListener implements
			ILiveServerListener {

		private MoSyncProject project;
		private IBuildVariant variant;
		private boolean started;

		public LiveServerListener(MoSyncProject project, IBuildVariant variant) {
			this.project = project;
			this.variant = variant;
		}

		public boolean isStarted() {
			return started;
		}

		@Override
		public void timeout(ReloadVirtualMachine vm) {
			// None of our business
		}

		@Override
		public void inited(ReloadVirtualMachine vm, boolean reset) {
			if (Util.equals(vm.getProject(), project.getWrappedProject())) {
				started = true;
			}
		}
	}

	public static int show(final MoSyncProject project,
			final IBuildVariant variant, final boolean onDevice,
			final ReloadVirtualMachine vm) {
		final int[] result = new int[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				Shell shell = PlatformUI.getWorkbench()
						.getModalDialogShellProvider().getShell();
				final JSODDConnectDialog dialog = new JSODDConnectDialog(shell,
						project, variant, onDevice, vm);
				final LiveServerListener listener = new LiveServerListener(
						project, variant);
				try {
					dialog.run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								Html5Plugin.getDefault().getReloadServer()
										.addListener(listener);
								monitor.beginTask(
										MessageFormat.format("Waiting for {0} to connect...", dialog.getTargetType()), -1);
								boolean waiting = true;
								while (!monitor.isCanceled() && waiting) {
									Thread.sleep(500);
									waiting = !listener.isStarted() && !dialog.shouldWaitInBackground();
								}
							} finally {
								Html5Plugin.getDefault().getReloadServer()
										.removeListener(listener);
							}
						}
					});
				} catch (Exception e) {
					dialog.close();
				}
				result[0] = dialog.getReturnCode();
			}
		});
		
		return result[0];
	}

	private MoSyncProject project;
	private IBuildVariant variant;
	private boolean onDevice;
	private ReloadVirtualMachine vm;

	public JSODDConnectDialog(Shell shell, MoSyncProject project,
			IBuildVariant variant, boolean onDevice, ReloadVirtualMachine vm) {
		super(shell);
		this.project = project;
		this.variant = variant;
		this.onDevice = onDevice;
		this.vm = vm;
	}
	
	protected void configureShell(final Shell shell) {
		super.configureShell(shell);
		shell.setText(MessageFormat.format("Waiting for {0}", getTargetType()));
	}

	protected void performAction(String actionId) {
		if ("package".equals(actionId)) {
			List<File> output = project.getBuildState(variant).getBuildResult()
					.getBuildResult().get(IBuildResult.MAIN);
			if (output != null && !output.isEmpty()) {
				Program.launch(output.get(0).getParent());
			}
		}

	}

	protected void addMessages() {
		if (!onDevice) {
			return;
		}
		String connectMessage = "Make sure your device can connect to the debug server.";
		if (Html5Plugin.getDefault().useDefaultServerURL()) {
			connectMessage += "\nThe simplest way to do this is often to connect to the same WiFi network.";
		}
		addMessage(
				MosyncUIPlugin.getDefault().getImageRegistry().get(MosyncUIPlugin.IMG_WIFI),
				connectMessage);

		// IOS?
		boolean ios = isIOS(variant.getProfile());
		if (ios && onDevice) {
			addMessage(
					MosyncUIPlugin.getDefault().getImageRegistry()
							.get(MosyncUIPlugin.IMG_BUILD_ONE),
					"Start the {0} app on your iOS device.\n(If you need to install the package manually, you can find it <a href=\"package\">here</a>.)", project.getName());
		}
	}
	
	private String getTargetType() {
		if (onDevice) {
			return "device";
		} else if (isIOS(variant.getProfile())) {
			return "simulator";
		} else {
			return "emulator";
		}
		
	}
	
	private static boolean isIOS(IProfile profile) {
		 return profile != null && profile.getPackager().getId()
			.equals("com.mobilesorcery.sdk.build.ios.packager"); 
	}

}
