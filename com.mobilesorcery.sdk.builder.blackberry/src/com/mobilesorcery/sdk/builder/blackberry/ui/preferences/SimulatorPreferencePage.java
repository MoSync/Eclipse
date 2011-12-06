package com.mobilesorcery.sdk.builder.blackberry.ui.preferences;

import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.mobilesorcery.sdk.builder.blackberry.JDE;

public class SimulatorPreferencePage extends JDEPreferencePage {

	@Override
	protected Control createContents(Composite parent) {
		return super.createContents(parent);
/*		if (Util.isWindows()) {
			return super.createContents(parent);
		} else {
			Label error = new Label(parent, SWT.NONE);
			error.setText("BlackBerry native simulators are only available for Windows");
			return error;
		}*/
	}

	@Override
	protected int getToolType() {
		return JDE.TYPE_SIMULATOR;
	}
}
