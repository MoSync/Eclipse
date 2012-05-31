package com.mobilesorcery.sdk.html5.ui;

import org.eclipse.core.expressions.PropertyTester;

public class IsJSODDLaunchTester extends PropertyTester {

	public IsJSODDLaunchTester() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		return ReloadHandler.extractVMs(receiver, null);
	}

}
