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

import com.mobilesorcery.sdk.core.IPropertyOwner.IWorkingCopy;

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
	 * <p>The 'default' test configuration id</p>
	 */
	public final static String TEST_ID = "Test";
	
	
	public String getId();
	
	public NameSpacePropertyOwner getProperties();

	public IBuildConfiguration clone(String id);

}
