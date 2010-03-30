/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

public class UpdateManager {

    public static class UpdateException extends IOException {
        public UpdateException(String msg) {
            super(msg);
        }
    }
    
    private static final UpdateManager INSTANCE = new UpdateManager();

	private static final int UNVERSIONED = -1;

    class Response {
        private InputStream input;
        private int length;

        Response(InputStream input, int length) {
            this.input = input;
            this.length = length;
        }

        public InputStream getContent() {
            return input;
        }

        public int getContentLength() {
            return length;
        }

        public void close() {
            UpdateManager.this.close(input);
        }
    }

    private UpdateManager() {

    }

    public static UpdateManager getDefault() {
        return INSTANCE;
    }

    public int getCurrentBinaryVersion() throws IOException {
        return getCurrentVersionFromFile(MoSyncTool.getDefault().getMoSyncBin().append("version.dat").toFile()); //$NON-NLS-1$
    }

    public int getCurrentProfileVersion() throws IOException {
        return getCurrentVersionFromFile(MoSyncTool.getDefault().getProfilesPath().append("version.dat").toFile()); //$NON-NLS-1$
    }

    public int getCurrentVersionFromFile(File versionFile) throws IOException {
        if (versionFile.exists()) {
            return readVersion(versionFile);
        }

        return 0;
    }

    private int readVersion(File versionFile) throws IOException {
        FileReader input = new FileReader(versionFile);
        try {
            LineNumberReader lineInput = new LineNumberReader(input);
            String version = lineInput.readLine();
            if (version != null) {
                version = version.trim();
                return Integer.parseInt(version);
            }

            return 0;
        } catch (Exception e) {
        	return UNVERSIONED;
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    public boolean isUpdateAvailable() throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        addProfileVersion(params);
        addHalfHash(params);

        Response response = sendRequest(getRequestURL("currentProfile", params)); //$NON-NLS-1$
        try {
            return getBooleanResponse(response, Messages.UpdateManager_ServerBouncedResendReq);
        } finally {
            response.close();
        }
    }
    
    public boolean isRegistered() throws IOException {
        if (MoSyncTool.getDefault().getProperty("email") == null) { //$NON-NLS-1$
            return false;
        }
        
        return isRegistered(MoSyncTool.getDefault().getProperty("email"));  //$NON-NLS-1$
    }

    public boolean isRegistered(String email) throws IOException {        
        Map<String, String> params = new HashMap<String, String>();
        params.put("email", email); //$NON-NLS-1$
        addProfileVersion(params);
     
        Response response = sendRequest(getRequestURL("registered", params)); //$NON-NLS-1$
        
        try {
            return getBooleanResponse(response, Messages.UpdateManager_ServerBouncedRegReq);
        } finally {
            response.close();
        }
    }
    
    public boolean isConfirmed() throws IOException {
        if (MoSyncTool.getDefault().getProperty("email") == null) { //$NON-NLS-1$
            return false;
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("email", MoSyncTool.getDefault().getProperty("email")); //$NON-NLS-1$ //$NON-NLS-2$
     
        Response response = sendRequest(getRequestURL("confirmed", params)); //$NON-NLS-1$
        
        try {
            return getBooleanResponse(response, Messages.UpdateManager_ServerBouncedRegReq);
        } finally {
            response.close();
        }
    }
    
    public boolean isValid() throws IOException {
        if (MoSyncTool.getDefault().getProperty("email") == null) { //$NON-NLS-1$
            return false;
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("email", MoSyncTool.getDefault().getProperty("email")); //$NON-NLS-1$ //$NON-NLS-2$
        params.put("hash", getUserHash()); //$NON-NLS-1$
     
        Response response = sendRequest(getRequestURL("valid", params)); //$NON-NLS-1$
        
        try {
            return getBooleanResponse(response, Messages.UpdateManager_ServerBouncedRegReq);
        } finally {
            response.close();
        }
    }

    public String getUpdateMessage() throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        addProfileVersion(params);
        addHalfHash(params);

        Response response = sendRequest(getRequestURL("updateMessage", params)); //$NON-NLS-1$
        try {
            StringBuffer result = new StringBuffer();
            InputStream content = response.getContent();
            for (int read = content.read(); read != -1; read = content.read()) {
                result.append((char) read);
            }

            return result.toString();
        } finally {
            response.close();
        }
    }
    
    public boolean resend() throws MalformedURLException, IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("email", MoSyncTool.getDefault().getProperty("email")); //$NON-NLS-1$ //$NON-NLS-2$
        addProfileVersion(params);
     
        Response response = sendRequest(getRequestURL("requestCode", params)); //$NON-NLS-1$
        
        try {
            return getBooleanResponse(response, Messages.UpdateManager_ServerBouncedResendReq);
        } finally {
            response.close();
        }
    }

    public void downloadProfileUpdate(IProgressMonitor monitor) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        addProfileVersion(params);
        addHalfHash(params);

        File updateZipFile = MoSyncTool.getDefault().getMoSyncHome().append("update.zip").toFile(); //$NON-NLS-1$
        OutputStream updateZip = null;

        try {
            Response response = sendRequest(getRequestURL("update", params)); //$NON-NLS-1$

            monitor.beginTask(Messages.UpdateManager_DownloadProgress, response.length);

            byte[] buffer = new byte[2048];
            updateZip = new FileOutputStream(updateZipFile);

            InputStream content = response.getContent();
            for (int read = content.read(buffer); read != -1; read = content.read(buffer)) {
                if (monitor.isCanceled()) {
                    return;
                }
                updateZip.write(buffer, 0, read);
                monitor.worked(read);
            }
        } finally {
            close(updateZip);
        }
        
        //monitor.setTaskName("Unpacking database...");
        
        //Util.unzip(updateZipFile, MoSyncTool.getDefault().getMoSyncHome().toFile());
        //MoSyncTool.getDefault().reinit();
    }

    public void runUpdater(IProgressMonitor monitor) throws IOException {
        monitor.setTaskName(Messages.UpdateManager_RestartingProgress);
        
        // QUASI-HARD-CODED LOCATION!
        String relativeMosyncExe = "./eclipse/mosync.exe"; //$NON-NLS-1$
        String absoluteMosyncExe = MoSyncTool.getMoSyncHomeFromEnv().append(relativeMosyncExe).toOSString();
        String pid = CoreMoSyncPlugin.getPid();
        String updaterExe = MoSyncTool.getMoSyncHomeFromEnv().append("bin/updater.exe").toOSString(); //$NON-NLS-1$
        
        if (!new File(absoluteMosyncExe).exists() || !new File(updaterExe).exists()) {
            throw new IOException(Messages.UpdateManager_UpdaterNotFoundError);
        }
        
        Process p = Runtime.getRuntime().exec(new String[] { Util.ensureQuoted(updaterExe), pid, relativeMosyncExe }, null, MoSyncTool.getMoSyncHomeFromEnv().toFile());
        //SpawnedProcess p = new SpawnedProcess(, pid + " " + relativeMosyncExe, );
        //System.err.println(p);
        //p.start();
        
        // Kill platform ui.
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            public void run() {
                PlatformUI.getWorkbench().close();
            }            
        });
    }
    
    public boolean registerMe(String email, String name, boolean addToMailingList) throws MalformedURLException, IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("email", email); //$NON-NLS-1$
        params.put("name", name); //$NON-NLS-1$
        params.put("mailinglist", addToMailingList ? "1" : "0"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addProfileVersion(params);

        Response response = sendRequest(getRequestURL("register", params)); //$NON-NLS-1$
        try {
            return getBooleanResponse(response, Messages.UpdateManager_ServerBouncedRegReq1);
        } finally {
            response.close();
        }
    }
    
    private boolean getBooleanResponse(Response response, String errmsg) throws IOException {
        int result = response.getContent().read();
        if (result == '0') {
            return false;
        } else if (result == '1') {
            return true;
        } else {
            throw new UpdateException(errmsg);
        }        
    }

    void close(OutputStream output) {
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
    }

    void close(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
    }

    public String getUserHash() {
        return MoSyncTool.getDefault().getProperty(MoSyncTool.USER_HASH_PROP);
    }

    public void setUserHash(String hash) {
        hash = hash == null ? null : hash.trim();
        MoSyncTool.getDefault().setProperty(MoSyncTool.USER_HASH_PROP, hash);
    }

    public String getUserHalfHash() {
        String hash = getUserHash();
        if (hash != null) {
            return hash.substring(0, hash.length() / 2);
        }

        return null;
    }
    
	public void clearRegistrationInfo() {
		MoSyncTool.getDefault().setProperty(MoSyncTool.EMAIL_PROP, null);
		MoSyncTool.getDefault().setProperty(MoSyncTool.USER_HASH_PROP, null);
	}


    private void addProfileVersion(Map<String, String> request) throws IOException {
        request.put("version", Integer.toString(getCurrentProfileVersion())); //$NON-NLS-1$
        request.put("type", "3"); // 'Magic string' //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void addHalfHash(Map<String, String> request) {
        request.put("hhash", getUserHalfHash()); //$NON-NLS-1$
    }

    private Response sendRequest(URL url) throws IOException {
        if (CoreMoSyncPlugin.getDefault().isDebugging()) {
        	CoreMoSyncPlugin.trace(url);
        }

        URLConnection connection = url.openConnection();
        int length = connection.getContentLength();
        InputStream input = connection.getInputStream();
        return new Response(input, length);
    }

    private String getServiceURL(String serviceName) {
        return "http://api.mosync.com/" + serviceName + "/index.php"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private URL getRequestURL(String serviceName, Map<String, String> params) throws MalformedURLException {
        String service = getServiceURL(serviceName);

        StringBuffer paramsStr = new StringBuffer();
        if (params != null && !params.isEmpty()) {
            paramsStr.append("?"); //$NON-NLS-1$
            int paramCnt = 0;
            for (Map.Entry<String, String> param : params.entrySet()) {
                paramCnt++;
                if (paramCnt > 1) {
                    paramsStr.append("&"); //$NON-NLS-1$
                }

                paramsStr.append(URLEncoder.encode(param.getKey()) + "=" + URLEncoder.encode(param.getValue())); //$NON-NLS-1$
            }
        }

        return new URL(service + paramsStr);
    }

    public static void main(String[] args) throws Exception {
        /*System.err.println(UpdateManager.getDefault().getCurrentBinaryVersion());
        System.err.println(UpdateManager.getDefault().getCurrentProfileVersion());
        System.err.println(MoSyncTool.getDefault().getProperty(MoSyncTool.USER_HASH_PROP));
        System.err.println(UpdateManager.getDefault().isUpdateAvailable());
        System.err.println(UpdateManager.getDefault().getUpdateMessage());
        System.err.println(UpdateManager.getDefault().isRegistered());
        System.err.println(UpdateManager.getDefault().isConfirmed());
        System.err.println(UpdateManager.getDefault().isValid());*/
        //System.err.println(UpdateManager.getDefault().resend());
        UpdateManager.getDefault().downloadProfileUpdate(new NullProgressMonitor());
        
    }



}
