package com.mobilesorcery.sdk.deployment.internal.ui.ftp;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.deployment.DeploymentPlugin;
import com.mobilesorcery.sdk.deployment.IDeploymentStrategy;
import com.mobilesorcery.sdk.deployment.IDeploymentStrategyFactory;
import com.mobilesorcery.sdk.deployment.internal.ftp.FTPDeploymentStrategy;

public class FTPDeploymentParamsPage extends WizardPage implements Listener {

	private Text hostText;
	private Text pathText;
	private Text userText;
	private Text passwordText;
    private FTPDeploymentStrategy strategy;

	protected FTPDeploymentParamsPage() {
		super("Deploy via FTP");
		setTitle(getName());
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		Label hostLabel = new Label(main, SWT.NONE);
		hostLabel.setText("&Host:");
		
		hostText = newText(main);
		
		Label pathLabel = new Label(main, SWT.NONE);
		pathLabel.setText("&Path:");
		
		pathText = newText(main);
		
		Label userLabel = new Label(main, SWT.NONE);
		userLabel.setText("&User:");
		
		userText = newText(main);
		
		Label passwordLabel = new Label(main, SWT.NONE);
		passwordLabel.setText("Pass&word:");
		
		passwordText = new Text(main, SWT.BORDER | SWT.PASSWORD | SWT.SINGLE);
		passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		if (strategy == null) {
		    strategy = new FTPDeploymentStrategy();
		}
		
		hostText.setText(notNull(strategy.getHost()));
		pathText.setText(notNull(strategy.getRemotePath()));
		userText.setText(notNull(strategy.getUsername()));
		passwordText.setText(notNull(strategy.getPassword()));
		
		hostText.addListener(SWT.Modify, this);
        pathText.addListener(SWT.Modify, this);
        userText.addListener(SWT.Modify, this);
		setControl(main);
	}
	
	private String notNull(String str) {
	    return str == null ? "" : str;
    }

    private Text newText(Composite main) {
		Text text = new Text(main, SWT.BORDER | SWT.SINGLE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return text;
	}

	public IDeploymentStrategy getStrategy() {
		strategy.setHost(hostText.getText());
		strategy.setRemotePath(pathText.getText());
		strategy.setUsername(userText.getText());
		strategy.setPassword(passwordText.getText());
		return strategy;
	}

    public void setStrategyToEdit(IDeploymentStrategy strategyToEdit) {
        if (strategyToEdit instanceof FTPDeploymentStrategy) {
            this.strategy = (FTPDeploymentStrategy) strategyToEdit;
        }
    }


    public void handleEvent(Event event) {
        validate();
    }

    private void validate() {
        String errorMessage = validateHost();
        if (errorMessage == null) {
            errorMessage = validatePath();
        }
        
        setMessage(errorMessage, errorMessage == null ? NONE : ERROR);
    }

    private String validatePath() {
        return pathText.getText().length() == 0 ? "Path must not be empty" : null;
    }

    private String validateHost() {
        String host = hostText.getText();
        try {
            URL hostURL = new URL(host);
            String protocol = hostURL.getProtocol();
            if (!"ftp".equals(protocol)) {
                return "Only the FTP: protocol is allowed"; 
            }
        } catch (MalformedURLException e) {
            return "Invalid URL";
        }
        
        return null;
    }

}
