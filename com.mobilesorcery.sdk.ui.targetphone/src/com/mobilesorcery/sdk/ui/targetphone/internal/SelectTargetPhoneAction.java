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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;

import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.targetphone.Activator;

public class SelectTargetPhoneAction implements
		IWorkbenchWindowPulldownDelegate {

	private final class OBEXScanJob extends Job {
		private final TargetPhone phone;

		private OBEXScanJob(TargetPhone phone) {
			super("Scanning for OBEX service");
			this.phone = phone;
		}

		protected IStatus run(IProgressMonitor monitor) {
			return assignPort(window, monitor, phone, true);
		}
	}

	private ISelection selection;
	protected IAction targetDialogAction;
	private IWorkbenchWindow window;

	public Menu getMenu(Control parent) {
		Menu menu = new Menu(parent);
		List<TargetPhone> phones = Activator.getDefault()
				.getSelectedTargetPhoneHistory();
		for (Iterator<TargetPhone> phoneIterator = phones.iterator(); phoneIterator
				.hasNext();) {
			TargetPhone phone = phoneIterator.next();
			MenuItem item = new MenuItem(menu, SWT.CHECK);
			item.setText(computeTargetPhoneLabel(phone));
			item.setSelection(phone == Activator.getDefault()
					.getCurrentlySelectedPhone());
			item.setData(phone);
			item.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent event) {
					widgetSelected(event);
				}

				public void widgetSelected(SelectionEvent event) {
					TargetPhone phone = (TargetPhone) event.widget.getData();
					Activator.getDefault().setCurrentlySelectedPhone(phone);
				}

			});
		}

		if (phones.size() == 0) {
			MenuItem empty = new MenuItem(menu, SWT.PUSH);
			empty.setText("<Empty>");
			empty.setEnabled(false);
		}

		new MenuItem(menu, SWT.SEPARATOR);
		MenuItem select = new MenuItem(menu, SWT.PUSH);
		select.setText("&Scan for Device...");
		select.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				run(targetDialogAction);
			}
		});

		if (phones.size() > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
			final MenuItem reassign = new MenuItem(menu, SWT.PUSH);
			reassign.setText("Edit Device &List...");
			reassign.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					openTargetPhoneListDialog(reassign.getDisplay());
				}
			});
		}

		return menu;
	}

	private static String computeTargetPhoneLabel(TargetPhone phone) {
		IProfile preferredProfile = phone.getPreferredProfile();
		String preferredProfileStr = preferredProfile == null ? "" : " - ["
				+ preferredProfile.getName() + "]";
		return phone.getName() + preferredProfileStr;
	}

	protected void openTargetPhoneListDialog(Display display) {
		EditDeviceListDialog dialog = new EditDeviceListDialog(new Shell(
				display));
		dialog.open();
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
		final TargetPhone[] phone = new TargetPhone[1];
		try {
			phone[0] = selectPhone();
			if (phone[0] == null) {
				return; // Cancelled.
			}
		} catch (IOException e) {
			Policy.getStatusHandler().show(
					new Status(IStatus.ERROR, Activator.PLUGIN_ID, e
							.getMessage(), e), e.toString());
		}

		Job job = new OBEXScanJob(phone[0]);

		job.setUser(true);
		job.schedule();
	}

	/**
	 * Pops up a dialog where the user can select a [BT] target phone.
	 * 
	 * @return
	 * @throws IOException
	 */
	public static TargetPhone selectPhone() throws IOException {
		SearchDeviceDialog dialog = new SearchDeviceDialog();
		TargetPhone info = dialog.open();
		return info;
	}

	static IStatus assignPort(IShellProvider shellProvider, IProgressMonitor monitor, TargetPhone phone, boolean askIfNoProfileAssigned) {
		try {
			monitor.beginTask("Scanning BT device for OBEX service", 1);
			monitor.setTaskName("Scanning BT device for OBEX service");
			int port = ServiceSearch.search(phone.getAddress());
			if (port != -1) {
				phone.assignPort(port);
				Activator.getDefault().addToHistory(phone);
			} else {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						"The device connected to has no OBEX service");
			}
			
			if (!askIfNoProfileAssigned || phone.getPreferredProfile() == null) {
				monitor.setTaskName(MessageFormat.format("Assigning profile to {0}", phone.getName()));
				selectProfileForPhone(phone, shellProvider, askIfNoProfileAssigned);
			}
			
			return Status.OK_STATUS;
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e
					.getMessage(), e);
		} finally {
			monitor.worked(1);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.targetDialogAction = action;
		this.selection = selection;
	}

	static IProfile selectProfileForPhone(final TargetPhone phone,
			final IShellProvider shellProvider, boolean askOnlyIfNotAssigned) {
		if (!askOnlyIfNotAssigned || phone.getPreferredProfile() == null) {
			Display d = shellProvider == null ? Display.getDefault() : shellProvider.getShell().getDisplay();
			d.syncExec(new Runnable() {
				public void run() {
					Shell shell = shellProvider == null ? new Shell(Display
							.getDefault()) : shellProvider.getShell();
					EditDeviceListDialog dialog = new EditDeviceListDialog(shell);
					dialog.setInitialTargetPhone(phone);
					dialog.open();
				}
			});
		}
		
		return phone.getPreferredProfile();
	}
}
