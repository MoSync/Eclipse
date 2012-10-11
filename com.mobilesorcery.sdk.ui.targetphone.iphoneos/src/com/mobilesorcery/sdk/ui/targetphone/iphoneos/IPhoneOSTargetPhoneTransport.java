package com.mobilesorcery.sdk.ui.targetphone.iphoneos;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.builder.iphoneos.Activator;
import com.mobilesorcery.sdk.builder.iphoneos.IPhoneOSPackager;
import com.mobilesorcery.sdk.builder.iphoneos.XCodeBuild;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ProfileDBManager;
import com.mobilesorcery.sdk.profiles.filter.AbstractDeviceFilter;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransportDelegate;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhoneTransportEvent;

public class IPhoneOSTargetPhoneTransport implements
		ITargetPhoneTransportDelegate {

	private static final String ID = "ios";
	
	private static final IDeviceFilter IOS_DEVICE_FILTER = new AbstractDeviceFilter() {
		@Override
		public boolean acceptProfile(IProfile profile) {
			return "iOS".equalsIgnoreCase(ProfileDBManager.getPlatform(profile));
		}

		@Override
		public String getFactoryId() {
			return null;
		}

		@Override
		public void saveState(IMemento memento) {
		}
	};
	
	
	public IPhoneOSTargetPhoneTransport() {
	}

	@Override
	public boolean store(ITargetPhone phone, IMemento memento) {
		return (phone instanceof IPhoneOSTargetPhone);
	}

	@Override
	public ITargetPhone load(IMemento memento, String name) {
		return new IPhoneOSTargetPhone(name, ID);
	}

	@Override
	public void send(IShellProvider shell, MoSyncProject project, IBuildVariant variant,
			ITargetPhone phone, File packageToSend, IProgressMonitor monitor)
			throws CoreException {
		if (!XCodeBuild.getDefault().isValid()) {
			throw new CoreException(new Status(IStatus.ERROR, IPhoneOSTransportPlugin.PLUGIN_ID,
				"To send to an IPhone requires Xcode to be installed."));
		}
		
		if (!IPhoneOSPackager.shouldUseProvisioning(project, variant)) {
			throw new CoreException(new Status(IStatus.ERROR, IPhoneOSTransportPlugin.PLUGIN_ID,
				"To send to an IPhone requires a provisioning profile to be set."));
		}
		
		String cert = IPhoneOSPackager.getCertificate(project, variant);
		if (Activator.IPHONE_DEV_CERT.equals(cert)) {
			throw new CoreException(new Status(IStatus.ERROR, IPhoneOSTransportPlugin.PLUGIN_ID,
				"To send to an IPhone requires a distribution (ad hoc) certificate to be used."));
		}
		
		try {
			IPhoneOSOTAServer.getDefault().offerProject(project, variant);
			if (AwaitConnectionDialog.show(project, variant) == AwaitConnectionDialog.CANCEL) {
				TargetPhonePlugin.getDefault().notifyListeners(new TargetPhoneTransportEvent(TargetPhoneTransportEvent.LAUNCH_CANCELLED, phone, project, variant));
			} else {
				// We send the ABOUT_TO_RUN event here, after the dialog has been dismissed.
				TargetPhonePlugin.getDefault().notifyListeners(new TargetPhoneTransportEvent(TargetPhoneTransportEvent.ABOUT_TO_LAUNCH, phone, project, variant));
			}
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, IPhoneOSTransportPlugin.PLUGIN_ID,
					"Unable to start local OTA server", e));
		}
	}

	@Override
	public ITargetPhone scan(IShellProvider shell, IProgressMonitor monitor)
			throws CoreException {
		return new IPhoneOSTargetPhone("IPhone OTA", ID);
	}

	@Override
	public IDeviceFilter getAcceptedProfiles() {
		return IOS_DEVICE_FILTER;
	}

	@Override
	public String getDescription(String context) {
		return "IPhone Over-The-Air";
	}

	@Override
	public boolean isAvailable() {
		return XCodeBuild.isMac();
	}

}
