package com.mobilesorcery.sdk.ui.targetphone.iphoneos;

import com.mobilesorcery.sdk.ui.targetphone.AbstractTargetPhone;

public class IPhoneOSTargetPhone extends AbstractTargetPhone {

	public IPhoneOSTargetPhone(String name, String transportId) {
		super(name, transportId);
	}
	
	public String toString() {
		return "IPhone OTA";
	}
	
	public boolean equals(Object other) {
		// We consider all to be equal.
		return other instanceof IPhoneOSTargetPhone;
	}
	
	public int hashCode() {
		return 1;
	}

}
