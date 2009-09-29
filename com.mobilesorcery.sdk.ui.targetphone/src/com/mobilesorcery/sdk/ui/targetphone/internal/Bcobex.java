package com.mobilesorcery.sdk.ui.targetphone.internal;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.mobilesorcery.sdk.core.Util;

/**
 * 
 * Minimal Services Search example.
 */
class ServiceSearch {

    static final UUID OBEX_OBJECT_PUSH = new UUID("1105", true);
    static int port = -1;

    public static int search(String address) throws Exception {

        final Object serviceSearchCompletedEvent = new Object();

        DiscoveryListener listener = new DiscoveryListener() {
            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
            }

            public void inquiryCompleted(int discType) {
            }

            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
                for (int i = 0; i < servRecord.length; i++) {
                    String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                    if (url == null) {
                        continue;
                    }
                    port = Integer.parseInt(url.substring(22, url.indexOf(';', 22)));
                    System.out.println("port found: " + port);
                }
            }

            public void serviceSearchCompleted(int transID, int respCode) {
                System.out.println("service search completed.");
                synchronized (serviceSearchCompletedEvent) {
                    serviceSearchCompletedEvent.notifyAll();
                }
            }
        };

        UUID[] searchUuidSet = new UUID[] { OBEX_OBJECT_PUSH };
        // int[] attrIDs = new int[] {};

        class MyRemoteDevice extends RemoteDevice {
            public MyRemoteDevice(String address) {
                super(address);
            }
        }
        ;
        RemoteDevice btDevice = new MyRemoteDevice(address);

        synchronized (serviceSearchCompletedEvent) {
            System.out.println("search services on " + btDevice.getBluetoothAddress() + " " + btDevice.getFriendlyName(false));
            LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(null, searchUuidSet, btDevice, listener);
            serviceSearchCompletedEvent.wait();
            if (port == -1)
                throw new IOException("No service port found");
            return port;
        }
    }
}

public class Bcobex {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            System.out.println("Usage: bcobex <address> <file>\n" + "\n" + "Sends file to bluetooth address.\n"
                    + "Address format: %012x\n" + "\n" + "Example: bcobex 012345689ABC someApp.jar\n" + "\n");
            return;
        }
        String address = args[0];
        String filename = args[1];
        int port = ServiceSearch.search(address);
        sendObexFile(address, port, filename);
    }

    public static void sendObexFile(String address, int port, String filename) throws IOException {
        sendObexFile(address, port, new File(filename), new NullProgressMonitor());
    }

    public static void sendObexFile(String address, int port, File file, IProgressMonitor monitor) throws IOException {
        // MB: Added eclipse-specific code 080815.
        String url = "btgoep://" + address + ":" + port;

        if (!file.exists()) {
            throw new IOException("Could not find file");
        }

        if (file.isDirectory()) {
            throw new IOException("File is a directory");
        }

        OutputStream os = null;
        InputStream fileInput = null;
        Operation putOperation = null;
        ClientSession clientSession = null;

        try {
            long length = file.length();
            monitor.beginTask("Sending file", (int) file.length());

            fileInput = new FileInputStream(file);

            monitor.setTaskName(MessageFormat.format("Connecting to {0}", url));

            clientSession = (ClientSession) Connector.open(url);
            HeaderSet hsConnectReply = clientSession.connect(null);
            if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
                throw new IOException("OBEX failed to connect");
            }
            monitor.setTaskName("Connected.");

            HeaderSet head = clientSession.createHeaderSet();
            head.setHeader(HeaderSet.NAME, file.getName());
            head.setHeader(HeaderSet.LENGTH, length);

            // Create PUT Operation
            putOperation = clientSession.put(head);

            // Send data to server
            os = putOperation.openOutputStream();
            byte[] buffer = new byte[788]; // Just an unusual number...
            int totalRead = 0;
            for (int read = fileInput.read(buffer); !monitor.isCanceled() && read != -1; read = fileInput.read(buffer)) {
                totalRead += read;
                monitor.setTaskName(MessageFormat.format("Sending {0} of {1}", Util.dataSize(totalRead), Util.dataSize(length)));
                monitor.worked(read);
                os.write(buffer, 0, read);
            }

        } finally {
            if (os != null) {
                os.close();
            }
            if (fileInput != null) {
                fileInput.close();
            }
            if (putOperation != null) {
                putOperation.close();
            }
            if (clientSession != null) {
                clientSession.disconnect(null);
                clientSession.close();
            }
        }

        monitor.setTaskName("Sent file to device");
        monitor.done();
    }

    public static void sendObexFile(TargetPhone phone, File file, IProgressMonitor monitor) throws IOException {
        sendObexFile(phone.getAddress(), phone.getPort(), file, monitor);
    }
}
