package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

/**
 * Pops up a dialog that can be used to select a bluetooth device in a list of
 * discovered bluetooth devices.
 * 
 * @author fmattias
 */
public class BluetoothDialog extends Dialog {
    private final static int REFRESH_ID = 0xff;

    private TableViewer deviceList;

    private boolean refreshInProgress = true;

    private ProgressBar scanProgress;

    private Label scanResults;

    private BluetoothDeviceDiscoverer discoverer;

    private BluetoothDevice selectedDevice;

    public BluetoothDialog(Shell parent) {
        super(parent);
        discoverer = new BluetoothDeviceDiscoverer(new UIUpdater(this));
    }

    public void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Discover Bluetooth Devices");
    }

    public int open() {
        discoverDevices();
        return super.open();
    }

    protected Control createDialogArea(Composite container) {
        Composite parent = (Composite) super.createDialogArea(container);

        Composite status = new Composite(parent, SWT.NONE);
        status.setLayout(new GridLayout(2, false));
        status.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        scanResults = new Label(status, SWT.NONE);
        scanResults.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        scanProgress = new ProgressBar(status, SWT.INDETERMINATE);
        GridData scanProgressData = new GridData(UIUtils.getDefaultFieldSize() / 2, SWT.DEFAULT);
        scanProgressData.horizontalAlignment = SWT.RIGHT;
        scanProgress.setLayoutData(scanProgressData);

        Composite deviceComposite = new Composite(parent, SWT.NONE);
        deviceList = new TableViewer(deviceComposite, SWT.BORDER | SWT.SINGLE);
        TableColumnLayout deviceListTableLayout = new TableColumnLayout();
        deviceComposite.setLayout(deviceListTableLayout);

        TableColumn column1 = new TableColumn(deviceList.getTable(), SWT.NONE);
        deviceListTableLayout.setColumnData(column1, new ColumnWeightData(70));
        
        deviceList.setLabelProvider(new BluetoothLabelProvider(deviceList));
        deviceList.setContentProvider(new ArrayContentProvider());
        deviceList.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                selectedDevice = (BluetoothDevice) ((IStructuredSelection) event.getSelection()).getFirstElement();
                updateUI();
            }
        });

        deviceList.getControl().addMouseListener(new MouseAdapter() {
            public void mouseDoubleClick(MouseEvent event) {
                // For some weird reason, adding a double click listener doesn't
                // work.
                if (selectedDevice != null) {
                    okPressed();
                }
            }
        });

        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = convertHeightInCharsToPixels(15);
        gd.widthHint = convertWidthInCharsToPixels(55);
        deviceComposite.setLayoutData(gd);
        
        Table table = deviceList.getTable();
        table.setFont(container.getFont());
        updateUI();
        return parent;
    }

    protected Control createButtonBar(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = convertHorizontalDLUsToPixels(7);
        layout.marginHeight = convertVerticalDLUsToPixels(7);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(4);
        layout.verticalSpacing = convertVerticalDLUsToPixels(4);

        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button refreshButton = createButton(composite, REFRESH_ID, "Refresh", false);
        setButtonLayout(refreshButton, SWT.LEFT);
        // ((GridData) refreshButton.getLayoutData()).horizontalIndent =
        // convertHorizontalDLUsToPixels(7);
        // ((GridData) refreshButton.getLayoutData()).verticalIndent =
        // convertHorizontalDLUsToPixels(4);

        Composite rightAligned = new Composite(composite, 0);
        GridLayout rightAlignedLayout = new GridLayout();
        rightAlignedLayout.marginWidth = 0;
        rightAlignedLayout.marginHeight = 0;
        rightAlignedLayout.horizontalSpacing = 0;
        rightAlignedLayout.verticalSpacing = layout.verticalSpacing;
        rightAligned.setLayout(rightAlignedLayout);

        setButtonLayout(createButton(rightAligned, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true), SWT.RIGHT);
        Button cancelButton = createButton(rightAligned, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        setButtonLayout(cancelButton, SWT.RIGHT);
        ((GridData) cancelButton.getLayoutData()).horizontalIndent = layout.horizontalSpacing;
        GridData rightAlignedData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        rightAlignedData.horizontalAlignment = SWT.RIGHT;
        rightAligned.setLayoutData(rightAlignedData);

        setRefreshInProgress(refreshInProgress);
        return composite;
    }

    protected void okPressed() {
        discoverer.cancelDiscovery();
        super.okPressed();
    }

    protected void cancelPressed() {
        discoverer.cancelDiscovery();
        super.cancelPressed();
    }

    protected void createButtonsForButtonBar(Composite parent) {
    }

    protected void setButtonLayout(Button button, int vAlignment) {
        setButtonLayoutData(button);
        ((GridData) button.getLayoutData()).verticalAlignment = vAlignment;
    }

    protected void buttonPressed(int buttonId) {
        if (buttonId == REFRESH_ID) {
            discoverDevices();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    public void setRefreshInProgress(final boolean refreshInProgress) {
        this.refreshInProgress = refreshInProgress;
        if (getShell() == null) {
            return;
        }

        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                updateUI();
            }
        });
    }

    protected void updateUI() {
        Button refreshButton = getButton(REFRESH_ID);
        if (refreshButton != null) {
            deviceList.setInput(discoverer.m_devices.toArray());
            deviceList.getControl().getParent().layout();
            refreshButton.setEnabled(!refreshInProgress);
            scanProgress.setVisible(refreshInProgress);
            updateScanResults();
            getButton(IDialogConstants.OK_ID).setEnabled(selectedDevice != null);
        }
    }

    protected void updateScanResults() {
        scanResults.setText(MessageFormat.format("Found {0} device(s)", discoverer.m_devices.size()));
    }

    /**
     * This function starts the discovery for devices. The function starts the
     * search in a new thread, and will return before all devices has been
     * found.
     */
    public void discoverDevices() {
        refreshInProgress = true;
        updateUI();
        discoverer.discoverDevices();
    }

    /**
     * Handles the event of a device being discovered.
     * 
     * @author fmattias
     */
    public class BluetoothDeviceDiscoverer implements DiscoveryListener {
        /**
         * List of devices that has been discovered so far.
         */
        private TreeSet<BluetoothDevice> m_devices;

        /**
         * Class called when a device is discovered.
         */
        private DeviceUpdate m_updater;

        private DiscoveryAgent agent;

        /**
         * 
         * @param updater
         *            Class that should be called when a device is found.
         */
        public BluetoothDeviceDiscoverer(DeviceUpdate updater) {
            clearDevices();
            m_updater = updater;
        }

        void clearDevices() {
            m_devices = new TreeSet<BluetoothDevice>(BluetoothDevice.COMPARATOR);
        }
        
        public void discoverDevices() {
            clearDevices();
            updateUI();

            /* Get local dongle and set up an agent for device discovery */
            LocalDevice dongle = null;
            try {
                dongle = LocalDevice.getLocalDevice();
                agent = dongle.getDiscoveryAgent();
                agent.startInquiry(DiscoveryAgent.GIAC, this);
            } catch (BluetoothStateException e) {
                setRefreshInProgress(false);
                Policy.getStatusHandler().show(new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID, e.getMessage()), "Could not scan for devices");
            }
        }

        public void cancelDiscovery() {
            if (agent != null) {
                new Thread(new Runnable() {
                    public void run() {
                        agent.cancelInquiry(BluetoothDeviceDiscoverer.this);
                    }
                }, "Cancel BT discovery");
            }
        }

        public void deviceDiscovered(RemoteDevice device, DeviceClass type) {
            BluetoothDevice btDevice = new BluetoothDevice(device, type);

            /* Only add devices that are not available */
            if (!m_devices.contains(btDevice)) {
                m_devices.add(btDevice);
                m_updater.deviceFound(btDevice);
            }
        }

        public void inquiryCompleted(int discType) {
            Set<BluetoothDevice> devices = m_devices;
            for (BluetoothDevice device : devices) {
                device.resolveFriendlyName(m_updater);
            }
            setRefreshInProgress(false);
        }

        public void serviceSearchCompleted(int arg0, int arg1) {
        }

        public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {
        }

        public ArrayList<BluetoothDevice> getDevices() {
            return new ArrayList<BluetoothDevice>(m_devices);
        }

    }

    /**
     * Interface used when a device is discovered.
     */
    public interface DeviceUpdate {
        /**
         * Is called once a device is discovered.
         * 
         * @param device
         *            The devices that has been discovered.
         */
        public void deviceFound(BluetoothDevice device);

        public void deviceUpdated(BluetoothDevice device);
    }

    /**
     * Updates the UI as soon as a device is discovered.
     */
    public class UIUpdater implements DeviceUpdate, Runnable {
        /**
         * Main widget that will be updated.
         */
        BluetoothDialog m_parent;

        public UIUpdater(BluetoothDialog parent) {
            m_parent = parent;
        }

        public void deviceFound(final BluetoothDevice device) {
            discoverer.m_devices.add(device);
            deviceUpdated(device);
        }

        public void deviceUpdated(BluetoothDevice device) {
            if (m_parent.getShell() != null) {
                m_parent.getShell().getDisplay().syncExec(this);
            }
        }

        public void run() {
            if (deviceList != null) {
                updateUI();
            }
        }
    }

    public BluetoothDevice getSelectedDevice() {
        return selectedDevice;
    }
}
