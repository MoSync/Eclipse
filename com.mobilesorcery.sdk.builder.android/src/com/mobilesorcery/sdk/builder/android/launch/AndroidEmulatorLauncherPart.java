package com.mobilesorcery.sdk.builder.android.launch;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.SimpleQueue;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;
import com.mobilesorcery.sdk.ui.launch.IEmulatorLaunchConfigurationPart;

public class AndroidEmulatorLauncherPart implements
		IEmulatorLaunchConfigurationPart {

	private Combo avd;
	private Android android;
	private final SimpleQueue q = new SimpleQueue(false);
	private HashSet<String> avdsAtLastRefresh = new HashSet<String>();

	public AndroidEmulatorLauncherPart() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(ILaunchConfiguration config) throws CoreException {
		avd.setText(config.getAttribute(AndroidEmulatorLauncher.AVD_NAME, ""));
	}

	@Override
	public Composite createControl(Composite parent, IUpdatableControl updatable) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		Label avdLabel = new Label(main, SWT.NONE);
		avdLabel.setText("&AVD:");
		avd = new Combo(main, SWT.NONE);
		avd.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button refresh = new Button(main, SWT.PUSH);
		refresh.setText("&Refresh AVD list");
		android = Android.getExternal();
		refresh.setEnabled(android.isValid());
		refresh.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				updateAVDs();
			}

		});

		updateAVDs();

		avd.addListener(SWT.Modify, new UpdateListener(updatable));
		return main;
	}

	protected void updateAVDs() {
		q.execute(new Runnable() {

			@Override
			public void run() {
				try {
					final List<String> avds = android.listAVDs();
					avdsAtLastRefresh = new HashSet<String>(avds);
					avd.getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							String oldText = avd.getText();
							avd.setItems(avds.toArray(new String[0]));
							avd.setText(oldText);
						}

					});
				} catch (CoreException e) {
					CoreMoSyncPlugin.getDefault().log(e);
				}
			}

		});
	}

	@Override
	public void apply(ILaunchConfigurationWorkingCopy copy) {
		copy.setAttribute(AndroidEmulatorLauncher.AVD_NAME, avd.getText().trim());
	}

	@Override
	public IMessageProvider validate() {
		String avdName = avd.getText().trim();
		if (Util.isEmpty(avdName)) {
			return new DefaultMessageProvider("No AVD set", IMessageProvider.ERROR);
		} else if (!avdsAtLastRefresh.contains(avdName)) {
			return new DefaultMessageProvider(MessageFormat.format("No AVD found with name {0}", avdName), IMessageProvider.WARNING);
		}
		return null;
	}

}
