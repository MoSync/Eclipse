package com.mobilesorcery.sdk.update.internal;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.util.Policy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.intro.IIntroPart;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IUpdater;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.stats.Variables;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.update.MosyncUpdatePlugin;
import com.mobilesorcery.sdk.update.UpdateManager;
import com.mobilesorcery.sdk.update.UpdateManagerBase;

/**
 * An updater for the 'new' update process; 'new' is obviously new only for a
 * certain period of time, so if you're reading this javadoc it's probably older
 * than the flashy new-and-fresh-car feeling conveyed by the 'new' word.
 *
 * @author Mattias Bybro, mattias.bybro@purplescout.s
 *
 */
public class DefaultUpdater2 extends UpdateManagerBase implements IUpdater {

	public class InternalLocationListener implements LocationListener {

		private final IViewPart view;

		public InternalLocationListener(IViewPart view) {
			this.view = view;
		}

		@Override
		public void changed(LocationEvent event) {
		}

		@Override
		public void changing(LocationEvent event) {
			String location = event.location;
			// Specially formatted urls will kill the editor.
			try {
				URL locationURL = new URL(location);
				boolean isKillURL = locationURL.getPath().contains(
						"close-ide-registration");
				if (isKillURL) {
					view.getSite().getWorkbenchWindow().getActivePage()
							.hideView(view);
				}
			} catch (MalformedURLException e) {
				// Just ignore.
			}
		}

	}

	public static final String SHOW_CONNECTION_FAILED_POPUP = "show.conn.fail.popup";

	public class OpenBrowserRunnable implements Runnable {

		private final URL whereToGo;
		private final String name;
		private boolean reopenIntro;

		public OpenBrowserRunnable(URL whereToGo, String name) {
			this.whereToGo = whereToGo;
			this.name = name;
		}

		@Override
		public void run() {
			try {
				// Since we implicitly open a perspective we do not want
				// the "re-open welcome screen" flag cleared; hence this
				// listener deactivation.
				perspectiveListener.setActive(false);
				IWorkbenchWindow window = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow();
				IIntroPart currentIntroPart = PlatformUI.getWorkbench()
						.getIntroManager().getIntro();
				reopenIntro = currentIntroPart != null;

				if (reopenIntro) {
					PlatformUI.getWorkbench().getIntroManager()
							.closeIntro(currentIntroPart);
				}

				try {
					IWorkbench wb = PlatformUI.getWorkbench();
					IWorkbenchPage page = wb.showPerspective(RegistrationPerspectiveFactory.REGISTRATION_PERSPECTIVE_ID,
									window);
				} catch (WorkbenchException e) {
					// We can still do SOMETHING.
					CoreMoSyncPlugin.getDefault().log(e);
				}

				IViewPart view = RegistrationWebBrowserView.open(whereToGo,
						reopenIntro);
				Browser browser = RegistrationWebBrowserView.getBrowser(view);
				if (browser != null) {
					InternalLocationListener locationListener = new InternalLocationListener(
							view);
					browser.addLocationListener(locationListener);
				}
			} catch (PartInitException e) {
				e.printStackTrace();
				openFallbackDialog(whereToGo, name);
			} finally {
				perspectiveListener.setActive(true);
			}
		}

	}

	class UpdateJob extends Job {

		public UpdateJob() {
			super("Updating Profile Database");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				UpdateManager.getDefault().downloadProfileUpdate(monitor);
				if (showConfirmDialog()) {
					UpdateManager.getDefault().runUpdater(
							new NullProgressMonitor());
				}
			} catch (Exception e) {
				return new Status(IStatus.ERROR, MosyncUpdatePlugin.PLUGIN_ID,
						"Could not download updated profile database", e);
			}
			return null;
		}

		private boolean showConfirmDialog() {
			final Display d = PlatformUI.getWorkbench().getDisplay();
			final boolean[] result = new boolean[1];
			d.syncExec(new Runnable() {
				@Override
				public void run() {
					Shell shell = new Shell(d);
					result[0] = MessageDialog
							.openConfirm(
									shell,
									"Restart?",
									"A new profile database has been downloaded. To activate the new profiles, the IDE must be restarted");
					shell.dispose();
				}
			});

			return result[0];
		}
	}

	class UpdateRunnable implements Runnable {
		private final boolean isStartedByUser;
		private final boolean registrationOnly;

		public UpdateRunnable(boolean isStartedByUser, boolean registrationOnly) {
			this.isStartedByUser = isStartedByUser;
			this.registrationOnly = registrationOnly;
		}

		@Override
		public void run() {
			UIUtils.awaitWorkbenchStartup(null);
			String userKey = MoSyncTool.getDefault().getProperty(
					MoSyncTool.USER_HASH_PROP_2);

			try {
				boolean requestNewKey = Util.isEmpty(userKey);
				if (!Util.isEmpty(userKey)) {
					boolean isValidKey = validateKey();
					if (!isValidKey) {
						MoSyncTool.getDefault().setProperty(
								MoSyncTool.USER_HASH_PROP_2, null);
						showInvalidKeyWarning();
					}

					requestNewKey |= !isValidKey;
				}

				if (requestNewKey) {
					userKey = requestKeyFromServer();
					MoSyncTool.getDefault().setProperty(
							MoSyncTool.USER_HASH_PROP_2, userKey);
				}

				int userStatus = getUserStatus();
				switch (userStatus) {
				case USER_NOT_CONFIRMED:
					userNotConfirmedAction();
					break;
				case USER_NOT_REGISTERED:
					userNotRegisteredAction();
					break;
				case USER_ACTIVATED:
					if (registrationOnly) {
						userAlreadyRegisteredAction();
					} else if (isStartedByUser || shouldPerformAutoUpdate()) {
						performUpdateAction(isStartedByUser);
					}
				}
			} catch (IOException e) {
				if (isStartedByUser || shouldPopupConnectionFailedMessage()) {
					popupConnectionFailedDialog(isStartedByUser);
				}
			}
		}
	}

	private final RegistrationPartListener perspectiveListener = new RegistrationPartListener(
			null, false);

	private WindowListener windowListener;

	private final class WindowListener implements IWindowListener {
		@Override
		public void windowOpened(IWorkbenchWindow window) {
		}

		@Override
		public void windowDeactivated(IWorkbenchWindow window) {
			detachPerspectiveListener(window);
		}

		@Override
		public void windowClosed(IWorkbenchWindow window) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowActivated(IWorkbenchWindow window) {
			attachPerspectiveListener(window);
		}
	}

	public boolean shouldPopupConnectionFailedMessage() {
		return MessageDialogWithToggle.ALWAYS.equals(MosyncUpdatePlugin
				.getDefault().getPreferenceStore()
				.getString(SHOW_CONNECTION_FAILED_POPUP));
	}

	public void showInvalidKeyWarning() {
		final Display d = PlatformUI.getWorkbench().getDisplay();
		d.asyncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				MessageDialog.openWarning(shell, "Invalid user key",
						"Your registration key is invalid.\n"
								+ "Please re-register.");
			}
		});
	}

	public void shouldPopupConnectionFailedMessage(
			boolean shouldPopupConnectionFailedMessage) {
		// Why doesn't the dialog do this properly - investigate later, for now
		// just make it work.
		MosyncUpdatePlugin
				.getDefault()
				.getPreferenceStore()
				.setValue(
						SHOW_CONNECTION_FAILED_POPUP,
						shouldPopupConnectionFailedMessage ? MessageDialogWithToggle.ALWAYS
								: MessageDialogWithToggle.NEVER);
	}

	public DefaultUpdater2() {
		initPerspectiveListener();
	}

	@Override
	public void dispose() {
		PlatformUI.getWorkbench().removeWindowListener(windowListener);
	}

	public void initPerspectiveListener() {
		windowListener = new WindowListener();
		PlatformUI.getWorkbench().addWindowListener(windowListener);
	}

	protected void attachPerspectiveListener(IWorkbenchWindow window) {
		window.addPerspectiveListener(perspectiveListener);
	}

	protected void detachPerspectiveListener(IWorkbenchWindow window) {
		window.removePerspectiveListener(perspectiveListener);
	}

	public void closeRegistrationPerspective() {
		perspectiveListener.closeRegistrationPerspective();
	}

	@Override
	public void update(boolean isStartedByUser) {
		startUpdateRunnable(isStartedByUser, false);
	}

	@Override
	public void register(boolean isStartedByUser) {
		startUpdateRunnable(isStartedByUser, true);
	}

	private void startUpdateRunnable(boolean isStartedByUser,
			boolean registerOnly) {
		if (isStartedByUser
				|| !MosyncUIPlugin.getDefault().isExampleWorkspace()) {
			UpdateRunnable updater = new UpdateRunnable(isStartedByUser,
					registerOnly);
			Thread updateThread = new Thread(updater,
					"Registration and/or update");
			updateThread.start();
		}
	}

	private void performUpdateAction(boolean isStartedByUser)
			throws IOException {
		try {
			if (UpdateManager.getDefault().isUpdateAvailable()) {
				// MOSYNC-1548: Do not perform automatic updates;
				// show a dialog instead.
				/*
				 * Old code, for reference: UpdateJob updateJob = new
				 * UpdateJob(); updateJob.setUser(isStartedByUser);
				 * updateJob.schedule();
				 */
				showUpdatesDialog(true);
			} else if (isStartedByUser) {
				showUpdatesDialog(false);
			}
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw (IOException) e;
			} else {
				throw new IOException("Could not connect", e);
			}
		}
	}

	private void showUpdatesDialog(final boolean thereAreUpdates) {
		final Display d = PlatformUI.getWorkbench().getDisplay();
		d.asyncExec(new Runnable() {
			@Override
			public void run() {
				innerShowUpdatesDialog(d, thereAreUpdates);
			}
		});
	}

	private void innerShowUpdatesDialog(Display d, boolean thereAreUpdates) {
		try {
			Shell shell = new Shell(d);
			if (thereAreUpdates) {
				MessageDialog dialog = new MessageDialog(shell,
						"There are updates", null,
						"There is a new version of MoSync available",
						MessageDialog.INFORMATION, new String[] {
								"Go To Download Site",
								IDialogConstants.OK_LABEL }, 1);
				if (dialog.open() == 0) {
					PlatformUI.getWorkbench().getBrowserSupport()
							.getExternalBrowser()
							.openURL(getRequestURL("download", null));
				}
			} else {
				MessageDialog.openInformation(shell, "No updates",
						"No updates found");
			}
			shell.dispose();
		} catch (Exception e) {
			Policy.getStatusHandler()
					.show(new Status(IStatus.ERROR, "Internal error",
							e.getMessage()), "");
		}
	}

	public void popupConnectionFailedDialog(final boolean isStartedByUser) {
		final Display d = PlatformUI.getWorkbench().getDisplay();
		d.asyncExec(new Runnable() {
			@Override
			public void run() {
				popupConnectionFailedDialogSync(d, isStartedByUser);
			}
		});
	}

	private void popupConnectionFailedDialogSync(Display d,
			boolean isStartedByUser) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();
		// Shell shell = new Shell(d);
		String dialogTitle = "Could not connect";
		String msg = "MoSync was unable to connect to its update server.\n"
				+ "If you would like the latest device profiles and updates you will need to be connected to the Internet.\n"
				+ "Check your Internet connection and your firewall settings.";

		try {
			if (isStartedByUser) {
				MessageDialog.openInformation(shell, dialogTitle, msg);
			} else {
				MessageDialogWithToggle dialog = MessageDialogWithToggle.open(
						MessageDialogWithToggle.INFORMATION, shell,
						dialogTitle, msg,
						"Show this message if no connection is found",
						shouldPopupConnectionFailedMessage(),
						MosyncUpdatePlugin.getDefault().getPreferenceStore(),
						SHOW_CONNECTION_FAILED_POPUP, SWT.SHELL_TRIM);
				if (dialog.getReturnCode() != -1) {
					shouldPopupConnectionFailedMessage(dialog.getToggleState());
				}
			}
		} finally {
			// shell.dispose();
		}
	}

	private void userNotRegisteredAction() throws IOException {
		launchInternalBrowser(getInitialRegistrationRequestURL(), "Register");
	}

	private URL getInitialRegistrationRequestURL() throws MalformedURLException {
		return getRequestURL("registration/register/", MosyncUIPlugin
				.getDefault().getVersionParameters(true));
	}

	private void userNotConfirmedAction() throws IOException {
		launchInternalBrowser(
				getRequestURL("registration/confirmation/", MosyncUIPlugin
						.getDefault().getVersionParameters(true)),
				"Confirm Registration");
	}

	private void userAlreadyRegisteredAction() throws IOException {
		// The web server handles this in the same way if the user is NOT
		// registered.
		userNotRegisteredAction();
	}

	private void launchInternalBrowser(URL whereToGo, String name) {
		PlatformUI.getWorkbench().getDisplay()
				.syncExec(new OpenBrowserRunnable(whereToGo, name));
	}

	private void openFallbackDialog(URL whereToGo, String name) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();
		String message = MessageFormat.format(
				"Unable to launch browser. To {0}, enter this URL"
						+ "into a web browser: {1}", name, whereToGo);
		MessageDialog dialog = new MessageDialog(shell,
				"Unable to launch browser", null, message, MessageDialog.ERROR,
				new String[] { "Copy web URL to Clipboard", "Ok" }, 1);
		int result = dialog.open();
		if (result == 0 /* copy to clipboard */) {
			Clipboard clipboard = new Clipboard(shell.getDisplay());
			try {
				clipboard.setContents(
						new Object[] { whereToGo.toExternalForm() },
						new Transfer[] { TextTransfer.getInstance() });
			} finally {
				clipboard.dispose();
			}
		}
	}

	public final static int USER_NOT_CONFIRMED = 0;

	public final static int USER_ACTIVATED = 1;

	public final static int USER_NOT_REGISTERED = 2;

	private static final int MAX_KEY_LENGTH = 8192;

	public String requestKeyFromServer() throws IOException {
		Response response = sendRequest(getRequestURL(
				"registration/request/key", null));
		byte[] buffer = new byte[MAX_KEY_LENGTH];
		try {
			InputStream input = response.getContent();
			int len = input.read(buffer);
			return new String(buffer, 0, len);
		} finally {
			response.close();
		}
	}

	public boolean validateKey() throws IOException {
		Response validated = sendRequest(getRequestURL(
				"registration/request/validate", MosyncUIPlugin.getDefault()
						.getVersionParameters(false)));
		boolean isValid = getBooleanResponse(validated,
				"Server failed to accept user key validation request");
		return isValid;
	}

	public int getUserStatus() throws IOException {
		Response response = sendRequest(getRequestURL(
				"registration/request/userstatus", MosyncUIPlugin.getDefault()
						.getVersionParameters(false)));
		InputStream input = null;
		try {
			input = response.getContent();
			int result = input.read();
			if (Character.isDigit(result)) {
				return Integer.parseInt(Character.toString((char) result));
			}

			throw new UpdateException(MessageFormat.format(
					"Invalid response: {0} (character: {1})",
					Integer.toHexString(result), (char) result));
		} finally {
			response.close();
		}
	}

	public static String getInitialURL() {
		IUpdater updater = CoreMoSyncPlugin.getDefault().getUpdater();
		if (updater instanceof DefaultUpdater2) {
			try {
				return ((DefaultUpdater2) updater)
						.getInitialRegistrationRequestURL().toExternalForm();
			} catch (MalformedURLException e) {
				// That's ok -- we'll log this elsewhere.
			}
		}

		return "";
	}

	@Override
	public void sendStats(String stats) throws IOException {
		OutputStream output = null;
		InputStream input = null;
		HttpURLConnection connection = null;

		try {
			URL statsURL = getRequestURL("stats.php", null);
			// HTTP POST - fake HTML form :)
			connection = (HttpURLConnection) statsURL.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			HashMap<String, String> params = new HashMap<String, String>();
			params.putAll(MosyncUIPlugin.getDefault().getVersionParameters(true));
			params.put("stats", stats);
			String postRequest = Util.toGetUrl(null, params);
			byte[] postRequestData = postRequest.getBytes();
			connection.setRequestProperty("Content-Length", Integer.toString(postRequestData.length));
			output = connection.getOutputStream();
			output.write(postRequestData);
			input = connection.getInputStream();
			int code = connection.getResponseCode();
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				Util.transfer(input, System.err);
			}
			if (code != HttpURLConnection.HTTP_OK) {
				throw new IOException("Stats server did not accept stats data.");
			}
		} finally {
			Util.safeClose(output);
			Util.safeClose(input);
		}
	}

}
