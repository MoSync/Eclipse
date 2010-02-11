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
package com.mobilesorcery.sdk.ui.targetphone;

import org.eclipse.jface.resource.ImageDescriptor;


public interface ITargetPhoneTransport extends ITargetPhoneTransportDelegate {
	
	/**
	 * Returns the unique identifier of this <code>ITargetPhoneTransport</code>.
	 * A shared instance of <code>ITargetPhoneTransport</code> with a specified id
	 * can be fetched using <code>TargetPhonePlugin.getTargetPhoneTransport(String)</code>
	 * @return
	 */
	public String getId();
	
	/**
	 * Returns a default icon for this means of transport.
	 * @return
	 */
	public ImageDescriptor getIcon();
}
