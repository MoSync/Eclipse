/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.ui;

import org.eclipse.jface.dialogs.IMessageProvider;

import com.mobilesorcery.sdk.core.Util;

/**
 * A default implementation of the <code>IMessageProvider</code> API.
 * @author Mattias Bybro
 *
 */
public class DefaultMessageProvider implements IMessageProvider {

	public static final IMessageProvider EMPTY = new DefaultMessageProvider(null, NONE);
	
	private String message;
	private int type;

	public DefaultMessageProvider(String message, int type) {
		this.message = message;
		this.type = type;
	}
	
	public String getMessage() {
		return message;
	}

	public int getMessageType() {
		return type;
	}

	public static boolean isEmpty(IMessageProvider provider) {
		if (provider == null) {
			return true;
		}
		
		return Util.isEmpty(provider.getMessage()) && provider.getMessageType() == IMessageProvider.NONE;
	}

}
