package com.mobilesorcery.sdk.builder.winmobilecs.ui.properties;

import java.util.UUID;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.builder.winmobilecs.PropertyInitializer;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.Note;
import com.mobilesorcery.sdk.ui.UpdateListener;

public class WindowsPhoneGUIDPropertyPage extends MoSyncPropertyPage {

	public class GenerateRandomUIDListener implements Listener {

		@Override
		public void handleEvent(Event event) {
			guid.setText(CoreMoSyncPlugin.getDefault().generateUUID().toString());
		}

	}

	private Text guid;

	@Override
	protected Control createContents(Composite parent) {
	    noDefaultAndApplyButton();

        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(3, false));

        Label guidLabel = new Label(main, SWT.NONE);
        guidLabel.setText("&GUID:");
        guid = new Text(main, SWT.BORDER | SWT.SINGLE);
        guid.addListener(SWT.Modify, new UpdateListener(this));
        guid.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button generateRandom = new Button(main, SWT.PUSH);
        generateRandom.setText("Generate G&UID");
        generateRandom.addListener(SWT.Selection, new GenerateRandomUIDListener());
        generateRandom.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));

        setText(guid, getProject().getProperty(PropertyInitializer.GUID));

        Note note = new Note(main, SWT.NONE);
        note.setText("The \'Generate GUID\' button will generate a time-based UUID with a random MAC address.");
        note.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

        return main;
	}

	@Override
	public void validate() {
		String msg = null;
		int msgType = IMessageProvider.NONE;
		try {
			UUID.fromString(guid.getText());
		} catch (Exception e) {
			msg = "Invalid GUID (Note: No brackets are allowed)";
			msgType = IMessageProvider.ERROR;
		}
		setMessage(new DefaultMessageProvider(msg, msgType));
	}

	@Override
	public boolean performOk() {
		getProject().setProperty(PropertyInitializer.GUID, guid.getText());
		return super.performOk();
	}
}
