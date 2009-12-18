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
package com.mobilesorcery.sdk.product.intro;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.Platform;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

public class IntroInitializer {

	private static IntroInitializer instance = new IntroInitializer();

	public static IntroInitializer getInstance() {
		// Yay, singleton!
		return instance;
	}

	private int state = INIT;

	private URI uri;

	private Exception e;

	private IntroParser parseState;

	public final static int INIT = 0;
	public final static int FETCHING = 1;
	public final static int FETCHED = 2;
	public final static int ERROR = 3;

	public interface ICallback {
		public void done();
	}
	
	private IntroInitializer() {
		try {
			uri = getClass().getResource("/content/welcome.xml").toURI();
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
			// Should not happen.
		}
	}
	
	public void fetch(final ICallback callback) {
		if (state == FETCHING) {
			return;
		}
		
		Thread thread = new Thread(new Runnable() {
			public void run() {
				syncFetch(callback);
			}			
		});
		thread.start();
	}
	
	private synchronized void syncFetch(ICallback callback) {
		state = FETCHING;
		InputStream input = null;
		try {
			URLConnection connection = uri.toURL().openConnection();
			input = connection.getInputStream();
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			IntroParser parseState = new IntroParser(); 
			parser.parse(input, parseState);
			this.parseState = parseState;
			state = FETCHED;
		} catch (Exception e) {
			setException(e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					setException(e);
				}
			}
		}
		callback.done();
	}
	
	private void setException(Exception e) {
		this.e = e;
		state = ERROR;
	}

	public int getState() {
		return state;
	}

	public Exception getException() {
		return e;
	}
	
	public List<IntroLink> getLinks(String type) {
		List<IntroLink> result = parseState == null ? null : parseState.getLinks(type);
		return result == null ? new ArrayList<IntroLink>(0) : result; 
	}
}
