package com.mobilesorcery.sdk.builder.iphoneos.ui.properties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

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

import com.mobilesorcery.sdk.builder.iphoneos.Activator;
import com.mobilesorcery.sdk.builder.iphoneos.PropertyInitializer;
import com.mobilesorcery.sdk.builder.iphoneos.SDK;
import com.mobilesorcery.sdk.builder.iphoneos.XCodeBuild;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class IPhoneOSSSDKPropertyPage extends MoSyncPropertyPage {

	private static final Object AUTOMATIC_SDK = "@auto";

	private ComboViewer iOSSDKs;
	private ComboViewer iOSSimulatorSDKs;

	class SDKLabelProvider extends LabelProvider {

		private final int sdkType;

		public SDKLabelProvider(int sdkType) {
			this.sdkType = sdkType;
		}

		@Override
		public String getText(Object element) {
			if (element == AUTOMATIC_SDK) {
				SDK sdk = XCodeBuild.getDefault().getDefaultSDK(sdkType);
				return MessageFormat.format("Automatic ({0})", sdk.getName());
			} else {
				return ((SDK) element).getName();
			}
		}

	}
	public IPhoneOSSSDKPropertyPage() {
		XCodeBuild.getDefault().refresh();
	}

	@Override
	protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));

        Label iOSLabel = new Label(main, SWT.NONE);
        iOSLabel.setText("iOS SDK");
        iOSSDKs = createSDKCombo(main, XCodeBuild.IOS_SDKS);

        Label iOSSimulatorLabel = new Label(main, SWT.NONE);
        iOSSimulatorLabel.setText("iOS Simulator SDK");
        iOSSimulatorSDKs = createSDKCombo(main, XCodeBuild.IOS_SIMULATOR_SDKS);

        Label infoLabel = new Label(main, SWT.WRAP);
        infoLabel.setFont(MosyncUIPlugin.getDefault().getFont(MosyncUIPlugin.FONT_INFO_TEXT));
        infoLabel.setText("Select which iOS SDKs to use for building.\nThe iOS Simulator SDK selected will be used for building for and launching the iPhone Simulator");
        infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));

        return main;
	}

	private ComboViewer createSDKCombo(Composite parent, int sdkType) {
		ComboViewer combo = new ComboViewer(parent);
		List<SDK> listSDKs = XCodeBuild.getDefault().listSDKs(sdkType);
	    combo.setContentProvider(new ArrayContentProvider());
        combo.setLabelProvider(new SDKLabelProvider(sdkType));
        ArrayList<Object> sdksPlusAuto = new ArrayList<Object>();
		sdksPlusAuto.add(AUTOMATIC_SDK);
		sdksPlusAuto.addAll(listSDKs);
		combo.setInput(sdksPlusAuto);
		boolean useAuto = PropertyUtil.getBoolean(getProject(), sdkType == XCodeBuild.IOS_SDKS ? PropertyInitializer.IOS_SDK_AUTO : PropertyInitializer.IOS_SIM_SDK_AUTO);
		SDK sdk = Activator.getDefault().getSDK(getProject(), sdkType);
		Object selected = useAuto ? AUTOMATIC_SDK : sdk;
		if (selected != null) {
			combo.setSelection(new StructuredSelection(selected), true);
		}
		return combo;
	}

	@Override
	public boolean performOk() {
		updateProperties(iOSSDKs, XCodeBuild.IOS_SDKS);
		updateProperties(iOSSimulatorSDKs, XCodeBuild.IOS_SIMULATOR_SDKS);

		return super.performOk();
	}

	private void updateProperties(ComboViewer sdkCombo, int sdkType) {
		IStructuredSelection selection = (IStructuredSelection) sdkCombo.getSelection();
		Object element = selection.getFirstElement();
		boolean auto = element == AUTOMATIC_SDK;
		SDK sdk = auto ? null : (SDK) element;
		Activator.getDefault().setSDK(getProject(), sdkType, sdk, auto);
	}

}
