package com.mobilesorcery.sdk.extensionsupport.ui.internal.properties;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.CUtil;
import com.mobilesorcery.sdk.core.Capabilities;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.extensionsupport.ExtensionCompiler;
import com.mobilesorcery.sdk.extensionsupport.ExtensionSupportPlugin;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.ProfileLabelProvider;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener;

public class ExtensionsPropertyPage extends MoSyncPropertyPage {

	private Button useDefault;
	private Button useCustom;
	private Text customPrefix;
	private Button jsEnable;
	private CheckboxTableViewer supportedPlatforms;

	public ExtensionsPropertyPage() {
		super(true);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(2));
		
		useDefault = new Button(main, SWT.RADIO);
		useDefault.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
		useDefault.setText(MessageFormat.format("Use default prefix for C functions ({0})", ExtensionCompiler.getDefaultPrefix(getProject())));
		useCustom = new Button(main, SWT.RADIO);
		useCustom.setText("Use custom prefix:");
		
		customPrefix = new Text(main, SWT.SINGLE | SWT.BORDER);
		customPrefix.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		customPrefix.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				useCustom.setSelection(true);
				useDefault.setSelection(false);
			}			
		});
		
		Group jsGroup = new Group(main, SWT.NONE);
		jsGroup.setText("JavaScript");
		jsGroup.setLayout(new GridLayout(1, false));
		jsGroup.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
		
		jsEnable = new Button(jsGroup, SWT.CHECK);
		jsEnable.setText("&Generate JavaScript Library");
		jsEnable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		setText(customPrefix, getProject().getProperty(ExtensionSupportPlugin.PREFIX_PROP));
		useDefault.setSelection(!PropertyUtil.getBoolean(getProject(), ExtensionSupportPlugin.USE_CUSTOM_PREFIX_PROP));
		useCustom.setSelection(!useDefault.getSelection());
		jsEnable.setSelection(PropertyUtil.getBoolean(getProject(), ExtensionSupportPlugin.GENERATE_JS_PROP));
		
		UpdateListener listener = new UpdateListener(this);
		useDefault.addListener(SWT.Selection, listener);
		useCustom.addListener(SWT.Selection, listener);
		customPrefix.addListener(SWT.Modify, listener);
		jsEnable.addListener(SWT.Selection, listener);

		Group supportedPlatformsGroup = new Group(main, SWT.NONE);
		supportedPlatformsGroup.setText("Supported &platforms");
		supportedPlatformsGroup.setLayout(new GridLayout(1, false));
		supportedPlatformsGroup.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
		
		supportedPlatforms = CheckboxTableViewer.newCheckList(supportedPlatformsGroup, SWT.BORDER);
		supportedPlatforms.setLabelProvider(new ProfileLabelProvider(SWT.NONE));
		supportedPlatforms.setContentProvider(new ArrayContentProvider());
		supportedPlatforms.setInput(MoSyncTool.getDefault().getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE).
				getVendors(DeviceCapabilitiesFilter.create(new String[] { "Extensions" } ,new String[0])));
		String[] supportedPlatformNames = PropertyUtil.getStrings(getProject(), ExtensionSupportPlugin.SUPPORTED_PLATFORMS_PROP);
		IVendor[] platforms = new IVendor[supportedPlatformNames.length];
		for (int i = 0; i < supportedPlatformNames.length; i++) {
			platforms[i] = MoSyncTool.getDefault().getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE).getVendor(supportedPlatformNames[i]);
		}
		supportedPlatforms.setCheckedElements(platforms);
		supportedPlatforms.getTable().setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		
		supportedPlatforms.getTable().addListener(SWT.Selection, listener);
		updateUI();
		
		return main;
	}
	
	public void updateUI() {
		super.updateUI();
	}
	
	public void validate() {
		IMessageProvider message = DefaultMessageProvider.EMPTY;
		if (useCustom.getSelection() && !CUtil.isValidCIdentifier(customPrefix.getText())) {
			message = new DefaultMessageProvider("Custom prefix must be a valid C identifier", IMessageProvider.ERROR);
		} else if (supportedPlatforms.getCheckedElements().length == 0) {
			message = new DefaultMessageProvider("Must have at least one supported platform", IMessageProvider.ERROR);
		}
		setMessage(message);
	}
	
	public boolean performOk() {
		getProject().setProperty(ExtensionSupportPlugin.PREFIX_PROP, customPrefix.getText());
		PropertyUtil.setBoolean(getProject(), ExtensionSupportPlugin.USE_CUSTOM_PREFIX_PROP, useCustom.getSelection());
		PropertyUtil.setBoolean(getProject(), ExtensionSupportPlugin.GENERATE_JS_PROP, jsEnable.getSelection());
		Object[] selectedPlatformObjs = supportedPlatforms.getCheckedElements();
		String[] selectedPlatformNames = new String[selectedPlatformObjs.length];
		for (int i = 0; i < selectedPlatformObjs.length; i++) {
			selectedPlatformNames[i] = ((IVendor) selectedPlatformObjs[i]).getName();
		}
		PropertyUtil.setStrings(getProject(), ExtensionSupportPlugin.SUPPORTED_PLATFORMS_PROP, selectedPlatformNames);
		return true;
	}

}
