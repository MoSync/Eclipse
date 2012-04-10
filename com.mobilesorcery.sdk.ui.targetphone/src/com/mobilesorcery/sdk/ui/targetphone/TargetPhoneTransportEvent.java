package com.mobilesorcery.sdk.ui.targetphone;

import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class TargetPhoneTransportEvent {

	public final Object type;
	public final ITargetPhoneTransport transport;
	public final Object project;
	public final IBuildVariant variant;


	public TargetPhoneTransportEvent(Object type, ITargetPhoneTransport transport,
			MoSyncProject project, IBuildVariant variant) {
		this.type = type;
		this.transport = transport;
		this.project = project;
		this.variant = variant;
	}
}
