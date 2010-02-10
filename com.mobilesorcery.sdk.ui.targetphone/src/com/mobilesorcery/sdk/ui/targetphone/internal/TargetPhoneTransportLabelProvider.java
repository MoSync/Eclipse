package com.mobilesorcery.sdk.ui.targetphone.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransport;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

public class TargetPhoneTransportLabelProvider extends LabelProvider {

	public String getText(Object element) {
		if (element instanceof ITargetPhoneTransport) {
			ITargetPhoneTransport transport = (ITargetPhoneTransport) element;
			return transport.getDescription(null);
		} else {
			return super.getText(element);
		}
	}
	
	public Image getImage(Object element) {
		if (element instanceof ITargetPhoneTransport) {
			ITargetPhoneTransport transport = (ITargetPhoneTransport) element;
			if (TargetPhonePlugin.getDefault().getImageRegistry().getDescriptor(transport.getId()) != null) {
				return TargetPhonePlugin.getDefault().getImageRegistry().get(transport.getId());
			}
		} 
		
		return null;
	}
}
