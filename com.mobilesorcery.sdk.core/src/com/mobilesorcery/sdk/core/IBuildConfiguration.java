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

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * <p>An interface representing a build configuration, such as "Release", "Debug", "Test", etc.</p>
 * <p><b>Implementation note:</b> Why not use CDT managed make? Because it is still too heavy-weight
 * and too general for our purposes. Maybe some day we need that flexibility in tool-chains,
 * then we could integrate. But not before.</p>
 * @author Mattias Bybro
 *
 */
public interface IBuildConfiguration {

    /**
     * <p>The 'default' build configuration id</p>
     */
    public static final String RELEASE_ID = "Release";

	/**
	 * <p>The 'default' debug configuration id</p>
	 */
	public final static String DEBUG_ID = "Debug";
	
	/**
     * <p>The release build configuration type</p>
     */
    public static String RELEASE_TYPE = "Release";
	
    /**
     * <p>The debug build configuration type</p>
     */
    public static String DEBUG_TYPE = "Debug";
    
	
	
	/**
	 * Returns the (unique) id of this build configuration.
	 * @return
	 */
	public String getId();
	
	/**
	 * <p>Returns the build configuration types this 
	 * configuration matches (eg "debug", "release", "test",
	 * "profile", "finalize", etc).</p>
	 * <p>This method will always return at least one type.
	 * If the types are never set using <code>setTypes()</code>
	 * (or during object construction), this method will return
	 * <code>RELEASE_TYPE</code> for all build configurations except
	 * for build configurations with the default <code>DEBUG_ID</code>,
	 * in which case <code>DEBUG_TYPE</code> will be returned.</p>
	 * @return
	 */
	public Set<String> getTypes();
	
	public void setTypes(Collection<String> list);
	    
	public NameSpacePropertyOwner getProperties();

	/**
	 * Creates a copy of this build configuration
	 * @param id The id of the new build configuration
	 * @return
	 */
	public IBuildConfiguration clone(String id);

}
