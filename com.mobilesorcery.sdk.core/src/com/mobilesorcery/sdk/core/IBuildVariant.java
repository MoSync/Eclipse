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
 * <p>An interface representing a 'build variant' - each 
 * build variant will produce a different build result,
 * typically the location of the build result should depend
 * only on the build variant</p>
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface IBuildVariant {

    public String getConfigurationId();
    
    public IProfile getProfile();
    
    public boolean isFinalizerBuild();
    
}
