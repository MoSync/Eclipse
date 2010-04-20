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
/**
 * 
 */
package com.mobilesorcery.sdk.testing.emulator;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.mobilesorcery.sdk.testing.AssertionFailed;
import com.mobilesorcery.sdk.testing.ITestSession;
import com.mobilesorcery.sdk.testing.ITestSuite;
import com.mobilesorcery.sdk.testing.Test;
import com.mobilesorcery.sdk.testing.TestResult;
import com.mobilesorcery.sdk.testing.TestSession;
import com.mobilesorcery.sdk.testing.TestSuite;

class XMLTestReportParser extends DefaultHandler {
	/**
	 * 
	 */
	private final EmulatorTestSession emulatorTestSession;
	final static String FILE_ATTR = "file";			
	final static String LINE_ATTR = "line";			

	//final static String DEFINE_TEST = "define-test";
	//final static String DEFINE_SUITE = "define-suite";
	final static String TEST_RESULT = "test";
	final static String SUITE_RESULT = "suite";
	final static String NAME_ATTR = "name";
	final static String TIME = "time";
	final static String ELAPSED_ATTR = "elapsed";
	final static String LOCATION = "location";	
	final static String EXPECTED = "expected";
	final static String ACTUAL = "actual";

	private ITestSession session;
	private ITestSuite currentSuite;
	private StringBuffer currentTestMessage;
	private Test currentTest;
	private int currentTestTime;
	private String currentTag = null;
	private AssertionFailed currentAssertionFailed;
	
	public XMLTestReportParser(EmulatorTestSession emulatorTestSession, TestSession session) {
		this.emulatorTestSession = emulatorTestSession;
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
					xr.setContentHandler(XMLTestReportParser.this);
					xr.parse(new InputSource(input));
				} catch (Exception e) {
					XMLTestReportParser.this.emulatorTestSession.reportSessionError(e);
				}
			}					
		});
		
		thread.start();
	}
	
	public void startElement(String uri, String name, String qName, Attributes atts) {
		currentTag = name;
		
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
			
			if (currentSuite == null) {
				throw new IllegalStateException("Internal error: A test must belong to a suite");
			}
			currentSuite.addTest(test);
			session.getTestResult().startTest(test);			
		} else if (LOCATION.equals(name)) {
			String line = atts.getValue(LINE_ATTR);
			String file = atts.getValue(FILE_ATTR);
			session.getTestResult().setProperty(currentTest, LINE_ATTR, line);
			session.getTestResult().setProperty(currentTest, FILE_ATTR, file);
		} else if (TIME.equals(name)) {
			String testTimeStr = atts.getValue(ELAPSED_ATTR);
			try {
				currentTestTime = testTimeStr == null ? TestResult.TIME_UNDEFINED : Integer.parseInt(testTimeStr);
			} catch (NumberFormatException e) {
				currentTestTime = TestResult.TIME_UNDEFINED;						
			}
		}
	}
	
	public void endElement(String uri, String name, String qName) throws SAXException {
		if (TEST_RESULT.equals(name)) {
			session.getTestResult().endTest(currentTest, currentTestTime);
			if (currentAssertionFailed != null) {
				session.getTestResult().addFailure(currentTest, currentAssertionFailed);
			} else if (currentTestMessage != null && currentTestMessage.toString().trim().startsWith("failed:")) {
			    session.getTestResult().addFailure(currentTest, currentTestMessage.toString());
			}
		}
	}
	
	public void characters(char ch[], int start, int length) {
		if (EXPECTED.equals(currentTag)) {
			StringBuffer currentExpected = new StringBuffer();
			if (currentAssertionFailed != null) {
				currentExpected.append(currentAssertionFailed.expected());
			}
			currentExpected.append(ch, start, length);
			currentAssertionFailed = new AssertionFailed(currentExpected, currentAssertionFailed == null ? "" : currentAssertionFailed.actual());
		} else if (ACTUAL.equals(currentTag)) {
			StringBuffer currentActual = new StringBuffer();
			if (currentAssertionFailed != null) {
				currentActual.append(currentAssertionFailed.actual());
			}
			currentActual.append(ch, start, length);
			currentAssertionFailed = new AssertionFailed(currentAssertionFailed == null ? "" : currentAssertionFailed.expected(), currentActual);			
		} else if (TEST_RESULT.equals(currentTag)) {
			currentTestMessage.append(ch, start, length);
		}
	}
}