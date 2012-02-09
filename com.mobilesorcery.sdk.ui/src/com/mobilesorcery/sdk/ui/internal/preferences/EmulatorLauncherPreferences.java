package com.mobilesorcery.sdk.ui.internal.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class EmulatorLauncherPreferences extends PreferencePage implements IWorkbenchPreferencePage, IUpdatableControl  {

	public final static Object ASK_ME = new Object();

	class LauncherLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			if (ASK_ME == element) {
				return "Ask every time";
			} else if (element instanceof IEmulatorLauncher) {
				IEmulatorLauncher launcher = (IEmulatorLauncher) element;
				return launcher.getName();
			}
			return element.toString();
		}

	}

	class PlatformLauncherUI extends Composite {

		private final Label name;
		private final ComboViewer preferredLauncher;
		private IPackager packager;

		public PlatformLauncherUI(Composite parent, int style) {
			super(parent, style);
			setLayout(UIUtils.newPrefsLayout(2));
			name = new Label(this, SWT.NONE);
			name.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
			preferredLauncher = new ComboViewer(this);
			preferredLauncher.setContentProvider(new ArrayContentProvider());
			preferredLauncher.setLabelProvider(new LauncherLabelProvider());
			preferredLauncher.getControl().setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void setPackager(IPackager packager) {
			this.packager = packager;
			String platform = packager.getPlatform();
			String packagerId = packager.getId();
			name.setText(platform);
			ArrayList launchers = new ArrayList();
			launchers.add(ASK_ME);
			launchers.addAll(getAvailableLaunchers(packager));
			preferredLauncher.setInput(launchers.toArray());
			IEmulatorLauncher preferred = CoreMoSyncPlugin.getDefault().getPreferredLauncher(packagerId);
			StructuredSelection selection = new StructuredSelection(preferred == null ? ASK_ME : preferred);
			preferredLauncher.setSelection(selection);
		}

		public IPackager getPackager() {
			return packager;
		}

		public IEmulatorLauncher getPreferredLauncher() {
			IStructuredSelection selection = (IStructuredSelection) preferredLauncher.getSelection();
			Object selectedElement = selection.getFirstElement();
			if (selectedElement == ASK_ME) {
				return null;
			} else {
				return (IEmulatorLauncher) selectedElement;
			}
		}
	}

	private final ArrayList<PlatformLauncherUI> platformUIs = new ArrayList<EmulatorLauncherPreferences.PlatformLauncherUI>();

	@Override
	public void init(IWorkbench workbench) {
	}

	public Collection<IEmulatorLauncher> getAvailableLaunchers(IPackager packager) {
		ArrayList<IEmulatorLauncher> result = new ArrayList<IEmulatorLauncher>();
		for (String launcherId : CoreMoSyncPlugin.getDefault().getEmulatorLauncherIds()) {
			// TODO: FILTER!
			IEmulatorLauncher launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(launcherId);
			int launchType = launcher.getLaunchType(packager);
			if (launchType >= IEmulatorLauncher.LAUNCH_TYPE_DEFAULT) {
				result.add(launcher);
			}
		}
		return result;
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(1));

		Label info = new Label(main, SWT.WRAP);
		info.setText("Select which emulator to use for which platform.\n" +
				"Please note that this only applies to launch configurations " +
				"that has automatic emulator selection enabled.");
		info.setLayoutData(new GridData(2 * UIUtils.getDefaultFieldSize(), SWT.DEFAULT));

		List<IPackager> packagers = CoreMoSyncPlugin.getDefault().getPackagers();
		for (IPackager packager : packagers) {
			if (packager.getId() != null && packager.getPlatform() != null && getAvailableLaunchers(packager).size() > 1) {
				PlatformLauncherUI platformUI = new PlatformLauncherUI(main, SWT.NONE);
				platformUI.setPackager(packager);
				platformUIs.add(platformUI);
			}
		}
		return main;
	}

	@Override
	public boolean performOk() {
		for (PlatformLauncherUI platformUI : platformUIs) {
			IPackager packager = platformUI.getPackager();
			IEmulatorLauncher launcher = platformUI.getPreferredLauncher();
			CoreMoSyncPlugin.getDefault().setPreferredLauncher(packager.getId(), launcher == null ? null : launcher.getId());
		}
		return super.performOk();
	}

	@Override
	public void performDefaults() {
		// TBD.
	}

	@Override
	public void updateUI() {
		// TODO Auto-generated method stub

	}

}
