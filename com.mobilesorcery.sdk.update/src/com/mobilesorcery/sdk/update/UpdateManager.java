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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

/**
 * Handles downloading of new device profiles (and other stuff as well)
 * @author Mattias Bybro
 *
 */
public class UpdateManager extends UpdateManagerBase {

    private static final UpdateManager INSTANCE = new UpdateManager();

    // TODO: Should not be here...?
    private static final int HARDCODED_BASE_VERSION_CODE = 24;

    protected UpdateManager() {

    }

    public static UpdateManager getDefault() {
        return INSTANCE;
    }

    public boolean isUpdateAvailable() throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        addVersionInfo(params);
        addHalfHash(params);

        Response response = sendRequest(getRequestURL("updates/available", params)); //$NON-NLS-1$
        try {
            return getBooleanResponse(response, Messages.UpdateManager_ServerBouncedResendReq);
        } finally {
            response.close();
        }
    }

    public String getUpdateMessage() throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        addVersionInfo(params);
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

    public File getUpdateZip() {
        return MoSyncTool.getDefault().getMoSyncHome().append("update.zip").toFile(); //$NON-NLS-1$
    }

    public void downloadProfileUpdate(IProgressMonitor monitor) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        addVersionInfo(params);
        addHalfHash(params);

        File updateZipFile = getUpdateZip();
        OutputStream updateZip = null;

        try {
            Response response = sendRequest(getRequestURL("update", params)); //$NON-NLS-1$

            monitor.beginTask(Messages.UpdateManager_DownloadProgress, response.getContentLength());

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
            Util.safeClose(updateZip);
        }

        //monitor.setTaskName("Unpacking database...");

        //Util.unzip(updateZipFile, MoSyncTool.getDefault().getMoSyncHome().toFile());
        //MoSyncTool.getDefault().reinit();
    }

    private void addVersionInfo(Map<String, String> request) {
        request.put("revision", MoSyncTool.getDefault().getVersionInfo(MoSyncTool.BINARY_VERSION));
        request.put("baseversion", Integer.toString(HARDCODED_BASE_VERSION_CODE));
    }

    public void runUpdater(IProgressMonitor monitor) throws IOException {
        monitor.setTaskName(Messages.UpdateManager_RestartingProgress);

        // QUASI-HARD-CODED LOCATION!
        String absoluteMosyncExe = MoSyncTool.getDefault().getBinary("eclipse/mosync").toOSString();
        String pid = CoreMoSyncPlugin.getPid();
        String updaterExe = MoSyncTool.getDefault().getBinary("bin/updater").toOSString(); //$NON-NLS-1$

        if (!new File(absoluteMosyncExe).exists() || !new File(updaterExe).exists()) {
            throw new IOException(Messages.UpdateManager_UpdaterNotFoundError);
        }

        String args[] = new String[] {
        							Util.ensureQuoted(updaterExe),
        							pid,
        							absoluteMosyncExe
        						   };
        Process p = Runtime.getRuntime().exec(args, null, MoSyncTool.getMoSyncHomeFromEnv().toFile());
        //SpawnedProcess p = new SpawnedProcess(, pid + " " + relativeMosyncExe, );
        //System.err.println(p);
        //p.start();

        // Kill platform ui.
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
			public void run() {
                PlatformUI.getWorkbench().close();
            }
        });
    }

	public void clearRegistrationInfo() {
		MoSyncTool.getDefault().setProperty(MoSyncTool.EMAIL_PROP, null);
        MoSyncTool.getDefault().setProperty(MoSyncTool.USER_HASH_PROP, null);
        MoSyncTool.getDefault().setProperty(MoSyncTool.USER_HASH_PROP_2, null);
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
