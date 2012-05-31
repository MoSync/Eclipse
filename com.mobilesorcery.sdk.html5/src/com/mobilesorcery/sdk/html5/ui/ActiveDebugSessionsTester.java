package com.mobilesorcery.sdk.html5.ui;

import org.eclipse.core.expressions.PropertyTester;

import com.mobilesorcery.sdk.html5.Html5Plugin;

public class ActiveDebugSessionsTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		 return !Html5Plugin.getDefault().getReloadServer().getVMs(false).isEmpty();
	}

}
