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
package com.mobilesorcery.sdk.testing.emulator;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.xml.sax.Attributes;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IEmulatorProcessListener;
import com.mobilesorcery.sdk.testing.IRelaunchableTestSession;
import com.mobilesorcery.sdk.testing.ITestRunner;
import com.mobilesorcery.sdk.testing.ITestSession;
import com.mobilesorcery.sdk.testing.ITestSuite;
import com.mobilesorcery.sdk.testing.Test;
import com.mobilesorcery.sdk.testing.TestResult;
import com.mobilesorcery.sdk.testing.TestSession;
import com.mobilesorcery.sdk.testing.TestSessionEvent;
import com.mobilesorcery.sdk.testing.TestSuite;

public class EmulatorTestRunner implements ITestRunner {

	private static final byte[] TEST_MARKUP_PREFIX = "__TEST_MARKUP__".getBytes();
	private static final int TEST_MARKUP_PREFIX_LEN = TEST_MARKUP_PREFIX.length;
	
	final static String FILE_ATTR = "file";			
	final static String LINE_ATTR = "line";			
	
	public final static String FILE_KEY = FILE_ATTR;			
	public final static String LINE_KEY = LINE_ATTR;			
	
	public class EmulatorTestSession extends TestSession implements IEmulatorProcessListener, IRelaunchableTestSession {

		class TestParser extends DefaultHandler {	
			//final static String DEFINE_TEST = "define-test";
			//final static String DEFINE_SUITE = "define-suite";
			final static String TEST_RESULT = "test";
			final static String SUITE_RESULT = "suite";
			final static String SUCCESS_ATTR = "success";
			final static String NAME_ATTR = "name";
			final static String TIME_ATTR = "time";
			final static String LOCATION = "location";			

			private ITestSession session;
			private ITestSuite currentSuite;
			private StringBuffer currentTestMessage;
			private Test currentTest;
			private boolean currentTestState;
			private int currentTestTime;
			
			public TestParser(TestSession session) {
				this.session = session;
			}
			
			public void parse(final InputStream input) {
				Thread thread = new Thread(new Runnable() {
					public void run() {
						try {
							SAXParserFactory spf = SAXParserFactory.newInstance();
							spf.setValidating(false);
							spf.setNamespaceAware(true);
							SAXParser sp = spf.newSAXParser();
							final XMLReader xr = sp.getXMLReader();
							xr.setContentHandler(TestParser.this);
							xr.parse(new InputSource(input));
						} catch (Exception e) {
							reportSessionError(e);
						}
					}					
				});
				
				thread.start();
			}
			
			public void startElement(String uri, String name, String qName, Attributes atts) {
				String nameAttr = atts.getValue(NAME_ATTR);
				if (SUITE_RESULT.equals(name)) {
					currentSuite = new TestSuite(nameAttr);
					session.addTest(currentSuite);
					// No test result is reported for suites, but for individual tests
				} else if (TEST_RESULT.equals(name)) {					
					Test test = new Test(nameAttr);

					currentTest = test;
					currentTestMessage = new StringBuffer();
					currentTestTime = 0;
					currentTestState = true;
					
					if (currentSuite == null) {
						throw new IllegalStateException("Internal error: A test must belong to a suite");
					}
					currentSuite.addTest(test);
					session.getTestResult().startTest(test);
					
					String successStr = atts.getValue(SUCCESS_ATTR);
					boolean success = Boolean.parseBoolean(successStr);
					currentTestState = success;
					
					String testTimeStr = atts.getValue(TIME_ATTR);
					try {
						currentTestTime = testTimeStr == null ? TestResult.TIME_UNDEFINED : Integer.parseInt(testTimeStr);
					} catch (NumberFormatException e) {
						currentTestTime = TestResult.TIME_UNDEFINED;						
					}
				} else if (LOCATION.equals(name)) {
					String line = atts.getValue(LINE_ATTR);
					String file = atts.getValue(FILE_ATTR);
					session.getTestResult().setProperty(currentTest, LINE_ATTR, line);
					session.getTestResult().setProperty(currentTest, FILE_ATTR, file);
				}
			}
			
			public void endElement(String uri, String name, String qName) throws SAXException {
				if (TEST_RESULT.equals(name)) {
					session.getTestResult().endTest(currentTest, currentTestTime);
					if (!currentTestState) {
						String errorMessage = currentTestMessage.toString();
						session.getTestResult().addFailure(currentTest, errorMessage);
					}
				}
			}
			
			public void characters(char ch[], int start, int length) {
				currentTestMessage.append(ch, start, length);
			}
		}

		private boolean parserStarted;
		private PipedOutputStream buffer;
		private ILaunchConfiguration launchConfig;
		
		public EmulatorTestSession(String name, ILaunchConfiguration launchConfig) {
			super(name);
			this.launchConfig = launchConfig;
		}

		public void start() {
			checkStartable();
			CoreMoSyncPlugin.getDefault().getEmulatorProcessManager().addEmulatorProcessListener(emulatorId, this);
			// Expects it to be started externally...
			notifyListeners(new TestSessionEvent(TestSessionEvent.SESSION_STARTED, this, this));
		}
		
		public void finish() {
			CoreMoSyncPlugin.getDefault().getEmulatorProcessManager().removeEmulatorProcessListener(this);
		}
		
		public void dataStreamed(int id, byte[] data, int offset, int length) {
			try {
				if (length < TEST_MARKUP_PREFIX_LEN) {
					return;
				}
				
				for (int i = 0; i < TEST_MARKUP_PREFIX_LEN; i++) {
					if (TEST_MARKUP_PREFIX[i] != data[offset + i]) {
						return;
					}
				}
				
				feedIntoParser(data, offset + TEST_MARKUP_PREFIX_LEN, length - TEST_MARKUP_PREFIX_LEN);
			} catch (IOException e) {
				reportSessionError(e);
			}
		}

		private void feedIntoParser(byte[] data, int offset, int length) throws IOException {
			if (!parserStarted) {
				parserStarted = true;
				TestParser parser = new TestParser(this);
				PipedInputStream input = new PipedInputStream();
				buffer = new PipedOutputStream();
				buffer.connect(input);
				parser.parse(input);
			}
			
			buffer.write(data, offset, length);
		}

		public void processStarted(int id) {
		}

		public void processStopped(int id) {
			try {
				buffer.close();
			} catch (IOException e) {
				reportSessionError(e);
			}
		}

		private void reportSessionError(Exception e) {
			e.printStackTrace();	
		}

		public ILaunchConfiguration getLaunchConfiguration() {
			return launchConfig;
		}

	}

	private ILaunchConfiguration launchConfig;
	private int emulatorId;

	public EmulatorTestRunner(ILaunchConfiguration config, String name, int emulatorId) {
		this.launchConfig = config;		
		this.emulatorId = emulatorId;
	}


	public ITestSession createTestSession() {
		EmulatorTestSession session = new EmulatorTestSession(launchConfig.getName(), launchConfig);
		return session;
	}


	@Override
	public void run(ITestSession session) {
		// TODO Auto-generated method stub
		
	}

}
