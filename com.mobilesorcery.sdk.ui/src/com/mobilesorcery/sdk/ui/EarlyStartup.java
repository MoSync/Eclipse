package com.mobilesorcery.sdk.ui;

import org.eclipse.ui.IStartup;

public class EarlyStartup implements IStartup {

	@Override
	public void earlyStartup() {
		MosyncUIPlugin.getDefault().earlyStartup();
	}

}
