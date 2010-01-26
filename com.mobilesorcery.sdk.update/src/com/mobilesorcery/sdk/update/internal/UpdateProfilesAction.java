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
package com.mobilesorcery.sdk.update.internal;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.MutexSchedulingRule;
import com.mobilesorcery.sdk.update.UpdateManager;

public class UpdateProfilesAction extends Action {

    private final static MutexSchedulingRule updateMutex = new MutexSchedulingRule();

    private final class ProfileDownloadJob extends Job {
        private ProfileDownloadJob(String name) {
            super(name);
        }

        protected IStatus run(IProgressMonitor monitor) {
            try {
                monitor.beginTask(Messages.UpdateProfilesAction_Downloading, 1);
                update(monitor);
                return Status.OK_STATUS;
            } catch (Exception e) {
                e.printStackTrace();
                return new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e);
            }
        }
    }

    private boolean isStartedByUser = true;
	private Shell shell;

    public UpdateProfilesAction() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				shell = new Shell(Display.getDefault());
			}
		});
    }

    public void run() {
        UpdateJob update = new UpdateJob();
        update.setSystem(true);
        update.setRule(updateMutex);
        update.schedule();
    }

    class UpdateJob extends Job {

        public UpdateJob() {
            super(Messages.UpdateProfilesAction_UpdateJobTitle);
        }

        public IStatus run(IProgressMonitor monitor) {
            final UpdateManager mgr = UpdateManager.getDefault();
            try {
                boolean isRegistered = mgr.isRegistered();

                boolean retry = true;

                if (!isRegistered) {
                    retry = (openRegistrationDialog() == RegistrationDialog.OK);
                }

                while (retry) {
                    retry = false;
                    if (mgr.getUserHash() == null || mgr.getUserHash().length() == 0) {
                        if (!openConfirmDialog()) {
                            return Status.OK_STATUS;
                        }
                    }

                    boolean isValid = mgr.isValid();

                    if (!isValid) {
                        retry = showQuestion(getShell(), Messages.UpdateProfilesAction_InvalidConfirmationCode);
                        mgr.setUserHash(null);
                    } else {
                        if (!mgr.isUpdateAvailable()) {
                            if (isStartedByUser) {
                                showInfo(getShell(), Messages.UpdateProfilesAction_NoUpdatesAvailable);
                            }
                            return Status.OK_STATUS;
                        }

                       	startUpdateJob(isStartedByUser);
                    }
                }

            } catch (Exception e) {
            	CoreMoSyncPlugin.getDefault().log(e);
                MessageDialog.openError(getShell(), Messages.UpdateProfilesAction_IOError,
                        Messages.UpdateProfilesAction_CouldNotConnect);
            } finally {
                return Status.OK_STATUS;
            }
        }

        private void startUpdateJob(final boolean isStartedByUser) {
        	getShell().getDisplay().asyncExec(new Runnable() {
        		public void run() {

        			try {
        				UpdateWizard updateWizard = new UpdateWizard(UpdateManager.getDefault().getUpdateMessage());
        				UpdateWizardDialog updateWizardDialog = new UpdateWizardDialog(getShell(), updateWizard);
        				updateWizardDialog.create();
        				int result = updateWizardDialog.open();
        				if (result == UpdateWizardDialog.OK) {

        					Job job = new ProfileDownloadJob(Messages.UpdateProfilesAction_Downloading);

        					job.setUser(true);
        					job.schedule();
        				}
        			} catch (IOException e) {
        				MessageDialog.openError(getShell(), Messages.UpdateProfilesAction_IOError,
        						Messages.UpdateProfilesAction_CouldNotConnect);
        			}
        		}
        	});
        }
    }

    private void update(IProgressMonitor monitor) throws IOException {
        UpdateManager.getDefault().downloadProfileUpdate(new SubProgressMonitor(monitor, 1));
        UpdateManager.getDefault().runUpdater(new SubProgressMonitor(monitor, 1));
    }

    // Returns false if cancelled
    private boolean openConfirmDialog() throws IOException {
        final ArrayBlockingQueue<Boolean> result = new ArrayBlockingQueue<Boolean>(1);
        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                ConfirmationDialog dialog = new ConfirmationDialog(getShell());
                dialog.setIsStartedByUser(isStartedByUser);
                if (ConfirmationDialog.OK == dialog.open()) {
                    String confirmationCode = dialog.getConfirmationCode();
                    UpdateManager.getDefault().setUserHash(confirmationCode);
                    result.offer(true);
                } else {
                    result.offer(false);
                }
            }
        });

        try {
            return result.take();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    private int openRegistrationDialog() throws IOException {
        final UpdateManager mgr = UpdateManager.getDefault();
        RegistrationInfo info = new RegistrationInfo();

        boolean retry = true;
        int result = RegistrationDialog.CANCEL;

        while (retry) {
            retry = false;
            result = openRegistrationDialogUI(info);
            if (RegistrationDialog.OK == result) {
                MoSyncTool.getDefault().setProperty(MoSyncTool.EMAIL_PROP, info.mail);

                boolean alreadyRegistered = mgr.isRegistered();

                if (alreadyRegistered) {
                    boolean okToResend = showQuestion(getShell(), Messages.UpdateProfilesAction_AlreadyRegistered);
                    if (okToResend) {
                        mgr.resend();
                    }
                } else {
                    boolean ok = mgr.registerMe(info.mail, info.name, info.mailinglist);
                    if (ok) {
                        showInfo(getShell(), Messages.UpdateProfilesAction_ConfirmationCodeSent);
                        retry = false;
                    } else {
                        retry = showQuestion(getShell(), Messages.UpdateProfilesAction_InvalidEmail);
                    }
                }
            }
        }

        return result;
    }

    private int openRegistrationDialogUI(final RegistrationInfo info) throws IOException {
        final ArrayBlockingQueue<Integer> result = new ArrayBlockingQueue<Integer>(1);
        getShell().getDisplay().syncExec(new Runnable() {
            public void run() {
                RegistrationDialog dialog = new RegistrationDialog(getShell());
                dialog.setIsStartedByUser(isStartedByUser);
                dialog.setInfo(info);
                getShell().forceActive();
                result.offer(dialog.open());
            }
        });

        try {
            return result.take();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public static boolean showQuestion(final Shell shell, final String message) throws IOException {
        final ArrayBlockingQueue<Boolean> result = new ArrayBlockingQueue<Boolean>(1);
        shell.getDisplay().asyncExec(new Runnable() {
            public void run() {
                result.offer(MessageDialog.openQuestion(shell, Messages.UpdateProfilesAction_ConfirmTitle, message));
            }
        });

        try {
            return result.take();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public static void showInfo(final Shell shell, final String message) {
        shell.getDisplay().asyncExec(new Runnable() {
            public void run() {
                MessageDialog.openInformation(shell, Messages.UpdateProfilesAction_InformationTitle, message);
            }
        });
    }

    public void dispose() {
        // TODO Auto-generated method stub

    }

    private Shell getShell() {
    	return shell;
    }

    public void run(IAction action) {
        run();
    }

    public void selectionChanged(IAction arg0, ISelection arg1) {
    }

    public void setIsStartedByUser(boolean isStartedByUser) {
        this.isStartedByUser = isStartedByUser;
    }
}
