package com.mobilesorcery.sdk.ui.targetphone.internal;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransport;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransportDelegate;

public class TargetPhoneTransportProxy implements ITargetPhoneTransport {

	private String id;
	private IConfigurationElement transportExtension;
	private ITargetPhoneTransportDelegate delegate;
	private ImageDescriptor icon;

	public TargetPhoneTransportProxy(IConfigurationElement transportExtension) {
		this.id = transportExtension.getAttribute("id");
		String iconId = transportExtension.getAttribute("icon");
		String declaringPlugin = transportExtension.getDeclaringExtension()
				.getNamespaceIdentifier();
		if (iconId != null) {
			try {
				icon = AbstractUIPlugin.imageDescriptorFromPlugin(
						declaringPlugin, iconId);
			} catch (Exception e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
		}

		this.transportExtension = transportExtension;
	}

	private void initDelegate() {
		if (delegate == null) {
			try {
				delegate = (ITargetPhoneTransportDelegate) transportExtension
						.createExecutableExtension("implementation");
			} catch (CoreException e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
			transportExtension = null;
		}
	}

	public String getId() {
		return id;
	}

	public ITargetPhone load(IMemento memento, String name) {
		initDelegate();
		return delegate.load(memento, name);
	}

	public boolean store(ITargetPhone phone, IMemento memento) {
		initDelegate();
		return delegate.store(phone, memento);
	}

	public void send(IShellProvider shell, MoSyncProject project,
			ITargetPhone phone, File packageToSend, IProgressMonitor monitor)
			throws CoreException {
		initDelegate();
		delegate.send(shell, project, phone, packageToSend, monitor);
	}

	public ITargetPhone scan(IShellProvider shell, IProgressMonitor monitor)
			throws CoreException {
		initDelegate();
		return delegate.scan(shell, monitor);
	}

	public ImageDescriptor getIcon() {
		return icon;
	}

	public String getDescription(String context) {
		initDelegate();
		return delegate.getDescription(context);
	}

	public boolean isAvailable() {
		initDelegate();
		return delegate.isAvailable();
	}

	public IDeviceFilter getAcceptedProfiles() {
		initDelegate();
		return delegate.getAcceptedProfiles();
	}

}
