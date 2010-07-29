package com.mobilesorcery.sdk.ui.internal.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;

public class PermissionsPropertyPage extends MoSyncPropertyPage {

    public class PermissionsContentProvider implements ITreeContentProvider {
        private IApplicationPermissions permissions;

        public PermissionsContentProvider(IApplicationPermissions permissions) {
            this.permissions = permissions;
        }
        
        public Object[] getChildren(Object element) {
            return permissions.getAvailablePermissions(element == null ? null : element.toString()).toArray();
        }

        public Object getParent(Object element) {
            return null;
        }

        public boolean hasChildren(Object element) {
            return getChildren(element).length > 0;
        }

        public Object[] getElements(Object input) {
            return getChildren(null);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object obj, Object obj1) {
        }

    }

    private CheckboxTreeViewer permissionsList;
    private IApplicationPermissions permissionsWorkingCopy;
    private IApplicationPermissions permissions;

    protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));
        
        Label permissionsLabel = new Label(main, SWT.NONE);
        permissionsLabel.setText("&Select permissions for this project");
        
        permissionsList = new CheckboxTreeViewer(main);
        permissionsList.setLabelProvider(new LabelProvider() {
            public String getText(Object element) {
                String permission = (String) element;
                Path permissionPath = new Path(permission);
                return permissionPath.lastSegment();
            }
        });
        permissions = getProject().getPermissions();
        permissionsWorkingCopy = permissions.createWorkingCopy();
        permissionsList.setContentProvider(new PermissionsContentProvider(permissionsWorkingCopy));
        permissionsList.setCheckStateProvider(new ICheckStateProvider() {
            public boolean isGrayed(Object element) {
                String permission = (String) element;
                TreeSet<String> available = new TreeSet<String>(permissionsWorkingCopy.getAvailablePermissions(permission));
                if (available.isEmpty()) {
                    return false;
                }
                int availablePermissionCount = available.size();
                available.removeAll(permissionsWorkingCopy.getRequiredPermissions());
                int notRequiredPermissionCount = available.size();
                boolean shouldBeGrayed = availablePermissionCount != notRequiredPermissionCount && notRequiredPermissionCount > 0;

                return shouldBeGrayed;
            }
            
            public boolean isChecked(Object element) {
                String permission = (String) element;
                return isGrayed(element) || permissionsWorkingCopy.isPermissionRequired(permission);
            }
        });

        setPermissionsWorkingCopy(permissionsWorkingCopy);
        permissionsList.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        
        permissionsList.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                String permission = (String) event.getElement();
                permissionsWorkingCopy.setRequiredPermission(permission, event.getChecked());
                permissionsList.refresh(true);
            }
        });
        return main;
    }

    private void setPermissionsWorkingCopy(IApplicationPermissions permissionsWorkingCopy) {
        this.permissionsWorkingCopy = permissionsWorkingCopy;
        permissionsList.setInput(permissionsWorkingCopy);
        permissionsList.expandAll();
    }

    public void performDefaults() {
        setPermissionsWorkingCopy(CoreMoSyncPlugin.getDefault().getDefaultPermissions(getProject()));
    }
    
    public boolean performOk() {
        permissions.apply(permissionsWorkingCopy);
        return true;
    }
}
