package com.mobilesorcery.sdk.ui.targetphone;

import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;

public class TargetPhoneTransportEvent {
	/**
	 * A constant for indicating an event before sending a file.
	 */
	public final static String PRE_SEND = "send";
	
	/**
	 * A constant for indicating when the app should be able to launch,
	 * always after building, usually after having sent the app to the device,
	 * but never before actually launching
	 * The intention of this event is to prompt the user for manual actions;
	 * it may also provide the opportunity to the user to cancel the on-device launch.
	 * clients are responsible for sending this event at an appropriate time.
	 */
	public final static String ABOUT_TO_LAUNCH = "launch";

	public final Object type;
	public final ITargetPhone phone;
	public final MoSyncProject project;
	public final IBuildVariant variant;

	public TargetPhoneTransportEvent(Object type, ITargetPhone phone,
			MoSyncProject project, IBuildVariant variant) {
		this.type = type;
		this.phone = phone;
		this.project = project;
		this.variant = variant;
	}


	public static boolean isType(String type, TargetPhoneTransportEvent event) {
		return Util.equals(type, event == null ? null : event.type);
	}
}
