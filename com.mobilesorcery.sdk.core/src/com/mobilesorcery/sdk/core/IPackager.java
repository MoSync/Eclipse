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
package com.mobilesorcery.sdk.core;

import com.mobilesorcery.sdk.profiles.IProfile;


/**
 * <p>A packager is called after compiling and linking.</p>
 * <p>Clients should implement @see {@link IPackagerDelegate} and
 * add it using the corresponding extension point.
 * @see {@link AbstractPackager}
 * @author Mattias Bybro, mattias@bybro.com/mattias.bybro@purplescout.se
 *
 */
public interface IPackager extends IPackagerDelegate {

	/**
	 * Returns the id of this packager (may be {@code null}).
	 * @return
	 */
	public String getId();

	/**
	 * Returns the platform name of this packager (may return {@code null}).
	 * TODO: Platform and packager might need to be separated!
	 * @return
	 */
	public String getPlatform();


}
