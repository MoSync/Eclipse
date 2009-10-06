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
package com.mobilesorcery.sdk.builder.s60;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;

public class DefaultKeyInitializer implements PropertyChangeListener {

	public static final String DEFAULT_KEY_PATH = "etc/default.key"; //$NON-NLS-1$
	
	public static final String DEFAULT_CERT_PATH = "etc/default.cert"; //$NON-NLS-1$
	
	public static final String DEFAULT_PASS_KEY = "default"; //$NON-NLS-1$

	public class PipeToFile implements ILineHandler {
		private Writer output = null;
		private boolean stoppedwriting = false;
		
		PipeToFile(File file) throws IOException {
			output = new FileWriter(file);
		}
		
		public void newLine(String line) {
			if (stoppedwriting) {
				return;
			}

			try {
				output.write(line);
				output.write('\n');
			} catch (Exception e) {
				stoppedwriting = true;
			}
		}

		public void stop(IOException e) {
			if (output != null) {
				try {
					output.flush();
					output.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}

		public void close() {
			stop(null);
		}		
	}
	
	public DefaultKeyInitializer() {
		MoSyncTool.getDefault().addPropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (MoSyncTool.MOSYNC_HOME_UPDATED == event.getPropertyName()) {
			startInitializerJob(MoSyncTool.getDefault().getMoSyncHome());
		}
	}

	public void startInitializerJob() {
		startInitializerJob(MoSyncTool.getDefault().getMoSyncHome());
	}
	
	private void startInitializerJob(IPath moSyncHome) {
		Job initializerJob = new Job(Messages.DefaultKeyInitializer_InitializingDefaultKeys) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (!MoSyncTool.getDefault().isValid()) {
						return Status.OK_STATUS;						
					}

					IPath openssl = MoSyncTool.getDefault().getMoSyncBin().append("openssl"); //$NON-NLS-1$
					File keyFile = MoSyncTool.getDefault().getMoSyncHome().append(DEFAULT_KEY_PATH).toFile();
					File certFile = MoSyncTool.getDefault().getMoSyncHome().append(DEFAULT_CERT_PATH).toFile();

					boolean exists = keyFile.exists() && certFile.exists();
					if (exists) {
						return Status.OK_STATUS;
					}

					if (!keyFile.getParentFile().exists()) {
						keyFile.getParentFile().mkdirs();
					}
					
					IProcessConsole console = CoreMoSyncPlugin.getDefault().createConsole(Messages.DefaultKeyInitializer_ConsoleName);

					String[] keyCmdLine = new String[] { openssl.toOSString(),
							"genrsa", "-rand", "-des", "-passout", "pass:" + DEFAULT_PASS_KEY, "1024" };					 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					console.addMessage(Util.join(Util.ensureQuoted(keyCmdLine), " ")); //$NON-NLS-1$
					Process keyProcess = Runtime.getRuntime().exec(keyCmdLine);
					PipeToFile keyFilePipe = new PipeToFile(keyFile);
					console.attachProcess(keyProcess, keyFilePipe, null);					
					keyProcess.waitFor();
					keyFilePipe.close();
					
					IPath cfg = MoSyncTool.getDefault().getMoSyncBin().append("openssl.cnf"); //$NON-NLS-1$
					
					String[] certCmdLine = new String[] { openssl.toOSString(),
							"req", "-new", "-x509", "-nodes", "-sha1", "-days", "3650", "-key", keyFile.getAbsolutePath(), "-batch", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
							"-config", cfg.toOSString() }; //$NON-NLS-1$
					console.addMessage(Util.join(Util.ensureQuoted(certCmdLine), " ")); //$NON-NLS-1$
					Process certProcess = Runtime.getRuntime().exec(certCmdLine);
					
					PipeToFile certFilePipe = new PipeToFile(certFile);
					console.attachProcess(certProcess, certFilePipe, null);
					certProcess.waitFor();
					certFilePipe.close();
					
					return Status.OK_STATUS;
				} catch (Exception e) {
					return new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage());
				}
			}			
		};
		
		initializerJob.setUser(true);
		initializerJob.schedule();
	}

	private static File getDefaultFile(String path) {
        if (!MoSyncTool.getDefault().isValid()) {
            return null;
        }
        File file = MoSyncTool.getDefault().getMoSyncHome().append(path).toFile();
        return file;
	}	

    public static File getDefaultCertFile() {
        return getDefaultFile(DEFAULT_CERT_PATH);
    }

    public static File getDefaultKeyFile() {
        return getDefaultFile(DEFAULT_KEY_PATH);
    }
	
	public void dispose() {
		MoSyncTool.getDefault().removePropertyChangeListener(this);
	}
	
}
