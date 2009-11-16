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
package com.mobilesorcery.sdk.internal.debug;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.cdt.debug.mi.core.MIProcessAdapter;
import org.eclipse.core.runtime.IProgressMonitor;

public class MoSyncMIProcessAdapter extends MIProcessAdapter {

	private InputStream streamWrapper;

	class MoSyncInputStreamHook extends InputStream {

		private InputStream wrappedStream;

		public MoSyncInputStreamHook(InputStream wrappedStream) {
			this.wrappedStream = wrappedStream;
		}

		public int read() throws IOException {
			//System.err.println("#READ");
			int result = wrappedStream.read();
			//System.err.print((char) result);
			return result;
		}
		
		public int read(byte[] buffer, int offset, int len) throws IOException {			
			//System.err.println("#READ#BUF;" + buffer.length + ":" + offset + "-" + len);
			if (available() == 0) {
				len = 1;
			}
			int result = wrappedStream.read(buffer, offset, len);
			/*for (int i = offset; i < offset + result; i++) {
				System.err.print((char) buffer[i]);
			}*/
			
			return result;
		}
		
		public int available() throws IOException {
			int available = wrappedStream.available();
			//System.err.println("AVAILABLE:" + available);
			return available;
		}
		
	}
	
	public MoSyncMIProcessAdapter(String[] args, IProgressMonitor monitor)
			throws IOException {
		super(args, monitor);
	}

	public MoSyncMIProcessAdapter(String[] args, int launchTimeout,
			IProgressMonitor monitor) throws IOException {
		super(args, launchTimeout, monitor);
	}

	public InputStream getInputStream() {
		if (streamWrapper == null) {
			streamWrapper = wrapStream(super.getInputStream());
		}
		return streamWrapper;
	}

	private synchronized InputStream wrapStream(InputStream inputStream) {
		if (streamWrapper == null) {
			streamWrapper = new MoSyncInputStreamHook(inputStream);
		}
		
		return streamWrapper;
	}
}
