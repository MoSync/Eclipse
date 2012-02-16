package com.mobilesorcery.sdk.testing.internal.ui;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.testing.project.MoSyncProjectTestManager;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;

public class TestingPropertyPage extends MoSyncPropertyPage {

    public TestingPropertyPage() {
		super(false);
	}

	private Text testFolders;

    @Override
	protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));

        Label testFoldersLabel = new Label(main, SWT.NONE);
        testFoldersLabel.setText("&Folders containing tests:");
        testFolders = new Text(main, SWT.BORDER | SWT.SINGLE);
        testFolders.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        testFolders.addModifyListener(new ModifyListener() {
            @Override
			public void modifyText(ModifyEvent event) {
                validate();
            }
        });
        setText(testFolders, getProject().getProperty(MoSyncProjectTestManager.TEST_RESOURCES));

        Label spacer = new Label(main, SWT.NONE);
        Label infoLabel = new Label(main, SWT.WRAP);
        infoLabel.setText("These folders will by default be excluded from non-testing build configurations");
        infoLabel.setFont(MosyncUIPlugin.getDefault().getFont(MosyncUIPlugin.FONT_INFO_TEXT));

        validate();

        return main;
    }

    @Override
	protected void validate() {
        setMessage(validatePathsField(null, "Folders containing tests", testFolders, new IPath[] { getProject().getWrappedProject().getLocation() }));
    }

    @Override
	public boolean performOk() {
        getProject().setProperty(MoSyncProjectTestManager.TEST_RESOURCES, testFolders.getText());
        return true;
    }

}
