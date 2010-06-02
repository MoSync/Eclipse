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
package com.mobilesorcery.sdk.builder.winmobile;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.WineHelper;
import com.mobilesorcery.sdk.core.templates.Template;

public class WinMobilePackager 
extends AbstractPackager 
{
	private String m_cabWizLoc;

	public WinMobilePackager() 
	{
		MoSyncTool tool = MoSyncTool.getDefault( );
		m_cabWizLoc = tool.getBinary( "cabwiz" ).toOSString( );		
	}

    public void createPackage(MoSyncProject project, IBuildVariant variant, IBuildResult buildResult) 
    throws CoreException 
    {
        DefaultPackager internal = new DefaultPackager(project, variant);

		internal.setParameter("D", shouldUseDebugRuntimes() ? "D" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
        if (internal.resolveFile("%resource-output%").exists()) { //$NON-NLS-1$
            internal.setParameter("HAS_RESOURCES", Boolean.toString(true)); //$NON-NLS-1$
        }
        
        try 
        {
            internal.mkdirs("%package-output-dir%"); //$NON-NLS-1$
            
            File infFile = new File(internal.resolve("%package-output-dir%/cabwiz.inf")); //$NON-NLS-1$
            File cabFile = new File(internal.resolve("%package-output-dir%/cabwiz.cab")); //$NON-NLS-1$
            File renamedCabFile = new File(internal.resolve("%package-output-dir%/%app-name%.cab")); //$NON-NLS-1$
            
            internal.setParameter( "cab-runtime-dir", WineHelper.convPath( internal.resolve("%runtime-dir%") ) );
            internal.setParameter( "cab-compile-output-dir", WineHelper.convPath( internal.resolve("%compile-output-dir%") ) );
            
            
            Template template = new Template(getClass().getResource("/templates/cabwiz.inf.template")); //$NON-NLS-1$
            String resolvedTemplate = template.resolve(internal.getParameters().toMap());
            Util.writeToFile(infFile, resolvedTemplate);
            
            internal.runCommandLine( m_cabWizLoc, 
                                     WineHelper.convPath( infFile.getAbsolutePath( ) ), 
                                     "/dest", 
                                     WineHelper.convPath( internal.resolve( "%package-output-dir%" ) ), 
                                     "/compress" );
            Util.copyFile(new NullProgressMonitor(), cabFile, renamedCabFile);            
            buildResult.setBuildResult(renamedCabFile);
        } 
        catch (Exception e) 
        {
            throw new CoreException( new Status( IStatus.ERROR, 
            		                             "com.mobilesorcery.builder.winmobile", 
            		                             Messages.WinMobilePackager_PackageError, 
            		                             e ) ); //$NON-NLS-1$
        }
    }


}
