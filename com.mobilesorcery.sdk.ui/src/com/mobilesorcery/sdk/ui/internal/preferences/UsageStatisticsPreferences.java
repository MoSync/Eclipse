package com.mobilesorcery.sdk.ui.internal.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.stats.Stats;
import com.mobilesorcery.sdk.ui.TextDialog;
import com.mobilesorcery.sdk.ui.UIUtils;

public class UsageStatisticsPreferences extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Button sendUsageStatistics;

	public UsageStatisticsPreferences() {
		super("Usage Statistics");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(1));

		sendUsageStatistics = new Button(main, SWT.CHECK);
		sendUsageStatistics.setText("Send usage statistics");
		long sendInterval = Stats.getStats().getSendInterval();
		sendUsageStatistics.setSelection(sendInterval != Stats.DISABLE_SEND && sendInterval != Stats.UNASSIGNED_SEND_INTERVAL);
		Link showStats = new Link(main, SWT.NONE);
		showStats.setText("<a href=\"show\">If usage stats were sent now, what would it look like?</a>");
		showStats.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				TextDialog dialog = new TextDialog(shell);
				dialog.setTitle("Contents to send");
				dialog.setText(Stats.getStats().getContentsToSend());
				dialog.open();
			}
		});
		return main;
	}

	@Override
	public boolean performOk() {
		long sendInterval = sendUsageStatistics.getSelection() ?
				Stats.DEFAULT_SEND_INTERVAL : Stats.DISABLE_SEND;
		Stats.getStats().setSendInterval(sendInterval);
		return true;
	}

}
