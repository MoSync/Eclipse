package com.mobilesorcery.sdk.testing.internal.ui;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IMessageProvider;
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

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.testing.TestPlugin;
import com.mobilesorcery.sdk.testing.project.MoSyncProjectTestManager;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;

public class TestingPropertyPage extends MoSyncPropertyPage {

    private Text testFolders;

    protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));
        
        Label testFoldersLabel = new Label(main, SWT.NONE);
        testFoldersLabel.setText("&Folders containing tests:");
        testFolders = new Text(main, SWT.BORDER | SWT.SINGLE);
        testFolders.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        testFolders.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                validate();
            }
        });
        setText(testFolders, getProject().getProperty(MoSyncProjectTestManager.TEST_RESOURCES));
        
        Label spacer = new Label(main, SWT.NONE);
        Label infoLabel = new Label(main, SWT.WRAP);
        infoLabel.setText("These folders will by default be excluded from non-testing build configurations");
        // Move to UIUtils
        FontData[] infoLabelFontData = infoLabel.getFont().getFontData();
        for (int i = 0; i < infoLabelFontData.length; i++) {
            infoLabelFontData[i].setHeight(infoLabelFontData[i].getHeight() - 1);
            //infoLabelFontData[i].setStyle(SWT.ITALIC);
        }
        final Font infoLabelFont = new Font(infoLabel.getDisplay(), infoLabelFontData);
        infoLabel.setFont(infoLabelFont);
        infoLabel.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent event) {
                infoLabelFont.dispose();
            }
        });
        
        validate();
        
        return main;
    }

    protected void validate() {
        setMessage(validatePathsField(null, "Folders containing tests", testFolders, new IPath[] { getProject().getWrappedProject().getLocation() }));
    }

    public boolean performOk() {
        getProject().setProperty(MoSyncProjectTestManager.TEST_RESOURCES, testFolders.getText());
        return true;
    }

}
