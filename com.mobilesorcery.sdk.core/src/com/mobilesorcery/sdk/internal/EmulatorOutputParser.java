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
package com.mobilesorcery.sdk.internal;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.SimpleQueue;
import com.mobilesorcery.sdk.core.Util;

/**
 * Class that parses binary data from the emulator.
 * 
 * @author fmattias
 */
public class EmulatorOutputParser {

	public static class ParseEvent {
		public int type;
		public String message;
		public int ip;
		public int[] stack;

		public ParseEvent(int type) {
			this(type, null, -1, null);
		}

		public ParseEvent(int type, int ip) {
			this(type, null, ip, null);
		}

		public ParseEvent(int type, String message, int ip, int[] stack) {
			this.type = type;
			this.message = message;
			this.ip = ip;
			this.stack = stack;
		}

		public String toString() {
			String stackStr = "";
			if (stack != null) {
				String[] iStack = new String[stack.length];
				for (int i = 0; i < stack.length; i++) {
					iStack[i] = Integer.toHexString(stack[i]);
				}
				stackStr = Util.join(iStack, ",");
			}

			return "TYPE: " + type + ", MESSAGE: " + message + ", IP: "
					+ Integer.toHexString(ip) + ", STACK: " + stackStr;
		}
	}

	public interface IParseEventHandler {
		public void handleEvent(ParseEvent event);
	}

	public static final int REPORT_STRING = 0x1;
	public static final int REPORT_IP = 0x2;
	public static final int REPORT_EXIT_STRING = 0x3;
	public static final int REPORT_LOAD_PROGRAM = 0x4;
	public static final int REPORT_CALL_STACK = 0x5;
	public static final int REPORT_RELOAD = 0x6;
	//public static final int CLOSE = 0xffff;

	private static final int MAX_BUFFER = 65536;

	private byte[] buffer;
	private int id;
	private IParseEventHandler handler;
	private SimpleQueue handlerQueue = new SimpleQueue(true);

	private boolean dieAfterNextReportIp = false;

	public EmulatorOutputParser(int id, IParseEventHandler handler) {
		this.id = id;
		this.handler = handler;
		buffer = new byte[MAX_BUFFER];
	}

	public void parse(InputStream input) throws IOException {
		try {
			while (parseNext(input)) {
				;
			}
		} catch (EOFException e) {
			// Ok.
		}
	}

	private boolean parseNext(InputStream input) throws IOException {
		int opcode = readInt(input);
		int size = readInt(input);

		if (size > MAX_BUFFER) {
			throw new IOException("Invalid emulator info");
		}

		switch (opcode) {
		case REPORT_STRING:
		case REPORT_EXIT_STRING:
			/* It is important to read the number of bytes specified in
			 * the message. Otherwise, the pipe that we read from will 
			 * be blocked by the remaining data and cause the writer to hang.
			 */
			readAllBytes(input, buffer, size);
			CoreMoSyncPlugin.getDefault().getEmulatorProcessManager().dataStreamed(id, buffer, 0, size);
			String message = new String(buffer, 0, size, "UTF8");
			handle(new ParseEvent(opcode, message, -1, null));
			break;
		case REPORT_IP:
			int ip = readInt(input);
			handle(new ParseEvent(REPORT_IP, ip));
			break;
		case REPORT_CALL_STACK:
			int stackDepth = size >> 2;
			int[] ips = new int[stackDepth];
			for (int i = 0; i < ips.length; i++) {
				ips[i] = readInt(input);
			}

			handle(new ParseEvent(REPORT_CALL_STACK, null, -1, ips));

		case REPORT_LOAD_PROGRAM:
		case REPORT_RELOAD:
			handle(new ParseEvent(opcode));
		}

		if (opcode == REPORT_EXIT_STRING) {
			dieAfterNextReportIp = true;
		}

		return opcode != REPORT_IP || !dieAfterNextReportIp;
	}

	private void handle(final ParseEvent event) {
		final IParseEventHandler handler = this.handler;
		if (handler != null) {
			handlerQueue.execute(new Runnable() {
				public void run() {
					if (handler != null) {
						handler.handleEvent(event);
					}
				}
			});
		}		
	}
	
	public void awaitParseEventsToBeHandled(int timeout) {
		handlerQueue.awaitShutdown(timeout);
	}

	private int readInt(InputStream input) throws IOException {
		return Util.readInt(input);
	}
	
	/**
	 * Blocks until the specified number of bytes has been read.
	 * 
	 * @param input       The input stream to read from.
	 * @param buffer      The buffer that will contain the read data.
	 * @param bytesToRead The total number of bytes to read.
	 * 
	 * @throws IOException If an internal error occurs.
	 */
	private void readAllBytes(InputStream input, byte[] buffer, int bytesToRead)
		throws IOException {
		int bytesLeft = bytesToRead;
		int offset = 0;
		
		while(bytesLeft > 0) {
			int bytesRead = input.read(buffer, offset, bytesLeft);
			if(bytesRead > 0) {
				bytesLeft -= bytesRead;
				offset += bytesRead;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		// String hex =
		// "0100000013000000456d756c61746f7220636f6e6e65637465642e0100000010000000457869742040204950203078363265310200000004000000e1620000"
		// ;
		String hex = "0100000013000000456d756c61746f7220636f6e6e65637465642e"
				+ "0300000010000000557365722050616e69633a2022525422"
				+ "010000000e0000004578697420402049502030783330"
				+ "020000000400000030000000";
		byte[] bytes = convertHex(hex);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		EmulatorOutputParser self = new EmulatorOutputParser(0, null);
		self.parse(bis);
	}

	private static byte[] convertHex(String hex) {
		char[] chars = hex.toCharArray();
		byte[] result = new byte[chars.length / 2];
		for (int i = 0; i < result.length; i++) {
			int value = Integer.parseInt("" + chars[2 * i] + chars[2 * i + 1],
					16);
			result[i] = (byte) value;
		}
		return result;
	}
}
