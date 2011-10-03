/*  Copyright (C) 2011 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.internal;

import java.util.regex.Pattern;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.util.Util;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

public class OSTester extends PropertyTester {

	private final static String OS_REGEXP_PROP = "os-regexp";

	private final static String OS_FAMILY_PROP = "os-family";

	private final static String WIN = "win";
	private final static String MAC = "mac";
	private final static String LINUX = "linux";


	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		try {
			String os = System.getProperty("os.name");
			String arg = (String) args[0];
			if (OS_REGEXP_PROP.equals(property)) {
				Pattern p = Pattern.compile(arg);
				return p.matcher(os).matches();
			} else if (OS_FAMILY_PROP.equals(property)) {
				if (WIN.equals(arg)) {
					return Util.isWindows();
				} else if (MAC.equals(arg)) {
					return Util.isMac();
				} else if (LINUX.equals(arg)) {
					return Util.isLinux();
				}
			}
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
		return false;
	}

}
