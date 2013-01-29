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
package com.mobilesorcery.sdk.ui.targetphone.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;
import org.eclipse.ui.dialogs.ListDialog;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransport;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

public class SelectTargetPhoneAction implements
		IWorkbenchWindowPulldownDelegate {

	class ScanJob extends Job {

		private final ITargetPhoneTransport transport;

		public ScanJob(ITargetPhoneTransport transport) {
			super("Scanning for devices");
			this.transport = transport;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				ITargetPhone phone = transport.scan(window, monitor);
				if (phone != null) {
					TargetPhonePlugin.getDefault().addToHistory(phone);
					monitor.setTaskName(MessageFormat.format(
							"Assigning profile to {0}", phone.getName()));
					SelectTargetPhoneAction.selectProfileForPhone(
							TargetPhonePlugin.getDefault().getCurrentProfileManagerType(),
							phone, window, false);
				}
				return Status.OK_STATUS;
			} catch (CoreException e) {
				return e.getStatus();
			}
		}

	}

	private ISelection selection;
	protected IAction targetDialogAction;
	IWorkbenchWindow window;

	@Override
	public Menu getMenu(Control parent) {
		Menu menu = new Menu(parent);
		List<ITargetPhone> phones = TargetPhonePlugin.getDefault()
				.getSelectedTargetPhoneHistory();
		for (Iterator<ITargetPhone> phoneIterator = phones.iterator(); phoneIterator
				.hasNext();) {
			ITargetPhone phone = phoneIterator.next();
			MenuItem item = new MenuItem(menu, SWT.CHECK);
			item.setText(computeTargetPhoneLabel(phone));
			item.setSelection(phone == TargetPhonePlugin.getDefault()
					.getCurrentlySelectedPhone());
			item.setData(phone);
			item.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent event) {
					widgetSelected(event);
				}

				@Override
				public void widgetSelected(SelectionEvent event) {
					ITargetPhone phone = (ITargetPhone) event.widget.getData();
					TargetPhonePlugin.getDefault().setCurrentlySelectedPhone(
							phone);
				}

			});
		}

		if (phones.size() == 0) {
			MenuItem empty = new MenuItem(menu, SWT.PUSH);
			empty.setText("<Empty>");
			empty.setEnabled(false);
		}

		new MenuItem(menu, SWT.SEPARATOR);

		Collection<ITargetPhoneTransport> transports = TargetPhonePlugin
				.getDefault().getTargetPhoneTransports();

		for (final ITargetPhoneTransport transport : transports) {
			MenuItem select = new MenuItem(menu, SWT.PUSH);
			select.setText(MessageFormat.format("Scan for {0} device", transport.getDescription("")));
			Image icon = TargetPhonePlugin.getDefault().getIcon(transport);
			if (icon != null) {
				select.setImage(icon);
			}
			select.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					ScanJob job = new ScanJob(transport);
					job.setUser(true);
					job.schedule();
				}
			});
		}

		if (phones.size() > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
			final MenuItem reassign = new MenuItem(menu, SWT.PUSH);
			reassign.setText("Edit Device &List...");
			reassign.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					openTargetPhoneListDialog(reassign.getDisplay());
				}
			});
		}

		return menu;
	}

	private static String computeTargetPhoneLabel(ITargetPhone phone) {
		ArrayList<String> preferredProfiles = new ArrayList<String>();
		IProfile preferredProfile = phone.getPreferredProfile(MoSyncTool.DEFAULT_PROFILE_TYPE);
		if (preferredProfile != null) {
			preferredProfiles.add(preferredProfile.getName());
		}
		IProfile preferredLegacyProfile = phone.getPreferredProfile(MoSyncTool.LEGACY_PROFILE_TYPE);
		if (preferredLegacyProfile != null) {
			preferredProfiles.add(preferredLegacyProfile.getName());
		}
		String preferredProfileStr = preferredProfiles.isEmpty() ? "" : " - ["
				+ Util.join(preferredProfiles.toArray(), ",") + "]";
		return phone.getName() + preferredProfileStr;
	}

	protected void openTargetPhoneListDialog(Display display) {
	    Shell shell = new Shell(display);
	    try {
    		EditDeviceListDialog dialog = new EditDeviceListDialog(shell);
    		dialog.open();
	    } finally {
	        shell.dispose();
	    }
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.targetDialogAction = action;
		this.selection = selection;
	}

	/**
	 * A utility method for querying the user for which profile to assign
	 * to a specific device. If applicable, a device list dialog pops up (with the
	 * device fixed to the device passed in to this method)
	 * @param profileManagerType
	 * @param phone The target device, must not be <code>null</code>
	 * @param shellProvider
	 * @param askOnlyIfNotAssigned If <code>true</code>, will only ask the user
	 * if no profile is already assigned.
	 * @return
	 */
	public static IProfile selectProfileForPhone(int profileManagerType, final ITargetPhone phone,
			final IShellProvider shellProvider, boolean askOnlyIfNotAssigned) {
		if (!askOnlyIfNotAssigned || phone.getPreferredProfile(profileManagerType) == null) {
			Display d = shellProvider == null ? Display.getDefault()
					: shellProvider.getShell().getDisplay();
			d.syncExec(new Runnable() {
				@Override
				public void run() {
					Shell shell = shellProvider == null ? new Shell(Display
							.getDefault()) : shellProvider.getShell();
					EditDeviceListDialog dialog = new EditDeviceListDialog(
							shell);
					dialog.setFixedDevice(true);
					dialog.setInitialTargetPhone(phone);
					dialog.open();
				}
			});
		}

		return phone.getPreferredProfile(profileManagerType);
	}

	/**
	 * Lets the user select a target phone. If there is more than one
	 * <code>ITargetPhoneTransport</code> installed, a dialog is popped up which
	 * lets the user select type of transport.
	 * @param profileManagerType
	 *
	 * @return
	 */
	public static ITargetPhone selectPhone(ITargetPhone targetPhone, int profileManagerType, IShellProvider shellProvider,
			IProgressMonitor monitor) throws CoreException {
		ITargetPhoneTransport selectedTransport = targetPhone == null ? null : targetPhone.getTransport();
	    if (selectedTransport == null) {
	    	selectedTransport = selectTransport(shellProvider);
	    	if (selectedTransport != null) {
				targetPhone = selectedTransport.scan(shellProvider, monitor);
		    }
	    }

		if (targetPhone != null) {
		    TargetPhonePlugin.getDefault().addToHistory(targetPhone);
		    if (targetPhone.getPreferredProfile(profileManagerType) == null) {
                monitor.setTaskName(MessageFormat.format(
                        "Assigning profile to {0}", targetPhone.getName()));
                SelectTargetPhoneAction.selectProfileForPhone(profileManagerType, targetPhone,
                        shellProvider, true);
            }
		}

		return targetPhone;
	}

	public static ITargetPhoneTransport selectTransport(
			IShellProvider shellProvider) {
		List<ITargetPhoneTransport> transports = TargetPhonePlugin.getDefault()
				.getTargetPhoneTransports();
		ITargetPhoneTransport selectedTransport = null;
		if (transports.size() == 1) {
			selectedTransport = transports.get(0);
		} else {
			selectedTransport = showTransportsDialog(shellProvider);
		}

		return selectedTransport;
	}

	private static ITargetPhoneTransport showTransportsDialog(
			IShellProvider shellProvider) {
		final Shell shell = shellProvider.getShell();
		final ITargetPhoneTransport[] result = new ITargetPhoneTransport[1];

		shell.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				ListDialog dialog = new ListDialog(shell);
				dialog.setTitle("Select type of transport");
				dialog.setMessage("Select how you want to scan for the device");
				dialog.setContentProvider(new ArrayContentProvider());
				dialog.setLabelProvider(new TargetPhoneTransportLabelProvider());
				dialog.setInput(TargetPhonePlugin.getDefault()
						.getTargetPhoneTransports().toArray());
				if (dialog.open() == ListDialog.OK) {
					Object[] dialogResult = dialog.getResult();
					if (dialogResult.length > 0) {
						result[0] = (ITargetPhoneTransport) dialogResult[0];
					}
				}
			}
		});

		return result[0];

	}

	@Override
	public void run(IAction action) {
		ITargetPhoneTransport transport = selectTransport(window);
		if (transport != null) {
			ScanJob job = new ScanJob(transport);
			job.setUser(true);
			job.schedule();
		}
	}
}
