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
package com.mobilesorcery.sdk.builder.iphoneos;


import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;



/**
 * Plugin entry point. Is responsible for creating xcode 
 * project for iphone os devices.
 * 
 * @author Ali Mosavian
 */
public class IPhoneOSPackager
extends AbstractPackager
{
    


    /**
     * This method is called upon whenever a iphone os package
     * is to be built.
     *
     * @param project
     * @param targetProfile Profile information i presume.
     * @param buildResult The build result is returned through this parameter.
     *
     * @throws CoreException Error occurred
     */
    public void createPackage ( MoSyncProject project,
                                IBuildVariant variant,
                                IBuildResult buildResult )
    throws CoreException
    {
        DefaultPackager intern;

        // Was used for printing to console
        intern = new DefaultPackager( project,
                                      variant );
        
        try
        {

        }
        catch ( Exception e )
        {
            StringWriter s = new StringWriter( );
            PrintWriter  pr= new PrintWriter( s );
            e.printStackTrace( pr );

            // Return stack trace in case of error
            throw new CoreException( new Status( IStatus.ERROR,
                                                 "com.mobilesorcery.builder.iphoneos",
                                                 s.toString( ) ));
        }
    }
}
