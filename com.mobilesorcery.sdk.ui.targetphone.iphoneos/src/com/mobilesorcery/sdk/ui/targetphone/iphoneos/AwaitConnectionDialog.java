package com.mobilesorcery.sdk.ui.targetphone.iphoneos;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.ProfileDBManager;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.ProgressAndStepDialog;

public class AwaitConnectionDialog extends ProgressAndStepDialog {

	private static final class DialogIPhoneOSOTAServerListener implements
			IPhoneOSOTAServerListener {

		private MoSyncProject project;
		private boolean appRequested;

		public DialogIPhoneOSOTAServerListener(MoSyncProject project) {
			this.project = project;
		}

		public boolean isAppRequested() {
			return appRequested;
		}

		@Override
		public void appRequested(MoSyncProject project) {
			if (project.getName().equals(this.project.getName())) {
				appRequested = true;
			}
		}
	}

	public AwaitConnectionDialog(Shell shell) {
		super(shell);
	}

	public static int show(final MoSyncProject project, final IBuildVariant variant) {
		final int[] result = new int[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				Shell shell = PlatformUI.getWorkbench()
						.getModalDialogShellProvider().getShell();
				final AwaitConnectionDialog dialog = new AwaitConnectionDialog(shell);

				try {
					dialog.run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							DialogIPhoneOSOTAServerListener listener =
									new DialogIPhoneOSOTAServerListener(project);

							try {
								IPhoneOSOTAServer.getDefault().addListener(
										listener);
								monitor.beginTask(
										"Waiting for device to connect...", -1);
								boolean waiting = true;
								while (!monitor.isCanceled() && waiting) {
									Thread.sleep(500);
									waiting = !listener.isAppRequested() && !dialog.shouldWaitInBackground();
								}
							} finally {
								IPhoneOSOTAServer.getDefault().removeListener(
										listener);
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

	protected void addMessages() {
		IVendor vendor = ProfileDBManager.getInstance().getVendor("ios");
		Image vendorImage = MosyncUIPlugin.getDefault().getPlatformImage(vendor, new Point(16, 16));
		Image provImage = IPhoneOSTransportPlugin.getDefault().getImageRegistry().get(IPhoneOSTransportPlugin.PROV_IMAGE);
		try {
			URL url = IPhoneOSTransportPlugin.getDefault().getServerURL();
			addMessage(MosyncUIPlugin.getDefault().getImageRegistry().get(MosyncUIPlugin.IMG_WIFI), "Make sure your device can connect to your computer.\nThe simplest way to do this is often to connect to the same WiFi network.");
			addMessage(provImage, "Make sure that you have created an ad hoc provising profile that matches your device.\nIf not correct, the app will not install.");
			addMessage(vendorImage,
					MessageFormat.format("On your iPhone, go to this URL: {0}", url));
		} catch (IOException e) {
			addMessage(vendorImage, "Invalid server URL");
		}
	}
}
