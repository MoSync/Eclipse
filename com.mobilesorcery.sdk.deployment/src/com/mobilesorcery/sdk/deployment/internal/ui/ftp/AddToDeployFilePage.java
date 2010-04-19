package com.mobilesorcery.sdk.deployment.internal.ui.ftp;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class AddToDeployFilePage extends WizardPage {

	private Text deployFileText;
	private IFile initialDeployFile = null;

	protected AddToDeployFilePage() {
		super("Add to deploy file");
		setTitle(getName());
	}

	public void setInitialDeployFile(IFile initialDeployFile) {
		this.initialDeployFile = initialDeployFile;
	}
	
	public File internalGetDeployFile() {
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(deployFileText.getText()));
        return file.getLocation().toFile();
	}
	
	public File getDeployFile() {
		try {
		    return internalGetDeployFile();
		} catch (Exception e) {
			return null;
		}
	}
	
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		Label deployFileLabel = new Label(main, SWT.NONE);
		deployFileLabel.setText("&File:");
		
		deployFileText = new Text(main, SWT.BORDER | SWT.SINGLE);
		deployFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (initialDeployFile != null) {
			deployFileText.setText(initialDeployFile.getFullPath().toString());
		}
		
		deployFileText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                validate();
            }
        });
		
		setControl(main);
		validate();
	}

    private void validate() {
        int severity = IMessageProvider.NONE;
        String msg = null;
        
        if (!deployFileText.getText().isEmpty()) {
            try {
                 internalGetDeployFile();
            } catch (Exception e) {
                msg = e.getMessage();
                severity = IMessageProvider.ERROR;
            }
        } else {
            msg = "Leaving the field empty to not create a deploy file";
            severity = IMessageProvider.INFORMATION;
        }
        
        setMessage(msg, severity);
    }

}
