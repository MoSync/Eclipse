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

import java.util.SortedMap;

import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * <p>An interface representing a 'build variant' - each
 * build variant will produce a different build result,
 * typically the location of the build result should depend
 * only on the build variant</p>
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface IBuildVariant {

	/**
	 * Returns the configuration of this variant. (As a string identifier).
	 * @return
	 */
    public String getConfigurationId();

    /**
     * Returns the {@link IProfile} of this variant.
     * @return
     */
    public IProfile getProfile();

    /**
     * <p>Returns the set of specifiers used for this variant.</p>
     * <p>A specifier allows for specialized builds where a separate binary
     * needs to be produced for whatever reason.</p>
     * <p>Examples of potential specifiers:
     * <ul>
     * <li>Locale - if some aspect of the binary needs to be different based on language, etc</li>
     * <li>Emulation - the iPhone Simulator needs a completely different binary than used by the actual device</li>
     * </ul>
     * </p>
     * @return
     */
    public SortedMap<String, String> getSpecifiers();
}
