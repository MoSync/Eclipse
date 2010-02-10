/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.mobilesorcery.sdk.ui.targetphone.android;

import com.mobilesorcery.sdk.ui.targetphone.AbstractTargetPhone;

public class AndroidTargetPhone extends AbstractTargetPhone {

	private String serialNumber;

	public AndroidTargetPhone(String name, String serialNumber, String transportId) {
		super(name, transportId);
		this.serialNumber = serialNumber;
	}
	
	public String getSerialNumber() {
		return serialNumber;
	}

	public String toString() {
		return getName() + "-" + getSerialNumber();
	}
	
	public int hashCode() {
		return getSerialNumber().hashCode();
	}
	
	public boolean equals(Object o) {
		if (o instanceof AndroidTargetPhone) {
			return equals((AndroidTargetPhone) o);
		} else {
			return false;
		}
	}
	
	public boolean equals(AndroidTargetPhone other) {
		return getSerialNumber().equals(other.getSerialNumber());
	}
}
