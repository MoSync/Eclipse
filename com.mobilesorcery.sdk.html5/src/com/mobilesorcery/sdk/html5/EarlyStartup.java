package com.mobilesorcery.sdk.html5;

import org.eclipse.ui.IStartup;

public class EarlyStartup implements IStartup {

	@Override
	public void earlyStartup() {
		Html5Plugin.getDefault().earlyStartup();
	}

}
