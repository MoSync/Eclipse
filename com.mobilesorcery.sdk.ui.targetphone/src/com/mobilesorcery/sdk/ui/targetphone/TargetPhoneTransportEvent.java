package com.mobilesorcery.sdk.ui.targetphone;

import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;

public class TargetPhoneTransportEvent {
	/**
	 * A constant for indicating an event before sending a file.
	 */
	public final static String PRE_SEND = "send";

	public final Object type;
	public final ITargetPhoneTransport transport;
	public final MoSyncProject project;
	public final IBuildVariant variant;


	public TargetPhoneTransportEvent(Object type, ITargetPhoneTransport transport,
			MoSyncProject project, IBuildVariant variant) {
		this.type = type;
		this.transport = transport;
		this.project = project;
		this.variant = variant;
	}


	public static boolean isType(String type, TargetPhoneTransportEvent event) {
		return Util.equals(type, event == null ? null : event.type);
	}
}
