package com.mobilesorcery.sdk.html5.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

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
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class JSODDConnectDialog extends ProgressMonitorDialog {

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

	private static final int RUN_IN_BKG = 1 << 16;

	public static void show(final MoSyncProject project,
			final IBuildVariant variant, final boolean onDevice,
			final ReloadVirtualMachine vm) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				Shell shell = PlatformUI.getWorkbench()
						.getModalDialogShellProvider().getShell();
				JSODDConnectDialog dialog = new JSODDConnectDialog(shell,
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
										"Waiting for device to connect...", -1);
								boolean waiting = true;
								while (!monitor.isCanceled() && waiting) {
									Thread.sleep(500);
									waiting = !listener.isStarted();
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
			}
		});
	}

	private MoSyncProject project;
	private IBuildVariant variant;
	private boolean onDevice;
	private ReloadVirtualMachine vm;

	private ArrayList<Image> icons = new ArrayList<Image>();
	private ArrayList<String> messages = new ArrayList<String>();

	public JSODDConnectDialog(Shell shell, MoSyncProject project,
			IBuildVariant variant, boolean onDevice, ReloadVirtualMachine vm) {
		super(shell);
		this.project = project;
		this.variant = variant;
		this.onDevice = onDevice;
		this.vm = vm;
	}

	protected Control createMessageArea(Composite parent) {
		addMessages();

		for (int i = 0; i < icons.size(); i++) {
			Image icon = icons.get(i);
			String message = messages.get(i);
			Label iconLabel = new Label(parent, SWT.NONE);
			iconLabel.setLayoutData(new GridData(SWT.DEFAULT, SWT.TOP, false,
					false));
			if (icon != null) {
				iconLabel.setImage(icon);
			}
			Link messageLabel = new Link(parent, SWT.NONE);
			messageLabel.setLayoutData(new GridData(SWT.DEFAULT, SWT.TOP, true,
					false));
			messageLabel.setText(message);
			messageLabel.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					performAction(event.text);
				}
			});

			Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true,
					false, 2, 1));
		}

		// Important addition:
		super.createMessageArea(parent);

		return parent;
	}

	public Image getImage() {
		return PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, RUN_IN_BKG, "Wait in Background", true);
		createCancelButton(parent);
	}
	
	protected void configureShell(final Shell shell) {
		super.configureShell(shell);
		// We don't want that wait cursor.
		shell.setCursor(arrowCursor);
		shell.setText("Waiting for device");
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
				PlatformUI.getWorkbench().getSharedImages()
						.getImage(ISharedImages.IMG_ELCL_SYNCED),
				connectMessage);

		// IOS?
		boolean ios = variant.getProfile().getPackager().getId()
				.equals("com.mobilesorcery.sdk.build.ios.packager");
		if (ios /* && onDevice */) {
			addMessage(
					MosyncUIPlugin.getDefault().getImageRegistry()
							.get(MosyncUIPlugin.IMG_BUILD_ONE),
					"Install and start your iOS on device using iTunes.\nThe package to install is <a href=\"package\">here</a>.");
		}
	}

	protected void addMessage(Image icon, String message) {
		this.icons.add(icon);
		this.messages.add(message);
	}

}
