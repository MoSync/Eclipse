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
package com.mobilesorcery.sdk.profiling;

import java.io.File;
import java.util.Calendar;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.IFilter;

public interface IProfilingSession extends IAdaptable{

    /**
     * Returns the 'root' invocation of this session
     * @return
     */
    public IInvocation getInvocation();
    
    /**
     * Returns the filter of this profiling session
     * @return
     */
    public IFilter<IInvocation> getFilter();
    
    /**
     * Returns a user-friendly name to identify this session.
     * @return
     */
    public String getName();

    /**
     * Returns the start time of this session.
     * @return
     */
	public Calendar getStartTime();
	
	/**
	 * Returns the file that holds the data of this session.
	 * May be <code>null</code>.
	 * @return
	 */
	public File getProfilingFile(); 
}
