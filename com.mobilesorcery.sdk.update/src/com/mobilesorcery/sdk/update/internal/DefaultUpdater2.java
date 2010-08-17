package com.mobilesorcery.sdk.update.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.WebBrowserEditor;
import org.eclipse.ui.internal.browser.WebBrowserEditorInput;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IUpdater;
import com.mobilesorcery.sdk.core.MoSyncTool;
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

    public static final String SHOW_CONNECTION_FAILED_POPUP = "show.conn.fail.popup";

    public class OpenBrowserRunnable implements Runnable, IPartListener {

        final static String REGISTRATION_PERSPECTIVE_ID = "com.mobilesorcery.update.perspective";

        private URL whereToGo;
        private String name;

        public OpenBrowserRunnable(URL whereToGo, String name) {
            this.whereToGo = whereToGo;
            this.name = name;
        }

        public void run() {
            try {
                IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                try {
                    PlatformUI.getWorkbench().showPerspective(REGISTRATION_PERSPECTIVE_ID, window);
                } catch (WorkbenchException e) {
                    // We can still do SOMETHING.
                    CoreMoSyncPlugin.getDefault().log(e);
                }

                IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
                IWebBrowser browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.STATUS,
                        MosyncUpdatePlugin.PLUGIN_ID + ".browser", name, name);
                browser.openURL(whereToGo);

                window.getActivePage().addPartListener(this);
            } catch (PartInitException e) {
                e.printStackTrace();
                openFallbackDialog(whereToGo, name);
            }
        }

        public void closeRegistrationPerspective() {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IPerspectiveDescriptor perspective = PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(REGISTRATION_PERSPECTIVE_ID);
            if (perspective != null) {
                page.closePerspective(perspective, false, false);
            }
        }

        public void partActivated(IWorkbenchPart part) {
        }

        public void partBroughtToTop(IWorkbenchPart part) {
        }

        public void partClosed(IWorkbenchPart part) {
            if (part instanceof IEditorPart) {
                IEditorPart editor = (IEditorPart) part;
                IEditorInput input = editor.getEditorInput();
                if (input instanceof WebBrowserEditorInput) {
                    WebBrowserEditorInput wbInput = (WebBrowserEditorInput) input;
                    URL url = wbInput.getURL();
                    if (url != null && url.equals(whereToGo)) {
                        closeRegistrationPerspective();
                    }
                }
            }
        }

        public void partDeactivated(IWorkbenchPart part) {
        }

        public void partOpened(IWorkbenchPart part) {
        }

    }

    class UpdateJob extends Job {

        public UpdateJob() {
            super("Updating Profile Database");
        }

        protected IStatus run(IProgressMonitor monitor) {
            try {
                UpdateManager.getDefault().downloadProfileUpdate(monitor);
                if (showConfirmDialog()) {
                    UpdateManager.getDefault().runUpdater(new NullProgressMonitor());
                }
            } catch (Exception e) {
                return new Status(IStatus.ERROR, MosyncUpdatePlugin.PLUGIN_ID, "Could not download updated profile database", e);
            }
            return null;
        }

        private boolean showConfirmDialog() {
            final Display d = PlatformUI.getWorkbench().getDisplay();
            final boolean[] result = new boolean[1];
            d.syncExec(new Runnable() {
                public void run() {
                    Shell shell = new Shell(d);
                    result[0] = MessageDialog.openConfirm(shell, "Restart?",
                            "A new profile database has been downloaded. To activate the new profiles, the IDE must be restarted");
                    shell.dispose();
                }
            });

            return result[0];
        }
    }

    class UpdateRunnable implements Runnable {
        private boolean isStartedByUser;

        public UpdateRunnable(boolean isStartedByUser) {
            this.isStartedByUser = isStartedByUser;
        }

        public void run() {
            String userKey = MoSyncTool.getDefault().getProperty(MoSyncTool.USER_HASH_PROP_2);

            try {
                if (userKey == null || !validateKey()) {
                    userKey = requestKeyFromServer();
                    MoSyncTool.getDefault().setProperty(MoSyncTool.USER_HASH_PROP_2, userKey);
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
                    if (isStartedByUser || shouldPerformAutoUpdate()) {
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

    public boolean shouldPopupConnectionFailedMessage() {
        return MessageDialogWithToggle.ALWAYS.equals(MosyncUpdatePlugin.getDefault().getPreferenceStore().getString(SHOW_CONNECTION_FAILED_POPUP));
    }
    
    public void shouldPopupConnectionFailedMessage(boolean shouldPopupConnectionFailedMessage) {
        // Why doesn't the dialog do this properly - investigate later, for now just make it work.
        MosyncUpdatePlugin.getDefault().getPreferenceStore().setValue(SHOW_CONNECTION_FAILED_POPUP, shouldPopupConnectionFailedMessage ? MessageDialogWithToggle.ALWAYS : MessageDialogWithToggle.NEVER);
    }


    public void update(boolean isStartedByUser) {
        UpdateRunnable updater = new UpdateRunnable(isStartedByUser);
        Thread updateThread = new Thread(updater, "Registration and update");
        updateThread.start();
    }

    private void performUpdateAction(boolean isStartedByUser) throws IOException {
        try {
            if (UpdateManager.getDefault().isUpdateAvailable()) {
                UpdateJob updateJob = new UpdateJob();
                updateJob.setUser(isStartedByUser);
                updateJob.schedule();
            } else if (isStartedByUser) {
                showNoUpdatesDialog();
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException("Could not connect", e);
            }
        }
    }

    private void showNoUpdatesDialog() {
        final Display d = PlatformUI.getWorkbench().getDisplay();
        d.asyncExec(new Runnable() {
            public void run() {
                Shell shell = new Shell(d);
                MessageDialog.openInformation(shell, "No updates", "No updates found");
                shell.dispose();
            }
        });
    }

    public void popupConnectionFailedDialog(final boolean isStartedByUser) {
        final Display d = PlatformUI.getWorkbench().getDisplay();
        d.syncExec(new Runnable() {
            public void run() {
                popupConnectionFailedDialogSync(d, isStartedByUser);
            }
        });

    }

    private void popupConnectionFailedDialogSync(Display d, boolean isStartedByUser) {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        //Shell shell = new Shell(d);
        String dialogTitle = "Could not connect";
        String msg = "MoSync was unable to connect to its update server.\n"
                + "If you would like the latest device profiles and updates you will need to be connected to the Internet.\n"
                + "Check your Internet connection and your firewall settings.";

        try {
            if (isStartedByUser) {
                MessageDialog.openInformation(shell, dialogTitle, msg);
            } else {
                MessageDialogWithToggle dialog = MessageDialogWithToggle.open(MessageDialogWithToggle.INFORMATION, shell, dialogTitle, msg, 
                        "Show this message if no connection is found",  shouldPopupConnectionFailedMessage(),
                        MosyncUpdatePlugin.getDefault().getPreferenceStore(), SHOW_CONNECTION_FAILED_POPUP, SWT.SHELL_TRIM);
                if (dialog.getReturnCode() != -1) {
                    shouldPopupConnectionFailedMessage(dialog.getToggleState());
                }
            }
        } finally {
            //shell.dispose();
        }
    }


    private void userNotRegisteredAction() throws IOException {
        launchInternalBrowser(getRequestURL("registration/register/", assembleDefaultParams(true)), "Register");
    }

    private void userNotConfirmedAction() throws IOException {
        launchInternalBrowser(getRequestURL("registration/confirmation/", assembleDefaultParams(true)), "Confirm Registration");
    }

    private void launchInternalBrowser(URL whereToGo, String name) {
        PlatformUI.getWorkbench().getDisplay().syncExec(new OpenBrowserRunnable(whereToGo, name));
    }

    private void openFallbackDialog(URL whereToGo, String name) {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        String message = MessageFormat.format("Unable to launch browser. To {0}, enter this URL" + "into a web browser: {1}", name, whereToGo);
        MessageDialog dialog = new MessageDialog(shell, "Unable to launch browser", null, message, MessageDialog.ERROR, new String[] {
                "Copy web URL to Clipboard", "Ok" }, 1);
        int result = dialog.open();
        if (result == 0 /* copy to clipboard */) {
            Clipboard clipboard = new Clipboard(shell.getDisplay());
            try {
                clipboard.setContents(new Object[] { whereToGo.toExternalForm() }, new Transfer[] { TextTransfer.getInstance() });
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
        Response response = sendRequest(getRequestURL("registration/request/key", null));
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
        Response validated = sendRequest(getRequestURL("registration/request/validate", assembleDefaultParams(false)));
        boolean isValid = getBooleanResponse(validated, "Server failed to accept user key validation request");
        return isValid;
    }

    public int getUserStatus() throws IOException {
        Response response = sendRequest(getRequestURL("registration/request/userstatus", assembleDefaultParams(false)));
        InputStream input = null;
        try {
            input = response.getContent();
            int result = input.read();
            if (Character.isDigit(result)) {
                return Integer.parseInt(Character.toString((char) result));
            }

            throw new UpdateException(MessageFormat.format("Invalid response: {0} (character: {1})", Integer.toHexString(result), (char) result));
        } finally {
            response.close();
        }
    }

    private Map<String, String> assembleDefaultParams(boolean hashOnly) {
        HashMap<String, String> params = new HashMap<String, String>();
        if (!hashOnly) {
            int version = MoSyncTool.getDefault().getCurrentProfileVersion();
            String versionStr = Integer.toString(version);
            // For now we send the same version for all components.
            params.put("db", versionStr);
            params.put("sdk", versionStr);
            params.put("ide", versionStr);
        }
        params.put("hhash", getUserHalfHash());
        return params;
    }

}
