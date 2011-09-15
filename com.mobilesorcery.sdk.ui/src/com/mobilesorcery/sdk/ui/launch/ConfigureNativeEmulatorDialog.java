package com.mobilesorcery.sdk.ui.launch;

import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.MoReLauncher;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public abstract class ConfigureNativeEmulatorDialog extends IconAndMessageDialog implements IUpdatableControl {

	public static final int CONFIGURE_ID = 0xff01;

	public static final int NATIVE_LAUNCHER_ID = CONFIGURE_ID + 1;

	public static final int FALLBACK_ID = NATIVE_LAUNCHER_ID + 1;

	private Button dontAskAgain;

	private boolean isAutomaticSelection;

	private boolean needsConfig;

	private final  HashMap<Integer, IEmulatorLauncher> idToLauncherMap = new HashMap<Integer, IEmulatorLauncher>();

	private IEmulatorLauncher selectedLauncher;

	public ConfigureNativeEmulatorDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Performs the configure action. (When the user presses the button with {@link #CONFIGURE_ID}.)
	 */
	protected abstract void configure();

	/**
	 * Returns the packager (platform) that this dialog applies to -- also used to derive which
	 * the native launcher is.
	 * @return
	 */
	protected abstract IPackager getPackager();

    /**
     * <p>Clients must override this method to provide the user with a sensible message.</p>
     * <p>Typically, three different messages should be provided: one for every possible
     * combination of {@code isAutomaticSelection} and {@code needsConfig}, except for
     * when both are {@code false}.</p>
     * <p>However, there is no need to handle the exceptional case, since the code will
     * never reach this point if so.</p>
     *
     * @param isAutomaticSelection Whether this is an 'automatic' launch or not; in general
     * non-automatic launches should not provide any option to run other emulators.
     * @param needsConfig Whether the native emulator is not properly configured.
     * @return
     */
	protected abstract String createMessageBody(boolean isAutomaticSelection, boolean needsConfig);

	@Override
	public Control createDialogArea(Composite parent) {
		if (!isAutomaticSelection && !needsConfig) {
			throw new IllegalStateException("Cannot both platform specific AND properly configured. (Then we don't need this dialog.)");
		}
		getShell().setText(MessageFormat.format("Configure {0} launcher", getNativeLauncher().getName()));
		Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		message = createMessageBody(isAutomaticSelection, needsConfig);
		createMessageArea(contents);
		if (isAutomaticSelection) {
			Label spacer = new Label(contents, SWT.NONE);
			dontAskAgain = new Button(contents, SWT.CHECK);
			dontAskAgain.setText("Do not ask this again");
			dontAskAgain.addListener(SWT.Selection, new UpdateListener(this));
			dontAskAgain.setSelection(!needsConfig);
		}
		return contents;
	}

	@Override
	public void createButtonsForButtonBar(Composite parent) {
		if (needsConfig) {
			createConfigureButton(parent, !isAutomaticSelection);
		}

		if (isAutomaticSelection && !needsConfig) {
    		createFallbackButtons(parent);
    	}
    	createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

    	if (isAutomaticSelection && needsConfig) {
    		createFallbackButtons(parent);
    	}
    	if (!needsConfig) {
    		createLauncherButton(parent, getNativeLauncher(), NATIVE_LAUNCHER_ID, true);
    	}
	}

    /**
     * Clients may override this to create the configure button, which should have id {@code CONFIGURE_ID}
     * @param parent
     */
	protected void createConfigureButton(Composite parent, boolean defaultButton) {
    	createButton(parent, CONFIGURE_ID, MessageFormat.format("Configure {0} launcher", getNativeLauncher().getName()), defaultButton);
	}

	protected IEmulatorLauncher getNativeLauncher() {
    	IPackager packager = getPackager();
    	for (String id : CoreMoSyncPlugin.getDefault().getEmulatorLauncherIds()) {
    		IEmulatorLauncher launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(id);
    		if (launcher.getLaunchType(packager) == IEmulatorLauncher.LAUNCH_TYPE_NATIVE) {
    			return launcher;
    		}
    	}
    	return null;
    }

	/**
	 * <p>Clients may override this to create several possible fallback options,
	 * by calling {@link #createLauncherButton(Composite, IEmulatorLauncher, int, boolean)}.</p>
	 * <p>The default implementation creates a button for the default emulator.</p>
	 * @param parent
	 */
    protected void createFallbackButtons(Composite parent) {
    	createLauncherButton(parent, CoreMoSyncPlugin.getDefault().getEmulatorLauncher(MoReLauncher.ID), FALLBACK_ID, isAutomaticSelection);
    }

    protected void createLauncherButton(Composite parent, IEmulatorLauncher launcher, int id, boolean defaultButton) {
    	createButton(parent, id, MessageFormat.format("Run in {0}", launcher.getName()), defaultButton);
    	idToLauncherMap.put(id, launcher);
    }

    @Override
    public void buttonPressed(int buttonId) {
    	selectedLauncher = idToLauncherMap.get(buttonId);
    	if (selectedLauncher != null) {
    		buttonId = FALLBACK_ID;
    	}

    	setReturnCode(buttonId);
    	if (CONFIGURE_ID == buttonId) {
    		close();
    		CoreMoSyncPlugin.getDefault().setPreferredLauncher(getPackager().getId(), getNativeLauncher().getId());
    		configure();
    	} else if (FALLBACK_ID == buttonId) {
    		if (dontAskAgain.getSelection()) {
    			CoreMoSyncPlugin.getDefault().setPreferredLauncher(getPackager().getId(), selectedLauncher.getId());
    		}
    		close();
    	} else {
    		super.buttonPressed(buttonId);
    	}
    }

    /**
     * Returns the selected launcher, or {@code null} if none was selected.
     * @return
     */
	public IEmulatorLauncher getSelectedLauncher() {
    	return selectedLauncher;
    }

	protected void setSelectedLauncher(IEmulatorLauncher selectedLauncher) {
		this.selectedLauncher = selectedLauncher;
	}

	public void setIsAutomaticSelection(boolean isAutomaticSelection) {
		this.isAutomaticSelection = isAutomaticSelection;
	}

	public void setNeedsConfig(boolean needsConfig) {
		this.needsConfig = needsConfig;
	}

	@Override
	protected Image getImage() {
		return getQuestionImage();
	}

	@Override
	public void updateUI() {
		if (isAutomaticSelection) {
			getButton(CONFIGURE_ID).setEnabled(!dontAskAgain.getSelection());
		}
	}
}
