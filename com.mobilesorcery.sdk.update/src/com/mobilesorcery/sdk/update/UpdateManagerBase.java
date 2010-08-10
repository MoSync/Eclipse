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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;

/*
 * Refactoring note: this class is there just to help me implement
 * the 'new' registration process - might want to remove this later.
 */
public abstract class UpdateManagerBase {
    
    public static class UpdateException extends IOException {
        public UpdateException(String msg) {
            super(msg);
        }
    }
    
    protected class Response {
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
            UpdateManagerBase.this.close(input);
        }
    }
    
    protected Response sendRequest(URL url) throws IOException {
        if (CoreMoSyncPlugin.getDefault().isDebugging()) {
            CoreMoSyncPlugin.trace(url);
        }

        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(30000);
        int length = connection.getContentLength();
        InputStream input = connection.getInputStream();
        return new Response(input, length);
    }

    protected String getServiceURL(String serviceName) {
        String baseURL = MoSyncTool.getDefault().getProperty("update-baseurl"); //$NON-NLS-1$ 
        if (baseURL == null) {
            baseURL = "http://api.mosync.com/{0}"; //$NON-NLS-1$ 
        }
        return MessageFormat.format(baseURL, serviceName).trim();
    }

    protected URL getRequestURL(String serviceName, Map<String, String> params) throws MalformedURLException {
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
    
    protected boolean getBooleanResponse(Response response, String errmsg) throws IOException {
        int result = response.getContent().read();
        if (result == '0') {
            return false;
        } else if (result == '1') {
            return true;
        } else {
            throw new UpdateException(errmsg);
        }        
    }
     
    public String getUserHash() {
        return MoSyncTool.getDefault().getProperty(MoSyncTool.USER_HASH_PROP_2);
    }

    public void setUserHash(String hash) {
        hash = hash == null ? null : hash.trim();
        MoSyncTool.getDefault().setProperty(MoSyncTool.USER_HASH_PROP_2, hash);
    }

    public String getUserHalfHash() {
        String hash = getUserHash();
        if (hash != null) {
            return hash.substring(0, hash.length() / 2);
        }

        return null;
    }
    
    public boolean shouldPerformAutoUpdate() {
        IPreferenceStore prefStore = MosyncUpdatePlugin.getDefault().getPreferenceStore();
        prefStore.setDefault(MoSyncTool.AUTO_UPDATE_PREF, true);
        
        boolean shouldPerformAutoUpdate = MoSyncTool.getDefault().isValid() && prefStore.getBoolean(MoSyncTool.AUTO_UPDATE_PREF);
        return shouldPerformAutoUpdate;
    }

    protected void close(OutputStream output) {
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
    }

    protected void close(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
    }
}
