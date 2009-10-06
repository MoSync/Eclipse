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
package com.mobilesorcery.sdk.profiles;

/**
 * <p>A provider interface for <code>ITarget</code>s.
 * (The purpose of this class is just to keep an implementation-less
 * interface for UI purposes - see ProfilesView in the ui plugin)</p>
 * @author Mattias Bybro, mattias.bybro@purplescout.com/mattias@bybro.com
 *
 */
public interface ITargetProfileProvider {

	/**
	 * <p>Returns a target profile.</p>	
	 * @return
	 */
	public IProfile getTargetProfile();
	
}
