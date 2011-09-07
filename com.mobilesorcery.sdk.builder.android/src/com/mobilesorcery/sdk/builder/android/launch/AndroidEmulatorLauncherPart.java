package com.mobilesorcery.sdk.builder.android.launch;

import java.text.MessageFormat;
import java.util.ArrayList;
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
		IEmulatorLaunchConfigurationPart, IUpdatableControl {

	private Combo avd;
	private Button autoSelectAVD;
	private Android android;
	private final SimpleQueue q = new SimpleQueue(false);
	private HashSet<String> avdsAtLastRefresh = new HashSet<String>();

	public AndroidEmulatorLauncherPart() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(ILaunchConfiguration config) throws CoreException {
		avd.setText(config.getAttribute(AndroidEmulatorLauncher.AVD_NAME, ""));
		autoSelectAVD.setSelection(config.getAttribute(AndroidEmulatorLauncher.AUTO_SELECT_AVD, true));
		updateUI();
	}

	@Override
	public Composite createControl(Composite parent, IUpdatableControl updatable) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		UpdateListener listener = new UpdateListener(updatable);
		autoSelectAVD = new Button(main, SWT.CHECK);
		autoSelectAVD.setText("&Automatically select AVD");
		autoSelectAVD.addListener(SWT.Selection, new UpdateListener(this));
		autoSelectAVD.addListener(SWT.Selection, listener);

		autoSelectAVD.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true,
				false, 3, 1));

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
		avd.addListener(SWT.Modify, listener);
		updateUI();
		return main;
	}

	protected void updateAVDs() {
		q.execute(new Runnable() {

			@Override
			public void run() {
				try {
					android.refresh();
					List<AVD> avds = android.listAVDs();
					final List<String> avdNames = new ArrayList<String>();
					for (AVD avd : avds) {
						avdNames.add(avd.getName());
					}
					avdsAtLastRefresh = new HashSet<String>(avdNames);
					avd.getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							String oldText = avd.getText().trim();
							avd.setItems(avdNames.toArray(new String[0]));
							if (!Util.isEmpty(oldText)) {
								avd.setText(oldText);
							} else if (avdNames.size() > 0) {
								avd.setText(avdNames.get(0));
							}
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
		copy.setAttribute(AndroidEmulatorLauncher.AVD_NAME, avd.getText()
				.trim());
		copy.setAttribute(AndroidEmulatorLauncher.AUTO_SELECT_AVD,
				autoSelectAVD.getSelection());
	}

	@Override
	public IMessageProvider validate() {
		String avdName = avd.getText().trim();
		if (!autoSelectAVD.getSelection()) {
			if (Util.isEmpty(avdName)) {
				return new DefaultMessageProvider("No AVD set",
						IMessageProvider.ERROR);
			} else if (!avdsAtLastRefresh.contains(avdName)) {
				return new DefaultMessageProvider(MessageFormat.format(
						"No AVD found with name {0}", avdName),
						IMessageProvider.WARNING);
			}
		}
		return null;
	}

	@Override
	public void updateUI() {
		avd.setEnabled(!autoSelectAVD.getSelection());
	}

}
