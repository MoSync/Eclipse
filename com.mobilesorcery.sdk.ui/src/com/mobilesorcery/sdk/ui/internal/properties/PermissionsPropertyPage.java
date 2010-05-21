package com.mobilesorcery.sdk.ui.internal.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;

public class PermissionsPropertyPage extends MoSyncPropertyPage {

    public class PermissionsContentProvider implements ITreeContentProvider {
        private IApplicationPermissions permissions;

        public PermissionsContentProvider(IApplicationPermissions permissions) {
            this.permissions = permissions;
        }
        
        public Object[] getChildren(Object obj) {
            return new Object[0];
        }

        public Object getParent(Object obj) {
            return null;
        }

        public boolean hasChildren(Object obj) {
            return false;
        }

        public Object[] getElements(Object input) {
            return permissions.getAvailablePermissions().toArray();
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object obj, Object obj1) {
        }

    }

    private CheckboxTreeViewer permissionsList;
    private IApplicationPermissions permissions;

    protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));
        
        Label permissionsLabel = new Label(main, SWT.NONE);
        permissionsLabel.setText("&Select permissions for this project");
        
        permissionsList = new CheckboxTreeViewer(main);
        permissionsList.setLabelProvider(new LabelProvider());
        permissions = getProject().getPermissions();
        permissionsList.setContentProvider(new PermissionsContentProvider(permissions));
        permissionsList.setInput(permissions);
        initUI();
        permissionsList.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        
        return main;
    }

    private void initUI() {
        permissionsList.setCheckedElements(permissions.getRequiredPermissions().toArray());
    }

    public void performDefaults() {
        initUI();
    }
    
    public boolean performOk() {
        Object[] newPermissions = permissionsList.getCheckedElements();
        List<String> required = new ArrayList<String>();
        for (int i = 0; i < newPermissions.length; i++) {
            String permission = (String) newPermissions[i];
            required.add(permission);
        }
        
        permissions.setRequiredPermissions(required);
        return true;
    }
}
