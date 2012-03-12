package com.mobilesorcery.sdk.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormText;

public class Note extends Composite {

	private final Label icon;
	private final Label text;

	public Note(Composite parent, int style) {
		super(parent, style | SWT.BORDER);
		GridLayout layout = UIUtils.newPrefsLayout(2);
		setLayout(layout);
		Color bkg = getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		Color fg = getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);

		icon = new Label(this, SWT.NONE);
		icon.setImage(PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_INFO_TSK));
		icon.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false,
				1, 1));

		text = new Label(this, SWT.WRAP);
		text.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, true, true));

		text.setForeground(fg);
		text.setBackground(bkg);
		icon.setBackground(bkg);
		setForeground(fg);
		setBackground(bkg);
	}

	public void setText(String body) {
		text.setText(body);
	}

}
