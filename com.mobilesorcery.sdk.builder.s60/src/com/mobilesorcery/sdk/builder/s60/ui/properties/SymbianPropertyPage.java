package com.mobilesorcery.sdk.builder.s60.ui.properties;

import java.text.MessageFormat;
import java.util.Random;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import com.mobilesorcery.sdk.builder.s60.PropertyInitializer;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;

public class SymbianPropertyPage extends PropertyPage { 

	private static final String S60V2_UID = PropertyInitializer.S60V2_UID;
	private static final String S60V3_UID = PropertyInitializer.S60V3_UID;

	public class GenerateRandomUIDListener implements Listener {

		public void handleEvent(Event event) {
			s60v2UID.setText(PropertyInitializer.generateUID(S60V2_UID));
			s60v3UID.setText(PropertyInitializer.generateUID(S60V3_UID));
		}
	}

	private class ValidateListener implements Listener {

		public void handleEvent(Event event) {
			validate();
		}
		
	}
	
    private Text s60v3UID;

    private Text s60v2UID;

	private Label s60v2;

	private Label s60v3;

    public SymbianPropertyPage() {
        super();        
    }
    
    protected void performDefaults() {
	}
	
	public boolean performOk() {
	    boolean valid = isValid();
	    if (valid) {
            getProject().setProperty(S60V2_UID, s60v2UID.getText());
            getProject().setProperty(S60V3_UID, s60v3UID.getText());
	    }
	    
	    return valid;
	}
	
	public boolean isValid() {
        return validate() == null;
	}

	private String validate() {
		String errorMsg = validate(S60V2_UID, this.s60v2UID.getText());
		if (errorMsg == null) {
			errorMsg = validate(S60V3_UID, this.s60v3UID.getText());		
		}
		
		setErrorMessage(errorMsg);
		return errorMsg;
	}
	
    private String validate(String property, String uid) {
    	if (uid != null && uid.length() >= 2 && uid.substring(0, 2).equalsIgnoreCase("0x")) { //$NON-NLS-1$
        	try {
        		long uidValue = Long.parseLong(uid.substring(2), 16);
        		if (uidValue >= PropertyInitializer.getStartOfRange(property) &&
        			uidValue <= PropertyInitializer.getStartOfRange(property) + PropertyInitializer.getLengthOfRange(property)) {
        			return null; // Ok.
        		}
        	} catch (Exception e) {
        		// Just fall thru to error message
        	}
    	} else {
    		return Messages.SymbianPropertyPage_UIDPrefixError;
    	}
        
    	return MessageFormat.format(
    			Messages.SymbianPropertyPage_UIDRangeError,
    			getVersionString(property),
    			Long.toHexString(PropertyInitializer.getStartOfRange(property)),
    			Long.toHexString(PropertyInitializer.getStartOfRange(property) + PropertyInitializer.getLengthOfRange(property)));
    }

    private Object getVersionString(String property) {
		return S60V3_UID == property ? Messages.SymbianPropertyPage_S60V3 : Messages.SymbianPropertyPage_S60V2;
	}

	protected Control createContents(Composite parent) {
	    noDefaultAndApplyButton();
	    
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));
        
        Group symbianUIDs = new Group(main, SWT.NONE);
        symbianUIDs.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        symbianUIDs.setText(Messages.SymbianPropertyPage_S60_UIDs);
        symbianUIDs.setLayout(new GridLayout(3, false));
        
        s60v2 = new Label(symbianUIDs, SWT.NONE);
        s60v2.setText(Messages.SymbianPropertyPage_S60V2_UID);
        s60v2UID = new Text(symbianUIDs, SWT.SINGLE | SWT.BORDER);
        s60v2UID.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        s60v2UID.setText(getUID(S60V2_UID));

        Button generateRandom = new Button(symbianUIDs, SWT.PUSH);
        generateRandom.setText(Messages.SymbianPropertyPage_GenerateTestUIDs);
        generateRandom.addListener(SWT.Selection, new GenerateRandomUIDListener());
        generateRandom.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false, 1, 2));
        
        s60v3 = new Label(symbianUIDs, SWT.NONE);
        s60v3.setText(Messages.SymbianPropertyPage_S60V3_UID);
        s60v3UID = new Text(symbianUIDs, SWT.SINGLE | SWT.BORDER);
        s60v3UID.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        s60v3UID.setText(getUID(S60V3_UID));
        
        Listener validateListener = new ValidateListener();
		s60v2UID.addListener(SWT.Modify, validateListener);
		s60v3UID.addListener(SWT.Modify, validateListener);
        
        return main;
    }

    private MoSyncProject getProject() {
        IProject wrappedProject = (IProject) getElement();
        MoSyncProject project = MoSyncProject.create(wrappedProject);

        return project;
    }
    
    private String getUID(String property) {
        MoSyncProject project = getProject();
        String uid = project.getProperty(property);
        return uid == null ? "" : uid; //$NON-NLS-1$
    }


}