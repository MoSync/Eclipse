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
