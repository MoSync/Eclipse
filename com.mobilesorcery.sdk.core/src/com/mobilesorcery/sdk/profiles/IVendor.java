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

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * <p>Represents a vendor, for instance SonyEricsson, Nokia, Motorola, etc.</p>. 
 * @author Mattias Bybro, mattias.bybro@purplescout.com/mattias@bybro.com
 *
 */
public interface IVendor {

	/**
	 * <p>Returns the name of this <code>IVendor</code></p>
	 * @return
	 */
    String getName();

    /**
     * <p>Returns the icon used to represent this vendor - typically
     * a logotype.</p>
     * @return
     */
    ImageDescriptor getIcon();
    
    /**
     * <p>Returns all profiles associated with this vendor.
     * A profile roughly corresponds to a mobile phone model,
     * even though this may not be the case.</p> 
     * @return
     */
    IProfile[] getProfiles();

    /**
     * <p>Returns a specific profile associated with this vendor.</p> 
     * @param name
     * @return <code>null</code> if no profile with <code>name</code> exists.
     */
    IProfile getProfile(String name);
}
